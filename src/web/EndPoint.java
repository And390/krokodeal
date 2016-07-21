package web;

import data.*;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.and390.template.TemplateManager;
import service.MailSender;
import service.TaskReturnScheduler;
import util.Config;
import util.OrderedListMap;
import util.sql.SingleConnectionDataSource;
import utils.ByteArray;
import utils.Util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.mail.MessagingException;
import javax.script.Bindings;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static data.UserRole.ADMIN;
import static data.UserRole.MASTER;
import static data.MasterTaskStatus.*;

@Path("/")
public class EndPoint extends AbstractEndPoint implements AutoCloseable {

    private static Logger log = LoggerFactory.getLogger(EndPoint.class);

    // TODO not authorized и not logged in надо разделить - только в первом случае клиент должен обновлять страницу (а лучше еще мессадж прокидывать при релоаде)

    private final DataAccess dataAccess;
    private final TaskReturnScheduler taskReturnScheduler;
    private final MailSender mailSender;
    private final TemplateManager templateManager;

    private final String registerMailSubject;
    private final String registerMailText;

    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat datetimeFormat;

    private final ScheduledExecutorService scheduler;
    private volatile ConcurrentHashMap<Integer, Integer> feedbackCounts = null;
    private final String[] feedbackRecipients;
    private final String feedbackSubjectTemplate;
    private final String feedbackContentTemplate;
    private final int feedbackLimit;
    private final int feedbackLimitDuration;

    private final String[] commentsMailRecipients;
    private final String commentsMailSubjectTemplate;
    private final String commentsMailContentTemplate;

    public EndPoint(DataAccess dataAccess, TaskReturnScheduler taskReturnScheduler, MailSender mailSender, TemplateManager templateManager) {
        this.dataAccess = dataAccess;
        this.taskReturnScheduler = taskReturnScheduler;
        this.mailSender = mailSender;
        this.templateManager = templateManager;
        this.registerMailSubject = Config.getNotEmpty("register.mail.subject");
        this.registerMailText = Config.getNotEmpty("register.mail.text");
        this.dateFormat = new SimpleDateFormat(Config.getNotEmpty("web.def.dateformat").replace("m", "M").replace("yy", "yyyy"));
        this.datetimeFormat = new SimpleDateFormat(Config.getNotEmpty("web.def.datetimeformat"));
        this.feedbackRecipients = Util.slice(Config.getNotEmpty("feedback.recipients"), ',');
        this.feedbackSubjectTemplate = Config.get("feedback.subject");
        this.feedbackContentTemplate = Config.get("feedback.content");
        this.feedbackLimit = Config.getPInt("feedback.limit");
        this.feedbackLimitDuration = Config.getPInt("feedback.limit.duration");
        this.commentsMailRecipients = Util.slice(Config.getNotEmpty("comments.mail.recipients"), ',');
        this.commentsMailSubjectTemplate = Config.get("comments.mail.subject");
        this.commentsMailContentTemplate = Config.get("comments.mail.content");

        scheduler = Executors.newSingleThreadScheduledExecutor((r) -> new Thread(r, "Scheduler"));
        scheduler.scheduleWithFixedDelay(() -> feedbackCounts = new ConcurrentHashMap<>(), 0, feedbackLimitDuration, TimeUnit.SECONDS);
    }

    public void close()
    {
        scheduler.shutdown();
    }

    @Override
    protected Bindings createBindings(HttpServletRequest request, User user) {
        Bindings bindings = super.createBindings(request, user);
        bindings.put("dateFormat", dateFormat);
        bindings.put("datetimeFormat", datetimeFormat);
        return bindings;
    }

    private void sendMail(String to, String subject, String content)
    {
        try  {  mailSender.send(to, subject, content);  }
        catch (MessagingException e)  {  log.error("Error sending mail", e);  }
    }

    private void sendMail(String[] to, String subject, String content)
    {
        try  {  mailSender.send(to, subject, content);  }
        catch (MessagingException e)  {  log.error("Error sending mail", e);  }
    }

    @GET  @Produces(HTML_TYPE)
    public String getMain(@Context HttpServletRequest request) throws Exception
    {
        //TODO may be redirect
        User user = (User) request.getSession().getAttribute("user");
        if (user==null)  return getLogin(request);
        else if (user.role==UserRole.MASTER)  return getProfile(request);
        else if (user.role==UserRole.ADMIN)  return getAllTasks(request, null, null);
        else  throw new IllegalStateException("Unknow user.role: " + user.role);
    }

    @GET  @Path("/js/common")  @Produces("application/javascript")
    public String getCommonJS(@Context HttpServletRequest request) throws Exception
    {
        return templateManager.eval("/js/common.js", createBindings(request, null));
    }

    @GET  @Path("/login")  @Produces(HTML_TYPE)
    public String getLogin(@Context HttpServletRequest request) throws Exception
    {
        Bindings bindings = createBindings(request, null);
        return templateManager.eval("/login.html", bindings);
    }

    @GET  @Path("/register")  @Produces(HTML_TYPE)
    public String getRegister(@Context HttpServletRequest request) throws Exception
    {
        Bindings bindings = createBindings(request, null);
        return templateManager.eval("/register.html", bindings);
    }

    private static final int SHORT_INACTIVE_INTERVAL = 10*60;

    @POST  @Path("/login")  @Produces(JSON_TYPE)
    public Object postLogin(@Context HttpServletRequest request, @FormParam("login") String login, @FormParam("password") String password, @FormParam("remember") boolean remember) throws Exception
    {
        User user = dataAccess.loadUser(login);
        if (login != null && password != null && user != null && Arrays.equals(user.password, passwordServerHash(login, password)))  {
            log.info("login user id={}", user.id);
            HttpSession session = request.getSession();
            session.setAttribute("user", user);
            session.setMaxInactiveInterval(remember ? 0 :SHORT_INACTIVE_INTERVAL );
            return ApiResponse.success();
        }
        else
            throw new ClientException("Неправильный пароль или логин");
    }

    @POST  @Path("/register")  @Produces(JSON_TYPE)
    public Object postRegister(@Context HttpServletRequest request, @FormParam("login") String login, @FormParam("password") String password) throws Exception
    {
        checkNotEmpty(login, "login");
        checkNotEmpty(password, "password");
        User user = dataAccess.createUser(login, passwordServerHash(login, password), UserRole.MASTER);
        if (user == null)
            throw new ClientException("Пользователь с таким логином уже существует");
        log.info("register user id={}, login={}", user.id, login);
        sendMail(login, registerMailSubject, registerMailText.replace("${login}", login));
        HttpSession session = request.getSession();
        session.setAttribute("user", user);
        session.setMaxInactiveInterval(SHORT_INACTIVE_INTERVAL);
        return ApiResponse.success();
    }

    @POST  @Path("/logout")  @Produces(JSON_TYPE)
    public Object postLogout(@Context HttpServletRequest request) throws Exception
    {
        HttpSession session = request.getSession();
        session.removeAttribute("user");
        return ApiResponse.success();
    }

    @POST @Path("/change_password")  @Produces(JSON_TYPE)
    public Object postChangePassword(@Context HttpServletRequest request, @FormParam("password") String password) throws Exception
    {
        User user = checkRights(request, null);
        checkNotEmpty(password, "password");
        dataAccess.updatePassword(user.id, passwordServerHash(user.login, password));
        return ApiResponse.success();
    }

    @GET  @Path("/profile")  @Produces(HTML_TYPE)
    public String getProfile(@Context HttpServletRequest request) throws Exception
    {
        User user = checkRights(request, MASTER);
        Bindings bindings = createBindings(request, user);
        //if (user.role == UserRole.MASTER)  {
            MasterInfo master = dataAccess.loadMasterInfo(user.id);
            bindings.put("master", master);
            bindings.put("wallets", dataAccess.loadWallets());
            bindings.put("devices", dataAccess.loadDevices());
        //}
        return templateManager.eval("/profile.html", bindings);
    }

    @POST  @Path("/profile_info")  @Produces(JSON_TYPE)
    public Object postProfile(@Context HttpServletRequest request, @FormParam("city") String city,
                              @FormParam("phone") String phone, @FormParam("male") Boolean male, @FormParam("age") Integer age,
                              @FormParam("devices") List<Integer> devices) throws Exception
    {
        User user = checkRights(request, MASTER);
        if (age != null)  checkInt(age, 1, 127, "age");
        dataAccess.updateMasterInfo(user.id, city, phone, male, age);
        dataAccess.updateMasterDevices(user.id, devices);
        return ApiResponse.success();
    }

    @POST  @Path("/profile_wallet")  @Produces(JSON_TYPE)
    public Object postProfile(@Context HttpServletRequest request,
                              @FormParam("walletType") Integer walletType, @FormParam("walletNum") String walletNum) throws Exception
    {
        User user = checkRights(request, MASTER);
        dataAccess.updateMasterWallet(user.id, walletType, walletNum);
        return ApiResponse.success();
    }

    @GET  @Path("/faq")  @Produces(HTML_TYPE)
    public String getFAQ(@Context HttpServletRequest request) throws Exception
    {
        User user = checkRights(request, null);
        Bindings bindings = createBindings(request, user);
        return templateManager.eval("/faq.html", bindings);
    }

    @GET  @Path("/feedback")  @Produces(HTML_TYPE)
    public String getFeedback(@Context HttpServletRequest request) throws Exception
    {
        User user = checkRights(request, MASTER);
        Bindings bindings = createBindings(request, user);
        return templateManager.eval("/feedback.html", bindings);
    }

    @POST  @Path("/feedback")  @Produces(JSON_TYPE)
    public Object postFeedback(@Context HttpServletRequest request, @FormParam("subject") String subject,
                              @FormParam("content") String content) throws Exception
    {
        checkNotEmpty(subject, "subejct");
        checkNotEmpty(content, "content");
        User user = checkRights(request, MASTER);
        int n = feedbackCounts.compute(user.id, (id, count) -> count==null ? 1 : count + 1);
        if (n > feedbackLimit) {
            log.warn("feedback limit ({}) for user id={}, login={}", n, user.id, user.login);
            throw new ClientException("Вы исчерпали количество сообщений, попробуйте позже");
        }
        log.info("feedback from user id={}", user.id);
        dataAccess.addFeedback(user.id, subject, content);
        sendMail(feedbackRecipients,
                feedbackSubjectTemplate.replace("${login}", user.login).replace("${subject}", subject).replace("${content}", content),
                feedbackContentTemplate.replace("${login}", user.login).replace("${subject}", subject).replace("${content}", content));
        return ApiResponse.success();
    }

    @GET  @Path("/tasks/all")  @Produces(HTML_TYPE)
    public String getAllTasks(@Context HttpServletRequest request, @QueryParam("from") String from, @QueryParam("to") String to) throws Exception
    {
        Date fromDate = checkOptDate(from);
        Date toDate = checkOptDateEnd(to);
        User user = checkRights(request, ADMIN);
        return getTasks(request, user, dataAccess.loadAllTasks(fromDate, toDate));
    }

    @GET  @Path("/tasks/master")  @Produces(HTML_TYPE)
    public String getMasterTasks(@Context HttpServletRequest request, @QueryParam("from") String from, @QueryParam("to") String to) throws Exception
    {
        Date fromDate = checkOptDate(from);
        Date toDate = checkOptDateEnd(to);
        User user = checkRights(request, ADMIN);
        List<MasterTask> tasks = dataAccess.loadMasterTasks(fromDate, toDate);
        Collections.sort(tasks, (t1,t2) -> t1.status!=t2.status ? (t1.status==COMPLETE ? -1 : t2.status==COMPLETE ? 1 : t1.status==WORK ? -1 : 1) : t1.taken.compareTo(t2.taken));
        return getTasks(request, user, tasks, "/tasks.html");

    }

    @GET  @Path("/tasks/open")  @Produces(HTML_TYPE)
    public String getOpenTasks(@Context HttpServletRequest request) throws Exception
    {
        User user = checkRights(request, MASTER);
        //TODO возможно это лучше одним запросом сделать
        List<Integer> masterDevices =  dataAccess.loadMasterDevices(user.id);
        if (masterDevices != null && masterDevices.isEmpty())  throw new ClientException("Вы не указали ни одного устройства в найстройках", true);
        return getTasks(request, user, dataAccess.loadOpenTasks(TaskStatus.START, masterDevices, user.id), "/tasks.html");
    }

    @GET  @Path("/tasks/work")  @Produces(HTML_TYPE)
    public String getWorkTasks(@Context HttpServletRequest request) throws Exception
    {
        User user = checkRights(request, MASTER);
        List<MasterTask> tasks = dataAccess.loadTasks(user.id, MasterTaskStatus.WORK, null, null);
        return getTasks(request, user, tasks);
    }

    @GET  @Path("/tasks/complete")  @Produces(HTML_TYPE)
    public String getCompleteTasks(@Context HttpServletRequest request) throws Exception
    {
        User user = checkRights(request, MASTER);
        List<MasterTask> tasks = dataAccess.loadTasks(user.id, MasterTaskStatus.COMPLETE, null, null);
        return getTasks(request, user, tasks);
    }

    @GET  @Path("/tasks/confirm")  @Produces(HTML_TYPE)
    public String getConfirmTasks(@Context HttpServletRequest request, @QueryParam("from") String from, @QueryParam("to") String to) throws Exception
    {
        Date fromDate = checkOptDate(from);
        Date toDate = checkOptDateEnd(to);
        User user = checkRights(request, MASTER);
        List<MasterTask> tasks = dataAccess.loadTasks(user.id, MasterTaskStatus.CONFIRM, fromDate, toDate);
        return getTasks(request, user, tasks);
    }

    private String getTasks(HttpServletRequest request, User user, List<? extends Task> tasks) throws Exception  {  return getTasks(request, user, tasks, "/tasks.html");  }
    private String getTasks(HttpServletRequest request, User user, List<? extends Task> tasks, String path) throws Exception
    {
        Bindings bindings = createBindings(request, user);
        bindings.put("tasks", tasks);
        List<Device> devices = dataAccess.loadDevices();
        bindings.put("devices", devices);
        bindings.put("devicesMap", new OrderedListMap<>(devices));
        return templateManager.eval(path, bindings);
    }

    @GET  @Path("/tasks/{id}")  @Produces(HTML_TYPE)
    public String getTask(@Context HttpServletRequest request, @PathParam("id") Integer id, @QueryParam("master") Integer masterId) throws Exception
    {
        checkNotNull(id, "id");
        User user = checkRights(request, null);
        if (user.role==MASTER && masterId!=null)  throw new ClientException(DENIED);
        //TODO мастеру не нужено видеть остановленную задачу, если он ее не делает (сложный запрос будет), хотя можно и забить на это
        Task task = dataAccess.loadTask(id, null, masterId != null && user.role==UserRole.ADMIN ? masterId : user.role==MASTER ? user.id : null);
        if (task==null)  throw new ClientException("Задача не существует");  // TODO 404 not found
        Bindings bindings = createBindings(request, user);
        bindings.put("task", task);
        return templateManager.eval("/task.html", bindings);
    }

    @GET  @Path("/tasks/{id}/{masterId}")  @Produces(HTML_TYPE)
    public String getMasterTask(@Context HttpServletRequest request, @PathParam("id") Integer id, @PathParam("masterId") Integer masterId) throws Exception
    {
        return getTask(request, id, masterId);
    }

    @GET  @Path("/tasks/create")  @Produces(HTML_TYPE)
    public String getCreateTask(@Context HttpServletRequest request) throws Exception
    {
        User user = checkRights(request, ADMIN);
        Bindings bindings = createBindings(request, user);
        bindings.put("types", dataAccess.getTaskTypes());
        bindings.put("devices", dataAccess.loadDevices());
        bindings.put("task", null);
        return templateManager.eval("/edit_task.html", bindings);
    }

    @POST  @Path("/tasks/create")  @Produces(JSON_TYPE)
    public ApiResponse postCreateTask(@Context HttpServletRequest request, @FormParam("type") Integer typeId,
                                      @FormParam("device") Integer deviceId, @FormParam("price") int price,
                                      @FormParam("count") Integer countLimit, @FormParam("time") Integer timeLimit,
                                      @FormParam("title") String title, @FormParam("description") String description) throws Exception
    {
        if (typeId != null && dataAccess.getTaskType(typeId) == null)  throw new ClientException("Wrong typeId: "+typeId);
        checkNotNull(deviceId, "deviceId");
        checkPositive(price, "price");
        if (countLimit != null)  checkPositive(countLimit, "countLimit");
        if (timeLimit != null)  checkPositive(timeLimit, "timeLimit");
        checkNotEmpty(title, "title");
        checkNotEmpty(description, "description");
        User user = checkRights(request, ADMIN);
        dataAccess.createTask(user.id, typeId, deviceId, price, countLimit != null ? countLimit : 0, timeLimit != null ? timeLimit : 0, title, description);
        return ApiResponse.success();
    }

    @GET  @Path("/tasks/{id}/edit")  @Produces(HTML_TYPE)
    public String getEditTask(@Context HttpServletRequest request, @PathParam("id") Integer id) throws Exception
    {
        checkNotNull(id, "id");
        User user = checkRights(request, ADMIN);
        Task task = dataAccess.loadTask(id, null, null);
        if (task==null)  throw new ClientException("Задача не существует");  // TODO 404 not found
        Bindings bindings = createBindings(request, user);
        bindings.put("types", dataAccess.getTaskTypes());
        bindings.put("devices", dataAccess.loadDevices());
        bindings.put("task", task);
        return templateManager.eval("/edit_task.html", bindings);
    }

    @GET  @Path("/tasks/{id}/copy")  @Produces(HTML_TYPE)
    public String getCopyTask(@Context HttpServletRequest request, @PathParam("id") Integer id) throws Exception
    {
        return getEditTask(request, id);
    }

    @POST  @Path("/tasks/{id}/edit")  @Produces(JSON_TYPE)
    public ApiResponse postEditTask(@Context HttpServletRequest request,
                                    @PathParam("id") Integer id, @FormParam("type") Integer typeId,
                                    @FormParam("device") Integer deviceId, @FormParam("price") int price,
                                    @FormParam("count") Integer countLimit, @FormParam("time") Integer timeLimit,
                                    @FormParam("title") String title, @FormParam("description") String description) throws Exception
    {
        checkNotNull(id, "taskId");
        if (typeId != null && dataAccess.getTaskType(typeId) == null)  throw new ClientException("Wrong typeId: "+typeId);
        checkNotNull(deviceId, "deviceId");
        checkPositive(price, "price");
        if (countLimit != null)  checkPositive(countLimit, "countLimit");
        if (timeLimit != null)  checkPositive(timeLimit, "timeLimit");
        checkNotEmpty(title, "title");
        checkNotEmpty(description, "description");
        checkRights(request, ADMIN);
        dataAccess.updateTask(id, typeId, deviceId, price, countLimit != null ? countLimit : 0, timeLimit != null ? timeLimit : 0, title, description);
        return ApiResponse.success();
    }

//    @POST  @Path("/delete_task")  @Produces(JSON_TYPE)
//    public ApiResponse postDeleteTask(@Context HttpServletRequest request, @FormParam("id") int taskId) throws Exception
//    {
//        User user = checkRights(request, UserRole.ADMIN);
//        if (!dataAccess.deleteTask(taskId))  throw new ClientException("Задача не существует");  // TODO 404 not found
//        return ApiResponse.success();
//    }

    @POST  @Path("/start_task")  @Produces(JSON_TYPE)
    public ApiResponse postStartTask(@Context HttpServletRequest request, @FormParam("id") Integer id) throws Exception
    {
        checkNotNull(id, "id");
        User user = checkRights(request, ADMIN);
        if (!dataAccess.startTask(id))  throw new ClientException("Невозможно запустить задачу");
        return ApiResponse.success();
    }

    @POST  @Path("/stop_task")  @Produces(JSON_TYPE)
    public ApiResponse postStopTask(@Context HttpServletRequest request, @FormParam("id") Integer id) throws Exception
    {
        checkNotNull(id, "id");
        User user = checkRights(request, ADMIN);
        if (!dataAccess.stopTask(id))  throw new ClientException("Невозможно остановить задачу");
        return ApiResponse.success();
    }

    @POST  @Path("/close_task")  @Produces(JSON_TYPE)
    public ApiResponse postCloseTask(@Context HttpServletRequest request, @FormParam("id") Integer id) throws Exception
    {
        checkNotNull(id, "id");
        User user = checkRights(request, ADMIN);
        if (!dataAccess.closeTask(id))  throw new ClientException("Невозможно удалить задачу");
        return ApiResponse.success();
    }

    @POST  @Path("/take_task")  @Produces(JSON_TYPE)
    public ApiResponse postTakeTask(@Context HttpServletRequest request, @FormParam("id") Integer id) throws Exception
    {
        checkNotNull(id, "id");
        User user = checkRights(request, MASTER);
        if (!dataAccess.takeTask(id, user.id))  throw new ClientException("Невозможно взять задачу");
        scheduleTaskReturn(id, user.id);
        return ApiResponse.success();
    }

    @POST  @Path("/return_task")  @Produces(JSON_TYPE)
    public ApiResponse postReturnTask(@Context HttpServletRequest request, @FormParam("id") Integer id) throws Exception
    {
        checkNotNull(id, "id");
        User user = checkRights(request, MASTER);
        if (!dataAccess.returnTask(id, user.id))  throw new ClientException("Невозможно вернуть задачу");
        taskReturnScheduler.cancel(id, user.id);
        return ApiResponse.success();
    }

    @POST  @Path("/complete_task")  @Produces(JSON_TYPE)
    public ApiResponse postCompleteTask(@Context HttpServletRequest request, @FormParam("id") Integer id) throws Exception
    {
        checkNotNull(id, "id");
        User user = checkRights(request, MASTER);
        if (!dataAccess.completeTask(id, user.id))  throw new ClientException("Невозможно выполнить задачу");
        taskReturnScheduler.cancel(id, user.id);
        return ApiResponse.success();
    }

    @POST  @Path("/resume_task")  @Produces(JSON_TYPE)
    public ApiResponse postResumeTask(@Context HttpServletRequest request, @FormParam("id") Integer id) throws Exception
    {
        checkNotNull(id, "id");
        User user = checkRights(request, MASTER);
        if (!dataAccess.resumeTask(id, user.id))  throw new ClientException("Невозможно возобновить задачу");
        scheduleTaskReturn(id, user.id);
        return ApiResponse.success();
    }

    @POST  @Path("/reject_task")  @Produces(JSON_TYPE)
    public ApiResponse postRejectTask(@Context HttpServletRequest request, @FormParam("id") Integer id, @FormParam("master") Integer masterId) throws Exception
    {
        checkNotNull(id, "id");
        checkNotNull(masterId, "master");
        User user = checkRights(request, ADMIN);
        if (!dataAccess.resumeTask(id, masterId))  throw new ClientException("Невозможно отказать задачу");
        scheduleTaskReturn(id, user.id);
        return ApiResponse.success();
    }

    @POST  @Path("/confirm_task")  @Produces(JSON_TYPE)
    public ApiResponse postConfirmTask(@Context HttpServletRequest request, @FormParam("id") Integer id, @FormParam("master") Integer masterId) throws Exception
    {
        checkNotNull(id, "id");
        checkNotNull(masterId, "master");
        User user = checkRights(request, ADMIN);
        if (!dataAccess.confirmTask(id, masterId, user.id))  throw new ClientException("Невозможно подтвердить задачу");
        return ApiResponse.success();
    }

    private void scheduleTaskReturn(int taskId, int masterId) throws SQLException  {
        Task task = dataAccess.loadTask(taskId, null, null);
        if (task != null && task.timeLimit>0)  {  //маловероятно чтобы кто-то мог удалить задачу, но хуже от проверки не будет
            taskReturnScheduler.schedule(taskId, masterId, System.currentTimeMillis() + Task.LIMIT_TIME_UNIT.toMillis(task.timeLimit));
        }
    }

    @GET  @Path("/task_message")
    // TODO кэширование
    public void getTaskMessage(@Context HttpServletRequest request, @Context HttpServletResponse response, @QueryParam("id") Integer id) throws Exception
    {
        checkNotNull(id, "id");
        User user = checkRights(request, null);
        //TODO may be master must not see other people task messages
        dataAccess.loadTaskMessageContent(id, (type, in) -> {
            if (in==null)  throw new ClientException("Задача не существует");  // TODO 404 not found
            response.setContentType(type.mime);
            try (OutputStream out = response.getOutputStream())  {
                ByteArray.copy(in, out);
            }
            catch (IOException e)  {
                throw new RuntimeException(e);
            }
        });
    }

    @POST  @Path("/tasks/{id}/add_message")  @Produces(JSON_TYPE)
    public ApiResponse postAddTaskMessage(@Context HttpServletRequest request, @PathParam("id") Integer id, @FormParam("message") String message) throws Exception
    {
        User user = checkRights(request, MASTER);
        return postAddTaskMessage(id, user.id, user.id, message, user.login);
    }

    @POST  @Path("/tasks/{id}/{masterId}/add_message")  @Produces(JSON_TYPE)
    public ApiResponse postAddTaskMessage(@Context HttpServletRequest request, @PathParam("id") Integer id, @PathParam("masterId") Integer masterId, @FormParam("message") String message) throws Exception
    {
        User user = checkRights(request, ADMIN);
        checkNotNull(masterId, "masterId");
        return postAddTaskMessage(id, masterId, user.id, message, null);
    }

    private ApiResponse postAddTaskMessage(Integer id, int masterId, int authorId, String message, String doSendMailFrom) throws Exception
    {
        checkNotNull(id, "id");
        checkNotEmpty(message, "message");
        if (!dataAccess.addTaskMessage(id, masterId, authorId, message)) {
            throw new ClientException("По этой задаче невозможно отправить сообщение");
        }
        if (doSendMailFrom != null) {
            log.info("comment from master id={} for task id={}", authorId, id);
            sendMail(commentsMailRecipients,
                     commentsMailSubjectTemplate.replace("${login}", doSendMailFrom).replace("${content}", message),
                     commentsMailContentTemplate.replace("${login}", doSendMailFrom).replace("${content}", message));
        }
        return ApiResponse.success();
    }

    @POST  @Path("/tasks/{id}/add_image")  @Consumes(MediaType.MULTIPART_FORM_DATA)  @Produces(JSON_TYPE)
    public ApiResponse postAddTaskImage(@Context HttpServletRequest request, @PathParam("id") Integer id,
                                        @FormDataParam("image") InputStream image, @FormDataParam("image") FormDataContentDisposition disposition) throws Exception
    {
        User user = checkRights(request, MASTER);
        return postAddTaskImage(id, user.id, user.id, image, disposition);
    }

    @POST  @Path("/tasks/{id}/{masterId}/add_image")  @Consumes(MediaType.MULTIPART_FORM_DATA)  @Produces(JSON_TYPE)
    public ApiResponse postAddTaskImage(@Context HttpServletRequest request, @PathParam("id") Integer id, @PathParam("masterId") Integer masterId,
                                        @FormDataParam("image") InputStream image, @FormDataParam("image") FormDataContentDisposition disposition) throws Exception
    {
        User user = checkRights(request, ADMIN);
        checkNotNull(masterId, "masterId");
        return postAddTaskImage(id, masterId, user.id, image, disposition);
    }

    public ApiResponse postAddTaskImage(Integer id, int masterId, int authorId, InputStream image, FormDataContentDisposition disposition) throws Exception
    {
        checkNotNull(id, "id");
        checkNotNull(image, "image");
        checkNotNull(disposition.getFileName(), "filename");
        String extension = Util.fileExt(disposition.getFileName(), false);
        checkNotEmpty(disposition.getFileName(), "filename extension");
        TaskMessage.Type type = TaskMessage.Type.byExtension.get(extension);
        if (type == null)  throw new ClientException("Неизвестный тип файла: "+extension);
        if (!dataAccess.addTaskMessage(id, masterId, authorId, type, image)) {
            throw new ClientException("По этой задаче невозможно отправить сообщение");
        }
        return ApiResponse.success();
    }

    @GET  @Path("/finance")  @Produces(HTML_TYPE)
    public String getFinance(@Context HttpServletRequest request, @QueryParam("from") String from, @QueryParam("to") String to) throws Exception
    {
        Date fromDate = checkOptDate(from);
        Date toDate = checkOptDateEnd(to);
        User user = checkRights(request, MASTER);
        int payments = dataAccess.calcPayments(user.id, fromDate, toDate);
        Bindings bindings = createBindings(request, user);
        bindings.put("from", fromDate);
        bindings.put("to", toDate);
        bindings.put("payments", payments);
        return templateManager.eval("/finance.html", bindings);
    }

    @GET  @Path("/statistics/payments")  @Produces(HTML_TYPE)
    public String getStatisticsPayments(@Context HttpServletRequest request, @QueryParam("from") String from, @QueryParam("to") String to) throws Exception
    {
        Date fromDate = checkOptDate(from);
        Date toDate = checkOptDateEnd(to);
        User user = checkRights(request, ADMIN);
        List<MasterStat> masters = dataAccess.loadMastersStatistics(fromDate, toDate, false);
        Bindings bindings = createBindings(request, user);
        bindings.put("masters", masters);
        return templateManager.eval("/statistics_payments.html", bindings);
    }

    @GET  @Path("/statistics/masters")  @Produces(HTML_TYPE)
    public String getStatisticsMasters(@Context HttpServletRequest request, @QueryParam("from") String from, @QueryParam("to") String to) throws Exception
    {
        Date fromDate = checkOptDate(from);
        Date toDate = checkOptDateEnd(to);
        User user = checkRights(request, ADMIN);
        List<MasterStat> masters = dataAccess.loadMastersStatistics(fromDate, toDate, true);
        Bindings bindings = createBindings(request, user);
        bindings.put("masters", masters);
        return templateManager.eval("/statistics_masters.html", bindings);
    }


    public User checkRights(HttpServletRequest request, UserRole role) throws Exception
    {
        HttpSession session = request.getSession();
        User user = (User)session.getAttribute("user");
        if (user == null) {
            throw new ClientException(UNAUTHORIZED);
        }
        if (role != null && user.role != role) {
            throw new ClientException(DENIED);
        }
        return user;
    }

    private static void checkNotNull(Object value, String name)
    {
        if (value==null)  throw new ClientException("No parameter '" + name + "'");
    }

    private static void checkNotEmpty(String value, String name)
    {
        checkNotNull(value, name);
        if (value.length()==0)  throw new ClientException("Empty parameter value '" + name + "'");
    }

    private static void checkNotEmpty(byte[] value, String name)
    {
        checkNotNull(value, name);
        if (value.length==0)  throw new ClientException("Empty parameter value '" + name + "'");
    }

    private static void checkPositive(Integer value, String name)
    {
        checkNotNull(value, name);
        if (value <= 0)  throw new ClientException("Parameter value must be > 0 for '" + name +"'");
    }

    private static void checkInt(Integer value, int min, int max, String name)
    {
        checkNotNull(value, name);
        if (value < min)  throw new ClientException("Parameter value must be >= "+min+" for '"+name+"'");
        if (value > max)  throw new ClientException("Parameter value must be <= "+max+" for '"+name+"'");
    }

    private Date checkDate(String date) {
        try  {  return dateFormat.parse(date);  }
        catch (ParseException e)  {  throw new ClientException("Parameter value must be date in format '" + dateFormat.toPattern() + "'");  }
    }

    private Date checkOptDate(String dateString) {
        return dateString == null ? null : checkDate(dateString);
    }

    private Date checkOptDateEnd(String dateString) {
        if (dateString==null)  return null;
        Date date = checkDate(dateString);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTime();
    }


    //        ----    Authentication    ----

    private static String cryptEncoding = "UTF-8";  //та же, что используется на стороне клиента для преобразования строки соли

    private static byte[] passwordClientHash(String username, String password)
    {
        try  {
            byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(
                    new PBEKeySpec(password.toCharArray(), ("#"+username+"-salt!").getBytes(cryptEncoding), 100, 128)).getEncoded();
            return result;
        }
        catch (RuntimeException e)  {  throw e;  }
        catch (Exception e)  {  throw new RuntimeException (e);  }
    }

    private static byte[] passwordServerHash(String username, String password)
    {
        try  {
            byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(
                    new PBEKeySpec(password.toCharArray(), ("#"+username+"-server-salt!").getBytes(cryptEncoding), 200, 128)).getEncoded();
            return result;
        }
        catch (RuntimeException e)  {  throw e;  }
        catch (Exception e)  {  throw new RuntimeException (e);  }
    }

    private static byte[] passwordTotalHash(String username, String password)
    {
        return passwordServerHash(username, toHex(passwordClientHash(username, password)));
    }

    private static String toHex(byte[] array)
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if (paddingLength > 0)  return String.format("%0"+paddingLength+"d", 0) + hex;
        else  return hex;
    }

    public static class InsertUser {
        public static void main(String[] args) throws Exception {
            try (SingleConnectionDataSource dataSource = DataAccess.createTestDataSource())
            {
                DataAccess dataAccess = new DataAccess(dataSource);
                dataAccess.createUser(args[1], passwordTotalHash(args[1], args[2]), UserRole.valueOf(args[0].toUpperCase()));
            }
        }
    }

    public static class ChangePassword {
        public static void main(String[] args) throws Exception {
            try (SingleConnectionDataSource dataSource = DataAccess.createTestDataSource())
            {
                DataAccess dataAccess = new DataAccess(dataSource);
                User user = dataAccess.loadUser(args[0]);
                if (user == null) {
                    throw new Exception("user not found");
                }
                dataAccess.updatePassword(user.id, passwordTotalHash(user.login, args[1]));
            }
        }
    }
}

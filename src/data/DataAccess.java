package data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import util.Config;
import util.sql.SingleConnectionDataSource;
import util.sql.UtilConnection;
import util.sql.UtilStatement;
import utils.Util;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DataAccess {

    public static final Charset DB_CHARSET = StandardCharsets.UTF_8;

    public static HikariDataSource createDataSource()
    {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(Config.get("db.driver"));
        config.setJdbcUrl(Config.getNotEmpty("db.url"));
        config.setUsername(Config.get("db.user"));
        config.setPassword(Config.get("db.password"));
        //config.setAutoCommit(false);
        //config.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
        return new HikariDataSource(config);
    }

    public static SingleConnectionDataSource createTestDataSource() throws ClassNotFoundException, SQLException
    {
        return new SingleConnectionDataSource(Config.get("db.driver"), Config.getNotEmpty("db.url"), Config.get("db.user"), Config.get("db.password"));
    }

    private final DataSource dataSource;

    public DataAccess(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public User loadUser(String login) throws SQLException {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection());
             ResultSet resultSet = con.executeQuery("select id, password, role, created from user where login=?", login))
        {
            if (!resultSet.next())  return null;
            User user = new User();
            int i = 0;
            user.id = resultSet.getInt(++i);
            user.login = login;
            user.password = resultSet.getBytes(++i);
            user.role = UserRole.valueOf(resultSet.getString(++i).toUpperCase());
            user.created = resultSet.getTimestamp(++i);
            return user;
        }
    }

    public MasterInfo loadMasterInfo(int userId) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            MasterInfo master;
            try (ResultSet resultSet = con.executeQuery(
                     "select city, phone, male, age, wallet_id, wallet_num from master_info where user_id=?", userId))
            {
                if (!resultSet.next())  throw new IllegalStateException("master_info doesn't exist for user_id="+userId);
                master = new MasterInfo();
                int i = 0;
                master.city = resultSet.getString(++i);
                master.phone = resultSet.getString(++i);
                master.male = (Boolean)resultSet.getObject(++i);
                master.age = (Integer)resultSet.getObject(++i);
                master.walletType = resultSet.getInt(++i);
                master.walletNum = resultSet.getString(++i);
            }
            master.devices = loadMasterDevices(userId, con);
            return master;
        }
    }

    public List<MasterStat> loadMastersStatistics(Date startDate, Date endDate, boolean showZero) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            StringBuilder query = new StringBuilder("select user.id, login, user.created, city, phone, male, age, wallet_id, wallet_num, count(task_id), sum(price) " +
                                                    "from master_info inner join user on user.id=user_id " +
                                                    (showZero ? "left" : "inner")+" join master_task on user.id=master_id and master_task.status='"+CONFIRM+"' " +
                                                    (showZero ? "left" : "inner")+" join task on task_id=task.id ");
            appendCondition(query, "confirmed", startDate, endDate, true);
            query.append("group by user.id, login, user.created, city, phone, male, age, wallet_id, wallet_num");

            try (ResultSet rs = con.executeQuery(query.toString(), params(startDate, endDate)))  {
                ArrayList<MasterStat> result = new ArrayList<>();
                while (rs.next())
                {
                    MasterStat stat = new MasterStat();
                    int i = 0;
                    stat.id = rs.getInt(++i);
                    stat.login = rs.getString(++i);
                    stat.created = rs.getDate(++i);
                    stat.city = rs.getString(++i);
                    stat.phone = rs.getString(++i);
                    stat.male = (Boolean)rs.getObject(++i);
                    stat.age = (Integer)rs.getObject(++i);
                    stat.walletType = rs.getInt(++i);
                    stat.walletNum = rs.getString(++i);
                    stat.confirmedCount = rs.getInt(++i);
                    stat.confirmedAmount = rs.getInt(++i);
                    result.add(stat);
                }
                return result;
            }
        }
    }

    public List<Integer> loadMasterDevices(int userId) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            return loadMasterDevices(userId, con);
        }
    }

    private List<Integer> loadMasterDevices(int userId, UtilConnection con) throws SQLException
    {
        try (ResultSet resultSet = con.executeQuery("select device_id from master_device where user_id=? order by device_id", userId))
        {
            List<Integer> result = new ArrayList<>();
            while (resultSet.next())  {
                result.add(resultSet.getInt(1));
            }
            return result;
        }
    }

    public List<Wallet> loadWallets() throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection());
             ResultSet resultSet = con.executeQuery("select id, title from wallet_type order by id"))
        {
            List<Wallet> wallets = new ArrayList<>();
            while (resultSet.next())
            {
                Wallet wallet = new Wallet();
                int i = 0;
                wallet.id = resultSet.getInt(++i);
                wallet.title = resultSet.getString(++i);
                wallets.add(wallet);
            }
            return wallets;
        }
    }

    public List<Device> loadDevices() throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection());
             ResultSet resultSet = con.executeQuery("select id, title, type from device_type order by id"))
        {
            List<Device> devices = new ArrayList<>();
            while (resultSet.next())
            {
                Device device = new Device();
                int i = 0;
                device.id = resultSet.getInt(++i);
                device.title = resultSet.getString(++i);
                device.type = resultSet.getString(++i);
                devices.add(device);
            }
            return devices;
        }
    }

    // TODO need check and update cache here

    public Task.Type[] getTaskTypes()
    {
        return taskTypeCache.values().toArray(new Task.Type[0]);
    }

    public Task.Type getTaskType(int id)
    {
        return taskTypeCache.get(id);
    }

    public void updateMasterInfo(int userId, String city, String phone, Boolean male, Integer age) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            con.executeUpdateOne("update master_info set city=?, phone=?, male=?, age=? where user_id=?",
                    city, phone, male, age, userId);
        }
    }

    public void updateMasterWallet(int userId, Integer walletType, String walletNum) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            con.executeUpdateOne("update master_info set wallet_id=?, wallet_num=? where user_id=?", walletType, walletNum, userId);
        }
    }

    public void updateMasterDevices(int userId, List<Integer> devices) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            con.setAutoCommit(false);
            try  {
                con.executeUpdate("delete from master_device where user_id=?", userId);
                try (UtilStatement statement = con.statement("insert into master_device (user_id, device_id) values (?,?)"))  {
                    for (int deviceId : devices)  statement.addBatch(userId, deviceId);
                    statement.executeBatch();
                }
                con.commit();
            }
            catch (Exception e)  {  con.rollback(e);  throw e;  }
        }
    }

    public User createUser(String login, byte[] passwordHash, UserRole role) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            Integer id = con.executeInsertOneOrZero("insert ignore into user (login, password, role) values (?, ?, ?)", login, passwordHash, role.name().toLowerCase());
            if (id == null)  return null;
            if (role == UserRole.MASTER)  con.executeUpdateOne("insert into master_info (user_id) values (?)", id);
            User user = new User();
            user.id = id;
            user.login = login;
            user.password = passwordHash;
            user.role = role;
            return user;
        }
    }

    public void updatePassword(int userId, byte[] passwordHash) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            con.executeUpdateOne("update user set password=? where id=?", passwordHash, userId);
        }
    }

    public void addFeedback(int userId, String subject, String content) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            con.executeUpdateOne("insert into feedback (user_id, subject, content) values (?, ?, ?)", userId, subject, content);
        }
    }

    public List<Task> loadAllTasks(Date startDate, Date endDate) throws SQLException
    {
        StringBuilder query = new StringBuilder();
        query.append("select "+TOTAL_TASK_FIELDS+" from task left join master_task on task_id=task.id and master_task.status='"+CONFIRM+"' ");
        query.append("where task.status <> '"+CLOSE+"' ");
        appendCondition(query, "created", startDate, endDate, false);
        query.append("group by " + TASK_FIELDS + " ");
        query.append("order by id limit 1000");

        try (UtilConnection con = new UtilConnection(dataSource.getConnection());
             ResultSet resultSet = con.executeQuery(query.toString(), params(startDate, endDate)))
        {
            TaskTypeLoader typeLoader = taskTypeLoader(con);
            List<Task> result = new ArrayList<>();
            while (resultSet.next())  {
                result.add(mapTotalTask(resultSet, typeLoader));
            }
            return result;
        }
    }

    private void appendCondition(StringBuilder builder, String field, Date startDate, Date endDate, boolean first)  {
        if (startDate!=null)  {  builder.append(first ? "where " : "and ").append(field).append(">=? ");  first = false;  }
        if (endDate!=null)  {  builder.append(first ? "where " : "and ").append(field).append("<? ");  first = false;  }
    }

    private Object[] params(Date startDate, Date endDate)  {
        return startDate!=null && endDate!=null ? new Object[] {startDate,endDate} :
               startDate!=null ? new Object[] {startDate} : endDate!=null ? new Object[] {endDate} : new Object[] {};
    }

    private Object[] excludeNulls(Object... params)  {
        int count = 0;
        for (Object par : params)  if (par!=null)  count++;
        if (count == params.length)  return params;
        Object[] result = new Object[count];
        for (int i=0, j=0; i<params.length; i++)  if (params[i]!=null)  result[j++] = params[i];
        return result;
    }

    public List<MasterTask> loadMasterTasks(Date startDate, Date endDate) throws SQLException
    {
        StringBuilder query = new StringBuilder("select "+MASTER_TASK_FIELDS+", login " +
                                                "from task inner join master_task on task_id=task.id " +
                                                "left join user on master_task.master_id=user.id " +
                                                "where task.status <> '"+CLOSE+"' ");
        appendCondition(query, "taken", startDate, endDate, false);
        query.append("order by id limit 1000");

        try (UtilConnection con = new UtilConnection(dataSource.getConnection());
             ResultSet resultSet = con.executeQuery(query.toString(), params(startDate, endDate)))
        {
            TaskTypeLoader typeLoader = taskTypeLoader(con);
            List<MasterTask> result = new ArrayList<>();
            while (resultSet.next())  {
                result.add(mapMasterTaskWithLogin(resultSet, typeLoader));
            }
            return result;
        }
    }

    public List<MasterTask> loadTasks(int masterId, MasterTaskStatus status, Date confirmStartDate, Date confirmEndDate) throws SQLException
    {
        StringBuilder query = new StringBuilder("select "+MASTER_TASK_FIELDS+" " +
                                                "from task, master_task " +
                                                "where task_id=task.id and master_id=? and master_task.status=? ");
        appendCondition(query, "confirmed", confirmStartDate, confirmEndDate, false);
        query.append("order by id limit 1000");

        try (UtilConnection con = new UtilConnection(dataSource.getConnection());
             ResultSet resultSet = con.executeQuery(query.toString(), excludeNulls(masterId, status.name().toLowerCase(), confirmStartDate, confirmEndDate)))
        {
            return loadMasterTasks(resultSet, con);
        }
    }

    // TODO еще бы неплохо проверять возможность взять задачу при отображении соответствующей кнопки для мастера; вообще еще неплохобы это условие с "select count(*) ..." куда-то вынести

    public List<MasterTask> loadOpenTasks(TaskStatus status, List<Integer> devices, int masterId) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection());
             ResultSet resultSet = con.executeQuery(
                     "select "+MASTER_TASK_FIELDS+" " +
                     "from task left join master_task on task_id=task.id and master_id=? " +
                     "where task.status=? and device_id in (?) and master_task.status is null" +
                     " and (task.count_limit=0 or task.count_limit > (select count(*) from master_task where task_id=task.id)) " +
                     "order by id limit 1000", masterId, status.name().toLowerCase(), devices))
        {
            return loadMasterTasks(resultSet, con);
        }
    }

    public void loadTasksToReturn(Consumer<MasterTask> consumer) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection());
             ResultSet resultSet = con.executeQuery(
                     "select "+MASTER_TASK_FIELDS+" from task left join master_task on task_id=task.id " +
                     "where task.time_limit>0 and master_task.status='"+WORK+"'"))
        {
            TaskTypeLoader typeLoader = taskTypeLoader(con);
            while (resultSet.next()) {
                consumer.accept(mapMasterTask(resultSet, typeLoader));
            }
        }
    }

    private List<Task> loadTasks(ResultSet resultSet, UtilConnection con) throws SQLException
    {
        TaskTypeLoader typeLoader = taskTypeLoader(con);
        List<Task> result = new ArrayList<>();
        while (resultSet.next())  {
            result.add(mapTask(resultSet, typeLoader));
        }
        return result;
    }

    private List<MasterTask> loadMasterTasks(ResultSet resultSet, UtilConnection con) throws SQLException
    {
        TaskTypeLoader typeLoader = taskTypeLoader(con);
        List<MasterTask> result = new ArrayList<>();
        while (resultSet.next())  {
            result.add(mapMasterTask(resultSet, typeLoader));
        }
        return result;
    }

    public Task loadTask(int taskId, TaskStatus status, Integer masterId) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            Task task;
            if (masterId == null)
            {
                try (ResultSet resultSet = con.executeQuery("select "+ADMIN_TASK_FIELDS+" from task where id=?" +
                                                            (status != null ? " and status='"+status.name().toLowerCase()+"'" : ""), taskId))
                {
                    if (!resultSet.next())  return null;
                    task = mapAdminTask(resultSet, taskTypeLoader(con));
                }
            }
            else
            {
                try (ResultSet resultSet = con.executeQuery("select "+MASTER_TASK_FIELDS+" " +
                                                            "from task left join master_task on task_id=? and master_id=? " +
                                                            "where id=?" +
                                                            (status != null ? " and status='"+status.name().toLowerCase()+"'" : ""), taskId, masterId, taskId))
                {
                    if (!resultSet.next())  return null;
                    task = mapMasterTask(resultSet, taskTypeLoader(con));
                }

                task.messages = new ArrayList<>();
                try (ResultSet resultSet = con.executeQuery("select type, task_message.id, user.id, user.login, content " +
                                                            "from task_message left join user on author_id=user.id " +
                                                            "where task_id=? and master_id=? " +
                                                            "order by task_message.created",
                                                            taskId, masterId))
                {
                    while (resultSet.next())
                    {
                        int i = 0;
                        TaskMessage.Type type = TaskMessage.Type.valueOf(resultSet.getString(++i).toUpperCase());
                        TaskMessage message = type == TaskMessage.Type.TEXT ? new TaskMessage.TextMessage() : new TaskMessage();
                        message.id = resultSet.getInt(++i);
                        message.type = type;
                        message.taskId = taskId;
                        int userId = resultSet.getInt(++i);
                        String userLogin = resultSet.getString(++i);
                        if (userLogin != null)  {
                            message.user = new User();
                            message.user.id = userId;
                            message.user.login = userLogin;
                        }
                        if (type == TaskMessage.Type.TEXT)  ((TaskMessage.TextMessage)message).content = new String(resultSet.getBytes(++i), DB_CHARSET);
                        task.messages.add(message);
                    }
                }
            }
            return task;
        }
    }

    public void loadTaskMessageContent(int taskMessageId, BiConsumer<TaskMessage.Type, InputStream> consumer) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection());
             ResultSet resultSet = con.executeQuery("select type, content from task_message where id=?", taskMessageId))
        {
            if (!resultSet.next())  consumer.accept(null, null);
            else  consumer.accept(TaskMessage.Type.valueOf(resultSet.getString(1).toUpperCase()), resultSet.getBinaryStream(2));
        }
    }

    private static final String START = TaskStatus.START.name().toLowerCase();
    private static final String STOP = TaskStatus.STOP.name().toLowerCase();
    private static final String CLOSE = TaskStatus.CLOSE.name().toLowerCase();
    private static final String WORK = MasterTaskStatus.WORK.name().toLowerCase();
    private static final String COMPLETE = MasterTaskStatus.COMPLETE.name().toLowerCase();
    private static final String CONFIRM = MasterTaskStatus.CONFIRM.name().toLowerCase();

    public boolean startTask(int taskId) throws SQLException  {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))  {
            return con.executeUpdateOneOrZero("update task set status='"+START+"', started=current_timestamp() where id=? and status='"+STOP+"'", taskId);
        }
    }

    public boolean stopTask(int taskId) throws SQLException  {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))  {
            return con.executeUpdateOneOrZero("update task set status='"+STOP+"', stopped=current_timestamp() where id=? and status='"+START+"'", taskId);
        }
    }

    public boolean closeTask(int taskId) throws SQLException  {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))  {
            return con.executeUpdateOneOrZero("update task set status='"+CLOSE+"', closed=current_timestamp() where id=? and status='"+STOP+"'" +
                                              " and not exists(select 1 from master_task where task_id=? and status<>'"+CONFIRM+"')", taskId, taskId);
        }
    }

    public boolean takeTask(int taskId, int masterId) throws SQLException  {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))  {
            return con.executeUpdateOneOrZero(
                    "insert ignore into master_task (master_id, task_id, status) " +  // check master_task already exists
                    "select ?, id, '"+WORK+"' from task where id=? and status='"+START+"'" +  // check task exists and STARTed
                    " and (count_limit=0 or count_limit > (select count(*) from master_task where task_id=?))",  // check count_limit < already worked
                masterId, taskId, taskId);
        }
    }

    public boolean returnTask(int taskId, int masterId) throws SQLException  {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))  {
            return con.executeUpdateOneOrZero("delete from master_task where master_id=? and task_id=? and status='"+WORK+"'",
                    masterId, taskId);
        }
    }

    private static final String IMAGE_TYPES =
            "('" + Util.toString(
                TaskMessage.Type.IMAGES.stream().map(TaskMessage.Type::name).map(String::toUpperCase).collect(Collectors.toList()), "','")
            + "')";

    public boolean completeTask(int taskId, int masterId) throws SQLException  {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))  {
            //  check screenshot (теоретически это надо делать в той же транзакции, но пофиг)
            try (ResultSet rs = con.executeQuery("select 1 from task_message where task_id=? and master_id=? and author_id=? and type in " + IMAGE_TYPES, taskId, masterId, masterId))  {
                if (!rs.next())  return false;  //don't have a screenshot
            }
            //  update status
            return con.executeUpdateOneOrZero("update master_task set status='"+COMPLETE+"', completed=current_timestamp() where master_id=? and task_id=? and status='"+WORK+"'",
                    masterId, taskId);
        }
    }

    public boolean resumeTask(int taskId, int masterId) throws SQLException  {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))  {
            return con.executeUpdateOneOrZero("update master_task set status='"+WORK+"', taken=current_timestamp() where master_id=? and task_id=? and status='"+COMPLETE+"'",
                    masterId, taskId);
        }
    }

    public boolean confirmTask(int taskId, int masterId, int adminId) throws SQLException {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))  {
            return con.executeUpdateOneOrZero("update master_task set status='"+CONFIRM+"', confirmer_id=?, confirmed=current_timestamp() where master_id=? and task_id=? and status='"+COMPLETE+"'",
                    adminId, masterId, taskId);
        }
    }

    public void createTask(int userId, Integer typeId, int deviceId, int price, int countLimit, int timeLimit, String title, String description) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            con.executeUpdateOne("insert into task (creator_id, type_id, device_id, price, count_limit, time_limit, title, description) values (?,?,?,?,?,?,?,?)",
                    userId, typeId, deviceId, price, countLimit, timeLimit, title, description);
        }
    }

    public void updateTask(int taskId, Integer typeId, int deviceId, int price, int countLimit, int timeLimit, String title, String description) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            con.executeUpdateOne("update task set device_id=?, type_id=?, price=?, count_limit=?, time_limit=?, title=?, description=? where id=?",
                    deviceId, typeId, price, countLimit, timeLimit, title, description, taskId);
        }
    }

//    public boolean deleteTask(int taskId) throws SQLException
//    {
//        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))  {
//            return con.executeUpdateOneOrZero("update task set status='"+DELETE+"' where id=? and status<>"+DELETE, taskId);
//        }
//    }

    public boolean addTaskMessage(int taskId, int masterId, int authorId, String message) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            return con.executeUpdateOneOrZero(
                    "insert into task_message (task_id, master_id, author_id, type, content) " +
                    "select task_id, master_id, ?, ?, ? from master_task where task_id=? and master_id=?",
                authorId, TaskMessage.Type.TEXT.name().toLowerCase(), message.getBytes(DB_CHARSET), taskId, masterId);
        }
    }

    public boolean addTaskMessage(int taskId, int masterId, int authorId, TaskMessage.Type type, InputStream image) throws SQLException
    {
        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            return con.executeUpdateOneOrZero(
                    "insert into task_message (task_id, master_id, author_id, type, content) " +
                    "select task_id, master_id, ?, ?, ? from master_task where task_id=? and master_id=?",
                authorId, type.name().toLowerCase(), image, taskId, masterId);
        }
    }

    public int calcPayments(int masterId, Date startDate, Date endDate) throws SQLException
    {
        StringBuilder query = new StringBuilder(
                "select sum(price) from task inner join master_task on id=task_id where master_id=? and master_task.status='"+CONFIRM+"' ");
        appendCondition(query, "confirmed", startDate, endDate, false);

        try (UtilConnection con = new UtilConnection(dataSource.getConnection()))
        {
            return con.executeIntQuery(query.toString(), excludeNulls(masterId, startDate, endDate));
        }
    }

    private final Object taskTypeCacheMonitor = new Object();
    private volatile HashMap<Integer, Task.Type> taskTypeCache = null;
    private volatile LocalDateTime taskTypeCacheLastUpdate = null;

    private static final int taskTypeCacheUpdatePeriod = 300;  //если элементы только добавляются, то можно вообще не перезагружать по расписанию

    private Task.Type getTaskType(UtilConnection con, int id) throws SQLException  {
        LocalDateTime lastUpdate = taskTypeCacheLastUpdate;
        Task.Type type = lastUpdate==null || !lastUpdate.isAfter(LocalDateTime.now().minusSeconds(taskTypeCacheUpdatePeriod)) ? null
                : taskTypeCache.get(id);
        if (type == null) {
            synchronized (taskTypeCacheMonitor) {  // double checking
                lastUpdate = taskTypeCacheLastUpdate;
                type = lastUpdate==null || !lastUpdate.isAfter(LocalDateTime.now().minusSeconds(taskTypeCacheUpdatePeriod)) ? null
                        : taskTypeCache.get(id);
                if (type == null) {
                    //  reload cache
                    HashMap<Integer, Task.Type> cache = new HashMap<>();
                    try (ResultSet rs = con.executeQuery("select id, title from task_type")) {
                        while (rs.next()) {
                            type = new Task.Type();
                            type.id = rs.getInt(1);
                            type.title = rs.getString(2);
                            cache.put(id, type);
                        }
                    }
                    taskTypeCache = cache;

                    //  must be found in loaded cache
                    type = cache.get(id);
                    if (type == null) {
                        throw new IllegalStateException("Task type is not found: " + id);
                    }
                }
            }
        }
        return type;
    }

    private interface TaskTypeLoader {
        Task.Type get(int id) throws SQLException;
    }

    private TaskTypeLoader taskTypeLoader(UtilConnection con) {
        return new TaskTypeLoader() {
            HashMap<Integer, Task.Type> cache = taskTypeCache;
            LocalDateTime lastUpdate = taskTypeCacheLastUpdate;
            boolean reloaded = false;

            public Task.Type get(int id) throws SQLException {
                Task.Type type = lastUpdate==null || !lastUpdate.isAfter(LocalDateTime.now().minusSeconds(taskTypeCacheUpdatePeriod))
                        ? null : cache.get(id);
                if (type == null && !reloaded) {
                    synchronized (taskTypeCacheMonitor) {  // double checking
                        lastUpdate = taskTypeCacheLastUpdate;
                        cache = taskTypeCache;
                        type = lastUpdate==null || !lastUpdate.isAfter(LocalDateTime.now().minusSeconds(taskTypeCacheUpdatePeriod))
                                ? null : cache.get(id);
                        if (type == null) {
                            //  reload cache
                            cache = new HashMap<>();
                            try (ResultSet rs = con.executeQuery("select id, title from task_type")) {
                                while (rs.next()) {
                                    type = new Task.Type();
                                    type.id = rs.getInt(1);
                                    type.title = rs.getString(2);
                                    cache.put(type.id, type);
                                }
                            }
                            taskTypeCache = cache;
                            taskTypeCacheLastUpdate = lastUpdate = LocalDateTime.now();
                            reloaded = true;

                            //  must be found in loaded cache
                            type = cache.get(id);
                        }
                    }
                }
                if (type == null)  throw new IllegalStateException("Task type is not found: " + id);
                return type;
            }
        };
    }

    private static final String TASK_FIELDS = "task.id, task.type_id, task.device_id, task.price, task.title, task.description, task.status, task.created, task.started, task.stopped";
    private static final String ADMIN_TASK_FIELDS = TASK_FIELDS + ", task.count_limit, task.time_limit";
    private static final String TOTAL_TASK_FIELDS = ADMIN_TASK_FIELDS+", count(master_task.task_id)";
    private static final String MASTER_TASK_FIELDS = TASK_FIELDS+", task.time_limit, master_task.master_id, master_task.status, master_task.taken, master_task.completed, master_task.confirmed";

    private <T extends Task> T mapTask(ResultSet resultSet, T task, TaskTypeLoader typeLoader) throws SQLException
    {
        int i = 0;
        task.id = resultSet.getInt(++i);
        int typeId = resultSet.getInt(++i);
        task.type = resultSet.wasNull() ? null : typeLoader.get(typeId);
        task.deviceId = resultSet.getInt(++i);
        task.price = resultSet.getInt(++i);
        task.title = resultSet.getString(++i);
        task.description = resultSet.getString(++i);
        task.state = TaskStatus.valueOf(resultSet.getString(++i).toUpperCase());
        task.created = resultSet.getTimestamp(++i);
        task.started = resultSet.getTimestamp(++i);
        task.stopped = resultSet.getTimestamp(++i);
        return task;
    }

    private Task mapTask(ResultSet resultSet, TaskTypeLoader typeLoader) throws SQLException
    {
        return mapTask(resultSet, new Task(), typeLoader);
    }

    private Task mapAdminTask(ResultSet resultSet, TaskTypeLoader typeLoader) throws SQLException
    {
        Task task = mapTask(resultSet, typeLoader);
        int i = 10;
        task.countLimit = resultSet.getInt(++i);
        task.timeLimit = resultSet.getInt(++i);
        return task;
    }

    private Task mapTotalTask(ResultSet resultSet, TaskTypeLoader typeLoader) throws SQLException
    {
        Task task = mapAdminTask(resultSet, typeLoader);
        int i = 12;
        task.confirmedCount = resultSet.getInt(++i);
        return task;
    }

    private MasterTask mapMasterTask(ResultSet resultSet, TaskTypeLoader typeLoader) throws SQLException
    {
        MasterTask task = mapTask(resultSet, new MasterTask(), typeLoader);
        int i = 10;
        task.timeLimit = resultSet.getInt(++i);
        task.masterId = resultSet.getInt(++i);
        String status = resultSet.getString(++i);
        task.status = status == null ? null : MasterTaskStatus.valueOf(status.toUpperCase());
        task.taken = resultSet.getTimestamp(++i);
        task.completed = resultSet.getTimestamp(++i);
        task.confirmed = resultSet.getTimestamp(++i);
        return task;
    }

    private MasterTask mapMasterTaskWithLogin(ResultSet resultSet, TaskTypeLoader typeLoader) throws SQLException
    {
        MasterTask task = mapMasterTask(resultSet, typeLoader);
        int i = 16;
        task.masterLogin = resultSet.getString(++i);
        return task;
    }
}

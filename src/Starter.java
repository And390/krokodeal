import com.zaxxer.hikari.HikariDataSource;
import data.DataAccess;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import ru.and390.template.TemplateManager;
import ru.and390.template.WatchFileTemplateManager;
import service.MailSender;
import util.Config;
import web.AppExceptionMapper;
import web.EndPoint;

import javax.print.attribute.standard.Media;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class Starter {

    private static <T> T add(Collection<? super T> collection, T elem)  {  collection.add(elem);  return elem;  }

    public static void main(String[] args) throws Exception {

        List<AutoCloseable> closeables = new ArrayList<>();

        try  {

        //  db
        HikariDataSource dataSource = add(closeables, DataAccess.createDataSource());

        //  template manager
        TemplateManager templateManager = add(closeables, new WatchFileTemplateManager(new File("web"), "UTF-8"));

        //  initialize components
        DataAccess dataAccess = new DataAccess(dataSource);
        MailSender mailSender = new MailSender();
        EndPoint endPoint = new EndPoint(dataAccess, mailSender, templateManager);

        //  initialize server
        int port = Config.getUInt("server.port");
        Server jettyServer = new Server(port);

        ResourceConfig config = new ResourceConfig();
        config.register(MultiPartFeature.class);
        config.register(endPoint);
        config.register(new AppExceptionMapper(templateManager));
        ServletContainer jerseyServlet = new ServletContainer(config);

        ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletHandler.setContextPath("/");
        servletHandler.addServlet(new ServletHolder(jerseyServlet), "/*");

        ResourceHandler resourceHandler = new ResourceHandler();
        //resourceHandler.setDirectoriesListed(true);
        //resourceHandler.setWelcomeFiles(new String[]{ "login.html" });
        resourceHandler.setResourceBase("web");

        // fucken jetty - ResourceHandler gobbles root path; also sessions - double fuck
        SessionHandler indexHandler = new SessionHandler(servletHandler.getSessionHandler().getSessionManager());
        indexHandler.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                if (request.getRequestURI().equals("/")) {
                    baseRequest.setHandled(true);
                    try {
                        String result = endPoint.getMain(request);
                        response.setContentType(MediaType.TEXT_HTML);
                        response.getOutputStream().write(result.getBytes("UTF-8"));
                    } catch (IOException|ServletException|RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { indexHandler, resourceHandler, servletHandler });
        jettyServer.setHandler(handlers);

        try {
            jettyServer.start();
            System.out.println("Server started on port " + port);
            jettyServer.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jettyServer.destroy();
        }

        }  finally  {
            for (AutoCloseable closeable : closeables)
                try  {  closeable.close();  }
                catch (Throwable e)  {  e.printStackTrace();  }
        }
    }
}

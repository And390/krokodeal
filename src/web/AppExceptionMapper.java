package web;

import data.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.and390.template.TemplateManager;

import javax.script.Bindings;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.net.URI;
import java.net.URISyntaxException;


public class AppExceptionMapper extends AbstractEndPoint implements ExceptionMapper<Exception> {

    private static Logger log = LoggerFactory.getLogger(AppExceptionMapper.class);

    private TemplateManager templateManager;

    @Context
    private HttpServletRequest servletRequest;

    private URI mainPage;

    public AppExceptionMapper(TemplateManager templateManager) throws URISyntaxException  {
        this.templateManager = templateManager;
        mainPage = new URI(ROOT+"/");
    }

    public Response toResponse(Exception error) {
        if (error instanceof WebApplicationException)  return ((WebApplicationException)error).getResponse();
        if (!(error instanceof ClientException))  log.error("Error processing request", error);

        Response.ResponseBuilder response = Response.status(
                error instanceof ClientException ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR);
        String clientMessage = error instanceof ClientException ? error.getMessage() : "Внутренняя ошибка сервера";
        boolean unauthorized = error instanceof ClientException && error.getMessage() == UNAUTHORIZED;
        boolean denied = error instanceof ClientException && error.getMessage() == DENIED;
        boolean peaceful = error instanceof ClientException && ((ClientException) error).peaceful;
        if (servletRequest.getMethod().equals("GET"))
        {
            if (denied) {
                return Response.temporaryRedirect(mainPage).build();
            }
            try  {
                Bindings bindings = createBindings(servletRequest, (User)servletRequest.getSession().getAttribute("user"));
                bindings.put("error", clientMessage);
                bindings.put("peaceful", peaceful);
                String templatePath = unauthorized ? "/login.html" : "/error.html";
                String result = templateManager.eval(templatePath, bindings);
                return response.entity(result).type(HTML_TYPE).build();
            }
            catch (Exception e)  {
                log.error("Error on returning error page", e);
            }
        }
        else if (servletRequest.getMethod().equals("POST"))
        {
            return response.entity(new ApiResponse(null, clientMessage, unauthorized || denied)).type(JSON_TYPE).build();
        }
        return response.entity(clientMessage).type(TEXT_TYPE).build();
	}
}

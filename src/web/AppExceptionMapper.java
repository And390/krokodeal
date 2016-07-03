package web;

import data.User;
import ru.and390.template.TemplateManager;

import javax.script.Bindings;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.net.URI;
import java.net.URISyntaxException;


public class AppExceptionMapper extends AbstractEndPoint implements ExceptionMapper<Exception> {

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
        if (!(error instanceof ClientException))  error.printStackTrace();

        Response.ResponseBuilder response = Response.status(
                error instanceof ClientException ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR);
        String clientMessage = error instanceof ClientException ? error.getMessage() : "Внутренняя ошибка сервера";
        boolean unauthorized = error instanceof ClientException && error.getMessage() == UNAUTHORIZED;
        boolean denied = error instanceof ClientException && error.getMessage() == DENIED;
        if (servletRequest.getMethod().equals("GET"))
        {
            if (denied) {
                return Response.temporaryRedirect(mainPage).build();
            }
            try  {
                Bindings bindings = createBindings(servletRequest, (User)servletRequest.getSession().getAttribute("user"));
                bindings.put("error", clientMessage);
                String templatePath = unauthorized ? "/login.html" : "/error.html";
                String result = templateManager.eval(templatePath, bindings);
                return response.entity(result).type(HTML_TYPE).build();
            }
            catch (Exception e)  {
                e.printStackTrace();
            }
        }
        else if (servletRequest.getMethod().equals("POST"))
        {
            return response.entity(new ApiResponse(null, clientMessage, unauthorized || denied)).type(JSON_TYPE).build();
        }
        return response.entity(clientMessage).type(TEXT_TYPE).build();
	}
}

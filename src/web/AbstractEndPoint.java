package web;


import data.User;
import ru.and390.template.TemplateManager;
import util.Config;
import utils.Util;

import javax.script.Bindings;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;

public abstract class AbstractEndPoint {

    public static final String UNAUTHORIZED = "Need authorization";
    public static final String DENIED = "Access denied";

    public static final String ROOT = "";

    public static final String CHARSET_TYPE = "; charset=UTF-8";
    public static final String TEXT_TYPE = MediaType.TEXT_PLAIN + CHARSET_TYPE;
    public static final String HTML_TYPE = MediaType.TEXT_HTML + CHARSET_TYPE;
    public static final String JSON_TYPE = MediaType.APPLICATION_JSON + CHARSET_TYPE;

    private HashMap<String, String> definitions = new HashMap<>();

    protected Bindings createBindings(HttpServletRequest request, User user)  {
        Bindings bindings = TemplateManager.createBindings();
        Util.getAll(Config.getProperties(), "web.def.", (k, v) -> bindings.put(k, v));
        bindings.put("root", ROOT);
        bindings.put("path", request.getRequestURI());
        bindings.put("user", user);
        return bindings;
    }

    protected static class ApiResponse {
        public Object result;
        public String error;
        public boolean reload;
        public ApiResponse(Object result_, String error_, boolean reload_)  {  result = result_;  error = error_;  reload = reload_;  }
        public static ApiResponse success()  {  return new ApiResponse("ok", null, false);  }
    }
}

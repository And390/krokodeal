package web;


import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ClientException extends RuntimeException {

    public ClientException(String message) {
        super(message);
    }
}

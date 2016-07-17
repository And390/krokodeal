package web;

public class ClientException extends RuntimeException {

    public boolean peaceful = false;

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, boolean peaceful) {
        super(message);
        this.peaceful = peaceful;
    }
}

package app.Errors;

public class FatalException extends Exception {
    public FatalException() {
        super();
    }

    public FatalException(String msg) {
        super(msg);
    }

    public FatalException(Throwable cause) {
        super(cause);
    }

    public FatalException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
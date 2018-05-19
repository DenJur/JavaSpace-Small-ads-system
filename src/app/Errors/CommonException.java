package app.Errors;

public class CommonException extends Exception {
    public CommonException() {
        super();
    }

    public CommonException(String msg) {
        super(msg);
    }

    public CommonException(Throwable cause) {
        super(cause);
    }

    public CommonException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

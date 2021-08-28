package personthecat.catlib.exception;

public class ResourceException extends RuntimeException {
    public ResourceException(final String msg) {
        super(msg);
    }

    public ResourceException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

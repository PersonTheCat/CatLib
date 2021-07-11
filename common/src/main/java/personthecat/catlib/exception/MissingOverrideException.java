package personthecat.catlib.exception;

public class MissingOverrideException extends RuntimeException {
    public MissingOverrideException() {
        super("Method is missing override in child source set");
    }
}

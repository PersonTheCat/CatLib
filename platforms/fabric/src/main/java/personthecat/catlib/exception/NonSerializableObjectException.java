package personthecat.catlib.exception;

public class NonSerializableObjectException extends Exception {
    private NonSerializableObjectException(final String msg) {
        super(msg);
    }

    public static NonSerializableObjectException unsupportedKey(final Object key) {
        return new NonSerializableObjectException("Cannot serialize map of type " + key.getClass() + ". Keys must be strings.");
    }

    public static NonSerializableObjectException defaultRequired() {
        return new NonSerializableObjectException("Cannot serialize object. Generic types must have defaults.");
    }
}

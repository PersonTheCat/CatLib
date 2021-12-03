package personthecat.catlib.exception;

public class UncheckedFormattedException extends RuntimeException {
    public UncheckedFormattedException(final FormattedException cause) {
        super(cause);
    }

    @Override
    public FormattedException getCause() {
        return (FormattedException) super.getCause();
    }
}

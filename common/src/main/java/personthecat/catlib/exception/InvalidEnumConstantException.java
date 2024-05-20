package personthecat.catlib.exception;

import java.util.Arrays;

import static personthecat.catlib.util.LibUtil.f;

public class InvalidEnumConstantException extends RuntimeException {
    public InvalidEnumConstantException(final String name, final Class<? extends Enum<?>> clazz) {
        super(createMessage(name, clazz));
    }

    private static String createMessage(final String name, final Class<? extends Enum<?>> clazz) {
        final String values = Arrays.toString(clazz.getEnumConstants());
        return f("{} \"{}\" does not exist. Valid options are: {}", clazz.getSimpleName(), name, values);
    }
}

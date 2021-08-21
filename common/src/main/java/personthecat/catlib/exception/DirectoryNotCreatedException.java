package personthecat.catlib.exception;

import java.io.File;

import static personthecat.catlib.util.Shorthand.f;

public class DirectoryNotCreatedException extends RuntimeException {
    public DirectoryNotCreatedException(final File f) {
        super(f("A required directory was not created: ", f.getPath()));
    }

    public DirectoryNotCreatedException(final File f, final Throwable cause) {
        super(f("A required directory was not created: ", f.getPath()), cause);
    }
}

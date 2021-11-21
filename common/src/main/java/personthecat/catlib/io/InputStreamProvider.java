package personthecat.catlib.io;

import personthecat.fresult.functions.ThrowingSupplier;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamProvider {
    private final String name;
    private final ThrowingSupplier<InputStream, IOException> is;

    public InputStreamProvider(final String name, final ThrowingSupplier<InputStream, IOException> is) {
        this.name = name;
        this.is = is;
    }

    public String getName() {
        return this.name;
    }

    public InputStream getStream() throws IOException {
        return this.is.get();
    }

    public InputStreamProvider rename(final String name) {
        return new InputStreamProvider(name, this.is);
    }
}

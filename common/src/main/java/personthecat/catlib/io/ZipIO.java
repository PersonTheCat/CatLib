package personthecat.catlib.io;

import lombok.extern.log4j.Log4j2;
import personthecat.catlib.exception.ResourceException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.function.UnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Log4j2
public class ZipIO {

    public static void extract(final Path zip, final Path out) {
        if (Files.isRegularFile(out)) {
            throw new ResourceException("Expected a folder");
        }
        if (!Files.exists(zip)) {
            throw new ResourceException("Nothing to extract: " + zip);
        }
        try (final ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            doExtract(zis, out);
        } catch (final IOException e) {
            throw new ResourceException("Extracting file", e);
        }
    }

    private static void doExtract(final ZipInputStream zip, final Path out) throws IOException {
        final byte[] buffer = new byte[1024];
        ZipEntry entry = zip.getNextEntry();
        while (entry != null) {
            final Path file = createFile(out, entry);
            if (entry.isDirectory()) {
                Files.createDirectories(file);
            } else {
                Files.createDirectories(file.getParent());
                writeFile(file, buffer, zip);
            }
            entry = zip.getNextEntry();
        }
        zip.closeEntry();
    }

    private static Path createFile(final Path dir, final ZipEntry entry) throws IOException {
        final Path out = dir.resolve(entry.getName());
        if (!out.toRealPath().startsWith(dir.toRealPath())) {
            throw new IOException("Attempted to slip entry outside of target directory");
        }
        return out;
    }

    private static void writeFile(final Path file, final byte[] buffer, final ZipInputStream zip) throws IOException {
        try (final OutputStream os = Files.newOutputStream(file)) {
            int len;
            while ((len = zip.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
        }
    }

    public static void compress(final Path in, final Path zip) {
        try (final ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            doCompress(in, in.getFileName().toString(), zos);
        } catch (final IOException e) {
            throw new ResourceException("Compressing file", e);
        }
    }

    private static void doCompress(final Path in, final String name, final ZipOutputStream zip) throws IOException {
        if (Files.isHidden(in)) {
            return;
        }
        if (Files.isDirectory(in)) {
            zip.putNextEntry(new ZipEntry(name.endsWith("/") ? name : name + "/"));
            zip.closeEntry();
            try (final var files = Files.list(in)) {
                for (final Path file : files.toList()) {
                    doCompress(file, name + "/" + file.getFileName(), zip);
                }
            }
        } else {
            writeEntry(in, name, zip);
        }
    }

    private static void writeEntry(final Path in, final String name, final ZipOutputStream zip) throws IOException {
        writeEntry(Files.newInputStream(in), name, zip);
    }

    public static void transform(final Path zip, final UnaryOperator<InputStreamProvider> transformer) {
        if (!Files.exists(zip)) {
            throw new ResourceException("Nothing to transform: " + zip);
        }
        final Path temp = zip.resolveSibling(zip.getFileName() + ".temp.out");
        try (final ZipFile zf = new ZipFile(zip.toFile())) {
            try (final ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(temp))) {
                doTransform(zf, zos, transformer);
            }
        } catch (final IOException e) {
            throw new ResourceException("Transforming zip", e);
        } finally {
            try {
                Files.move(temp, zip, StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                log.error("Could not delete temporary file: {}", temp, e);
            }
        }
    }

    private static void doTransform(
        final ZipFile zf, final ZipOutputStream zos, final UnaryOperator<InputStreamProvider> transformer) throws IOException {

        final Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                writeEntry(zf.getInputStream(entry), entry.getName(), zos);
                continue;
            }
            final InputStreamProvider in = new InputStreamProvider(entry.getName(), () -> zf.getInputStream(entry));
            final InputStreamProvider out = transformer.apply(in);
            if (out != null) {
                writeEntry(out.getStream(), out.getName(), zos);
            }
        }
    }

    private static void writeEntry(final InputStream is, final String name, final ZipOutputStream zip) throws IOException {
        try (is) {
            zip.putNextEntry(new ZipEntry(name));
            final byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) >= 0) {
                zip.write(bytes, 0, len);
            }
        }
    }
}

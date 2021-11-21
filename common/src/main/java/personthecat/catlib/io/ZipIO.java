package personthecat.catlib.io;

import lombok.extern.log4j.Log4j2;
import personthecat.catlib.exception.ResourceException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.function.UnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Log4j2
public class ZipIO {

    public static void extract(final File zip, final File out) {
        if (out.isFile()) {
            throw new ResourceException("Expected a folder");
        }
        if (!zip.exists()) {
            throw new ResourceException("Nothing to extract: " + zip);
        }
        try (final ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            doExtract(zis, out);
        } catch (final IOException e) {
            throw new ResourceException("Extracting file", e);
        }
    }

    private static void doExtract(final ZipInputStream zip, final File out) throws IOException {
        final byte[] buffer = new byte[1024];
        ZipEntry entry = zip.getNextEntry();
        while (entry != null) {
            final File file = createFile(out, entry);
            if (entry.isDirectory()) {
                mkdirs(file);
            } else {
                mkdirs(file.getParentFile());
                writeFile(file, buffer, zip);
            }
            entry = zip.getNextEntry();
        }
        zip.closeEntry();
    }

    private static File createFile(final File dir, final ZipEntry entry) throws IOException {
        final File out = new File(dir, entry.getName());
        if (!out.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator)) {
            throw new IOException("Attempted to slip entry outside of target directory");
        }
        return out;
    }

    private static void mkdirs(final File file) throws IOException {
        if (!file.isDirectory() && !file.mkdirs()) {
            throw new IOException("Creating directory: " + file);
        }
    }

    private static void writeFile(final File file, final byte[] buffer, final ZipInputStream zip) throws IOException {
        try (final FileOutputStream fos = new FileOutputStream(file)) {
            int len;
            while ((len = zip.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    public static void compress(final File in, final File zip) {
        try (final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            doCompress(in, in.getName(), zos);
        } catch (final IOException e) {
            throw new ResourceException("Compressing file", e);
        }
    }

    private static void doCompress(final File in, final String name, final ZipOutputStream zip) throws IOException {
        if (in.isHidden()) {
            return;
        }
        if (in.isDirectory()) {
            zip.putNextEntry(new ZipEntry(name.endsWith("/") ? name : name + "/"));
            zip.closeEntry();
            final File[] files = in.listFiles();
            if (files != null) {
                for (final File file : files) {
                    doCompress(file, name + "/" + file.getName(), zip);
                }
            }
        } else {
            writeEntry(in, name, zip);
        }
    }

    private static void writeEntry(final File in, final String name, final ZipOutputStream zip) throws IOException {
        writeEntry(new FileInputStream(in), name, zip);
    }

    public static void transform(final File zip, final UnaryOperator<InputStreamProvider> transformer) {
        if (!zip.exists()) {
            throw new ResourceException("Nothing to transform: " + zip);
        }
        final File temp = new File(zip.getParent(), zip.getName() + ".temp.out");
        try (final ZipFile zf = new ZipFile(zip)) {
            try (final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(temp))) {
                doTransform(zf, zos, transformer);
            }
        } catch (final IOException e) {
            throw new ResourceException("Transforming zip", e);
        } finally {
            try {
                Files.move(temp.toPath(), zip.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                log.error("Could not delete temporary file: " + temp, e);
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
        try {
            zip.putNextEntry(new ZipEntry(name));
            final byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) >= 0) {
                zip.write(bytes, 0, len);
            }
        } finally {
            is.close();
        }
    }
}

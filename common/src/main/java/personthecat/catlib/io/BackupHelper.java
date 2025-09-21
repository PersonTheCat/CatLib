package personthecat.catlib.io;

import personthecat.catlib.exception.ResourceException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static personthecat.catlib.util.LibUtil.f;

/**
 * A helper object used for backing up files. Will take care of renaming
 * existing backups in the given directory according to their date, where
 * the oldest file is renamed to have the highest number.
 */
public class BackupHelper {
    final String base;
    final String ext;
    final Pattern pattern;

    BackupHelper(final Path file) {
        final String name = file.getFileName().toString();
        final int dotIndex = name.lastIndexOf(".");
        if (dotIndex > 0) {
            this.base = name.substring(0, dotIndex);
            this.ext = name.substring(dotIndex);
        } else {
            this.base = name;
            this.ext = "";
        }
        this.pattern = Pattern.compile(this.base + "(\\s\\((\\d+)\\))?" + this.ext);
    }

    int cycle(final Path dir) {
        final var matching = new ArrayList<Path>();
        FileIO.forEach(dir, path -> {
            if (this.matches(path)) {
                matching.add(path);
            }
        });
        matching.sort(this::compare);
        final int end = this.getFirstGap(matching);
        for (int i = end - 1; i >= 0; i--) {
            final Path f = matching.get(i);
            final int number = i + 1;
            final var newFile = f.getParent().resolve(this.base + " (" + number + ")" + this.ext);
            try {
                Files.move(f, newFile);
            } catch (final IOException e) {
                throw new ResourceException(f("Could not increment backup: {}", f.getFileName()), e);
            }
        }
        return matching.size();
    }

    boolean matches(final Path file) {
        return this.pattern.matcher(file.getFileName().toString()).matches();
    }

    private int compare(final Path f1, final Path f2) {
        return Integer.compare(this.getNumber(f1), this.getNumber(f2));
    }

    int getNumber(final Path file) {
        final Matcher matcher = this.pattern.matcher(file.getFileName().toString());
        if (!matcher.find()) throw new RuntimeException(f("Backup deleted externally: {}", file.getFileName()));
        final String g2 = matcher.group(2);
        return g2 == null ? 0 : Integer.parseInt(g2);
    }

    int getFirstGap(final List<Path> files) {
        int lastNum = 0;
        for (final Path f : files) {
            final int num = this.getNumber(f) + 1;
            if (num - lastNum > 1) {
                return lastNum;
            }
            lastNum = num;
        }
        return files.size();
    }
}

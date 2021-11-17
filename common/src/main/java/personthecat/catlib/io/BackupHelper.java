package personthecat.catlib.io;

import personthecat.catlib.exception.Exceptions;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper object used for backing up files. Will take care of renaming
 * existing backups in the given directory according to their date, where
 * the oldest file is renamed to have the highest number.
 */
public class BackupHelper {
    final String base;
    final String ext;
    final Pattern pattern;

    BackupHelper(final File file) {
        final String name = file.getName();
        final int dotIndex = name.lastIndexOf(".");
        if (dotIndex > 0) {
            base = name.substring(0, dotIndex);
            ext = name.substring(dotIndex);
        } else {
            base = name;
            ext = "";
        }
        pattern = Pattern.compile(base + "(\\s\\((\\d+)\\))?" + ext);
    }

    int cycle(final File dir) {
        final File[] arr = dir.listFiles(this::matches);
        if (arr == null || arr.length == 0) return 0;
        final List<File> matching = Arrays.asList(arr);
        matching.sort(this::compare);
        final int end = this.getFirstGap(matching);
        for (int i = end - 1; i >= 0; i--) {
            final File f = matching.get(i);
            final int number = i + 1;
            final File newFile = new File(f.getParentFile(), base + " (" + number + ")" + ext);
            if (!f.renameTo(newFile)) {
                throw Exceptions.resourceEx("Could not increment backup: {}", f.getName());
            }
        }
        return matching.size();
    }

    void truncate(final File dir, final int count) {
        final File[] arr = dir.listFiles(this::matches);
        if (arr == null || arr.length == 0) return;
        for (final File f : arr) {
            if (getNumber(f) >= count) {
                if (!f.delete()) {
                    throw Exceptions.resourceEx("Could not truncate backup: {}", f.getName());
                }
            }
        }
    }

    boolean matches(final File file) {
        return pattern.matcher(file.getName()).matches();
    }

    private int compare(final File f1, final File f2) {
        return Integer.compare(this.getNumber(f1), this.getNumber(f2));
    }

    int getNumber(final File file) {
        final Matcher matcher = pattern.matcher(file.getName());
        if (!matcher.find()) throw Exceptions.runEx("Backup deleted externally: {}", file.getName());
        final String g2 = matcher.group(2);
        return g2 == null ? 0 : Integer.parseInt(g2);
    }

    int getFirstGap(final List<File> files) {
        int lastNum = 0;
        for (File f : files) {
            final int num = this.getNumber(f) + 1;
            if (num - lastNum > 1) {
                return lastNum;
            }
            lastNum = num;
        }
        return files.size();
    }
}

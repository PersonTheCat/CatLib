package personthecat.catlib.versioning;

import org.jetbrains.annotations.NotNull;
import personthecat.fresult.Result;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Version implements Comparable<Version>, Serializable {

    public static final String SNAPSHOT = "SNAPSHOT";
    private static final int COMPONENT_SIZE = 1 << 16;

    public static final Version ZERO = new Version(0, 0, 0, "");

    private static final Pattern PATTERN =
        Pattern.compile("(\\d{1,5})(?:\\.(\\d{1,5})(?:\\.(\\d{1,5}))?)?(?:-([a-zA-Z0-9\\s:\\\\/_-]+))?");

    private final int major;
    private final int minor;
    private final int patch;
    private final String tag;
    private final long raw;
    private final String friendly;

    private Version(final int major, final int minor, final int patch, final @NotNull String tag) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.tag = tag;
        this.raw = pack(major, minor, patch);
        this.friendly = display(major, minor, patch, tag);
    }

    public static Version create(final int major, final int minor) {
        return new Version(checkRange(major), checkRange(minor), 0, "");
    }

    public static Version create(final int major, final int minor, final int patch) {
        return new Version(checkRange(major), checkRange(minor), checkRange(patch), "");
    }

    public static Version create(final int major, final int minor, final @NotNull String tag) {
        return new Version(checkRange(major), checkRange(minor), 0, tag);
    }

    public static Version create(final int major, final int minor, final int patch, final @NotNull String tag) {
        return new Version(checkRange(major), checkRange(minor), checkRange(patch), tag);
    }

    public static Result<Version, VersionParseException> tryParse(final String version) {
        return Result.<Version, VersionParseException>of(() -> parse(version)).ifErr(Result::IGNORE);
    }

    public static Version parse(final String version) {
        final Matcher matcher = PATTERN.matcher(version);
        if (!matcher.matches()) throw new DoesNotMatch(version);

        final int major = readComponent(matcher, 1);
        final int minor = readComponent(matcher, 2);
        final int patch = readComponent(matcher, 3);
        final String tag = matcher.group(4);
        return new Version(major, minor, patch, tag != null ? tag : "");
    }

    private static int readComponent(final Matcher matcher, final int group) {
        final String text = matcher.group(group);
        if (text == null) return 0;

        return checkRange(Integer.parseInt(text));
    }

    private static int checkRange(final int component) {
        if (component > COMPONENT_SIZE) throw new TooHigh(component);
        if (component < 0) throw new TooLow(component);
        return component;
    }

    private static long pack(final int major, final int minor, final int patch) {
        return (long) major << 47 | (long) minor << 31 | (long) patch << 15;
    }

    private static String display(final int major, final int minor, final int patch, final String tag) {
        final StringBuilder sb = new StringBuilder(major + "." + minor);
        if (patch != 0) {
            sb.append('.').append(patch);
        }
        if (!tag.isEmpty()) {
            sb.append('-').append(tag);
        }
        return sb.toString();
    }

    public int getMajorVersion() {
        return this.major;
    }

    public int getMinorVersion() {
        return this.minor;
    }

    public int getPatchVersion() {
        return this.patch;
    }

    public @NotNull String getTag() {
        return this.tag;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.raw) ^ this.tag.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Version) {
            return this.compareTo((Version) o) == 0;
        }
        return false;
    }

    @Override
    public int compareTo(final @NotNull Version o) {
        final int c = Long.compareUnsigned(this.raw, o.raw);
        return c != 0 ? c : this.compareTags(o);
    }

    public int compareTags(final @NotNull Version o) {
        final int c = this.tag.compareToIgnoreCase(o.tag);
        if (c != 0 && this.tag.equalsIgnoreCase(SNAPSHOT)) return 1;
        return c;
    }

    @Override
    public String toString() {
        return this.friendly;
    }

    public static class DoesNotMatch extends VersionParseException {
        private DoesNotMatch(final String input) {
            super("Version does not match pattern: " + input);
        }
    }

    public static class TooHigh extends VersionParseException {
        private TooHigh(final int component) {
            super(component + " > " + COMPONENT_SIZE);
        }
    }

    public static class TooLow extends VersionParseException {
        private TooLow(final int component) {
            super(component + " < 0");
        }
    }

    public static abstract class VersionParseException extends IllegalArgumentException {
        private VersionParseException(final String msg) {
            super(msg);
        }
    }
}

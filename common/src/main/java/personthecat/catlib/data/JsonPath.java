package personthecat.catlib.data;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static personthecat.catlib.exception.Exceptions.cmdSyntax;

/**
 * An object representing every accessor in a JSON object leading to a value.
 *
 * <p>In other words, this object is a container holding keys and indices which
 * point to a value at some arbitrary depth in a JSON array or object.
 */
public class JsonPath implements Iterable<Either<String, Integer>> {

    private final List<Either<String, Integer>> path;
    private final String raw;

    public JsonPath(final List<Either<String, Integer>> path) {
        this.path = path;
        this.raw = serialize(path);
    }

    public JsonPath(final List<Either<String, Integer>> path, final String raw) {
        this.path = path;
        this.raw = raw;
    }

    /**
     * Deserializes the given raw path into a collection of keys and indices.
     *
     * @throws CommandSyntaxException If the path is formatted incorrectly.
     * @param raw The raw JSON path being deserialized.
     * @return An object representing every accessor leading to a JSON value.
     */
    public static JsonPath parse(final String raw) throws CommandSyntaxException {
        return parse(new StringReader(raw));
    }

    /**
     * Deserializes the given raw path into a collection of keys and indices.
     *
     * @throws CommandSyntaxException If the path is formatted incorrectly.
     * @param reader A reader exposing the raw JSON path being deserialized.
     * @return An object representing every accessor leading to a JSON value.
     */
    public static JsonPath parse(final StringReader reader) throws CommandSyntaxException {
        final List<Either<String, Integer>> path = new ArrayList<>();
        final int begin = reader.getCursor();

        while(reader.canRead() && reader.peek() != ' ') {
            final char c = reader.read();
            if (c == '.') {
                checkDot(reader, begin);
            } else if (inKey(c)) {
                path.add(Either.left(c + readKey(reader)));
            } else if (c == '[') {
                checkDot(reader, begin);
                path.add(Either.right(reader.readInt()));
                reader.expect(']');
            } else {
                throw cmdSyntax(reader, "Invalid character");
            }
        }
        return new JsonPath(path, reader.getString().substring(begin, reader.getCursor()));
    }

    private static String readKey(final StringReader reader) {
        final int start = reader.getCursor();
        while (reader.canRead() && inKey(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    private static boolean inKey(final char c) {
        return c != '.' && c != ' ' && c != '[';
    }

    private static void checkDot(final StringReader reader, final int begin) throws CommandSyntaxException {
        final int cursor = reader.getCursor();
        final char last = reader.getString().charAt(cursor - 2);
        if (cursor - 1 == begin || last == '.') {
            throw cmdSyntax(reader, "Unexpected accessor");
        }
    }

    /**
     * Converts the given JSON path data into a raw string.
     *
     * @param path The parsed JSON path being serialized.
     * @return A string representing the equivalent path.
     */
    public static String serialize(final Collection<Either<String, Integer>> path) {
        final StringBuilder sb = new StringBuilder();
        for (final Either<String, Integer> either : path) {
            either.ifLeft(s -> {
                sb.append('.');
                sb.append(s);
            });
            either.ifRight(i -> {
                sb.append('[');
                sb.append(i);
                sb.append(']');
            });
        }
        final String s = sb.toString();
        return s.startsWith(".") ? s.substring(1) : s;
    }

    public Collection<Either<String, Integer>> asCollection() {
        return Collections.unmodifiableCollection(this.path);
    }

    public String asRawPath() {
        return this.raw;
    }

    public boolean isEmpty() {
        return this.path.isEmpty();
    }

    public int size() {
        return this.path.size();
    }

    public Either<String, Integer> get(final int index) {
        return this.path.get(index);
    }

    public List<Either<String, Integer>> subList(int s, int e) {
        return this.path.subList(s, e);
    }

    @NotNull
    @Override
    public Iterator<Either<String, Integer>> iterator() {
        return this.path.iterator();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof JsonPath) {
            return this.path.equals(((JsonPath) o).path);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.path.hashCode();
    }

    @Override
    public String toString() {
        return this.raw;
    }
}

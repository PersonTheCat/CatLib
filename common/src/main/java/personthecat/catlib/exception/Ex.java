package personthecat.catlib.exception;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lombok.experimental.UtilityClass;
import personthecat.catlib.util.Sh;

@UtilityClass
@SuppressWarnings("unused")
public class Ex {

    /**
     * Returns a clean-looking, general-purpose {@link RuntimeException}.
     *
     * @param x The error message to display.
     * @return A new {@link RuntimeException}.
     */
    public static RuntimeException runtime(final String x) {
        return new RuntimeException(x);
    }

    /**
     * Converts any standard exception into a {@link RuntimeException}.
     *
     * @param e The exception being wrapped.
     * @return A new {@link RuntimeException}.
     */
    public static RuntimeException runtime(final Throwable e) {
        return new RuntimeException(e);
    }

    /**
     * Shorthand for {@link RuntimeException#RuntimeException(String, Throwable)}.
     *
     * @param x The error message to display.
     * @param e The exception being wrapped.
     * @return A new {@link RuntimeException}.
     */
    public static RuntimeException runtime(final String x, final Throwable e) {
        return new RuntimeException(x, e);
    }

    /**
     * Shorthand for a {@link RuntimeException} using {@link Sh#f}.
     *
     * @param x The string template being interpolated.
     * @param args The interpolated arguments replacing <code>{}</code>.
     * @return A new {@link RuntimeException}.
     */
    public static RuntimeException runtimeF(final String x, final Object... args) {
        return new RuntimeException(Sh.f(x, args));
    }

    /**
     * Shorthand for a {@link ResourceException} using {@link Sh#f}.
     *
     * @param x The string template being interpolated.
     * @param args The interpolated arguments replacing <code>{}</code>.
     * @return A new {@link ResourceException}.
     */
    public static ResourceException resourceF(final String x, final Object... args) {
        return new ResourceException(Sh.f(x, args));
    }

    /**
     * Shorthand for a simple {@link CommandSyntaxException}.
     *
     * @param reader The reader being used to parse an argument.
     * @param msg The error message to display.
     * @return A new {@link CommandSyntaxException}.
     */
    public static CommandSyntaxException cmd(final StringReader reader, final String msg) {
        final int cursor = reader.getCursor();
        final String input = reader.getString().substring(0, cursor);
        final Message m = new LiteralMessage(msg);
        return new CommandSyntaxException(new SimpleCommandExceptionType(m), m, input, cursor);
    }
}

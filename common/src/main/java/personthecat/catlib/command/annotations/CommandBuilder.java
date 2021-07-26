package personthecat.catlib.command.annotations;

import personthecat.catlib.command.LibCommandBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method returns a {@link LibCommandBuilder} and should be
 * added into the registration context when processing this class file.
 * <p>
 *   This annotation should only be added to <b>static</b> methods consuming
 *   no parameters and returning an instance of {@link LibCommandBuilder}.
 * </p><p>
 *   For example:
 * </p><pre>
 *     &copy;CommandBuilder
 *     private static LibCommandBuilder myCommand() {
 *         return LibCommandBuilder.named("command")
 *             .append("Runs a generic command")
 *             .wrap("", wrapper -> wrapper.sendMessage("Hello, world!"))
 *             .generate((builder, wrappers) ->
 *                 builder.executes(wrappers.get("")));
 *     }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandBuilder {}

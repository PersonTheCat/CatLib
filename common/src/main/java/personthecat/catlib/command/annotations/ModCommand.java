package personthecat.catlib.command.annotations;

import com.mojang.brigadier.builder.ArgumentBuilder;
import personthecat.catlib.command.CommandContextWrapper;
import personthecat.catlib.command.CommandSide;
import personthecat.catlib.command.CommandType;
import personthecat.catlib.linting.SyntaxLinter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method should be passed into the registration context as
 * the body of a command. Annotated methods should be <b>static</b> and accept
 * a single parameter: {@link CommandContextWrapper}.
 * <p>
 *   To begin using this annotation, provide a single value: <code>name</code>.
 *   This token will become the literal argument required for using the
 *   command. Here's an example of a basic command using this setup:
 * </p><pre>
 *     &#064;ModCommand(name = "command")
 *     private static void myCommand(final CommandContextWrapper wrapper) {
 *         wrapper.sendMessage("Hello, world!");
 *     }
 * </pre><p>
 *   In addition, personthecat.catlib.CatLib will take care of generating a help command for your
 *   mod. You can enable this feature per command by providing a description.
 * </p><pre>
 *     &#064;ModCommand(
 *         name = "command",
 *         description = {
 *             "Runs the demo command. You can provide additional lines,",
 *             "but they will be formatted and wrapped automatically."
 *         }
 *     )
 *     private static void myCommand(final CommandContextWrapper wrapper) {}
 * </pre><p>
 *   Most commands are substantially more complicated than this setup allows.
 *   You can begin adding additional arguments to your command by providing a
 *   branch of {@link Node}s.
 * </p><pre>
 *     &#064;ModCommand(
 *         name = "command",
 *         description = "Runs the demo command.",
 *         branch = {
 *             &#064;Node(name = "arg1", type = BlockStateArgument.class),
 *             &#064;Node(name = "arg2", type = ItemInput.class, optional = true)
 *         }
 *     )
 *     private static void myCommand(final CommandContextWrapper wrapper) {
 *         // The first argument must exist:
 *         final BlockState block = wrapper.getBlock("arg1");
 *         // But the second is optional and may not:
 *         final Optional&lt;ItemInput&gt; item = wrapper.getOptional(ItemInput.class);
 *     }
 * </pre><p>
 *   To register more complex tree structures, you may need to provide multiple
 *   annotated methods with the same <code>name</code>. Brigadier will take care
 *   of resolving duplicate arguments into a single tree.
 * </p><pre>
 *     &#064;ModCommand(name = "sayHello")
 *     private static void sayHello(final CommandContextWrapper wrapper) {
 *         wrapper.sendMessage("Hello!")
 *     }
 *
 *     &#064;ModCommand(
 *         name = "sayHello"
 *         branch = @Node(name = "name", stringValue = @StringValue(type = Type.WORD))
 *     )
 *     private static void sayHelloName(final CommandContextWrapper wrapper) {
 *         wrapper.sendMessage("Hello, {}!", wrapper.getString("name"));
 *     }
 * </pre><p>
 *   For more information on setting up branches, see {@link Node}.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModCommand {

    /**
     * The first literal argument used to run this command. If absent, this will
     * default to the name of the declaring method.
     */
    String name() default "";

    /**
     * Optional alias for <code>name</code> used for declaring no-argument commands.
     *
     * <p>Note that neither value nor name is required. If both are absent, the
     * command value will default to the name of the declaring method.
     */
    String value() default "";

    /**
     * Optional subtext to display on the help page.
     *
     * <p>Note that, if this value is absent, the subtext will be generated
     * from the command node branch.
     */
    String arguments() default "";

    /**
     * The automatically-formatted help information to display on the generated
     * help page. Leave this out if you don't want the library to generate a
     * help entry for this command.
     */
    String[] description() default {};

    /**
     * An optional linter used to highlight text output in the chat. If a linter
     * is not provided, the default {@link SyntaxLinter} instance will be used
     * instead. Note that your output messages will not be linted automatically.
     * <p>
     *   You must call a variant of {@link CommandContextWrapper#sendLintedMessage}
     *   to take advantage of the feature.
     * </p>
     */
    Class<? extends SyntaxLinter>[] linter() default {};

    /**
     * An array of command node descriptors used for generating {@link ArgumentBuilder}s.
     * <p>
     *   For more information on using this feature, see {@link Node}.
     * </p>
     */
    Node[] branch() default {};

    /**
     * The type of command being generated. Either a sub command of the main mod
     * command or a global command registered to the root command node.
     */
    CommandType type() default CommandType.MOD;

    /**
     * The server side required for running this command. You can specify either
     * the dedicated or integrated server side, but by default, either side will
     * be accepted.
     */
    CommandSide side() default CommandSide.EITHER;
}

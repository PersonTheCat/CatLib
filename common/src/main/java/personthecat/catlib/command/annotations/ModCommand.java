package personthecat.catlib.command.annotations;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.api.Environment;
import personthecat.catlib.command.CommandContextWrapper;
import personthecat.catlib.command.CommandSide;
import personthecat.catlib.command.CommandType;
import personthecat.catlib.linting.SyntaxLinter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h1>@ModCommand</h1>
 * <p>
 *   Indicates that a method should be passed into the registration context as
 *   the body of a command. Annotated methods can be virtual or static, and may
 *   accept a {@link CommandContextWrapper} as the first argument, as well as
 *   named arguments which will be appended automatically to the command node.
 * </p>
 * <h2>Simple Commands</h2>
 * <p>
 *   This annotation can be used with no arguments. The method name will be
 *   split from camel case into lower-sentence case and each token will be
 *   appended to the command chain.
 * </p>
 * <p>
 *   For example, the following method generates this command:
 *   <code>/&lt;root&gt; my command</code>
 * </p>
 * <pre>{@code
 *   @ModCommand
 *   void myCommand(final CommandContextWrapper ctx) {
 *     ctx.sendMessage("Hello, world!");
 *   }
 * }</pre>
 * <h2>Renaming Commands</h2>
 * <p>
 *   To avoid pulling literal arguments from the method name, or to otherwise
 *   rename the command, provide a <b>single</b> value of <code>name</code>.
 * </p>
 * <p>
 *   For example, the method generates this command:
 *   <code>/&lt;root&gt; command</code>
 * </p>
 * <pre>{@code
 *   @ModCommand(name = "command")
 *   void command(final CommandContextWrapper ctx) {}
 * }</pre>
 * <h2>Documenting Commands</h2>
 * <p>
 *   In addition, CatLib will take care of generating a help command for your
 *   mod. You can enable this feature per command by providing a description.
 * </p>
 * <pre>{@code
 *   @ModCommand(description = "Does nothing")
 *   void command(final CommandContextWrapper ctx) {}
 * }</pre>
 * <p>
 *   Additional lines may be provided, but they will be wrapped automatically.
 * </p>
 * <pre>{@code
 *   @ModCommand(description = {
 *     "This text",
 *     "Is appended by this text"
 *   })
 *   void command(final CommandContextWrapper ctx) {}
 * }</pre>
 * <h2>Complex Command Trees</h2>
 * <p>
 *   Brigadier will automatically handle collapsing your similar commands
 *   into a single command tree. This logic is supported (but not provided)
 *   by CatLib.
 * </p>
 * <p>
 *   For example, the following methods create the following commands:
 *   <ul>
 *     <li><code>/&lt;root&gt; command a</code></li>
 *     <li><code>/&lt;root&gt; command b</code></li>
 *   </ul>
 * </p>
 * <pre>{@code
 *   @ModCommand
 *   void commandA(final CommandContextWrapper ctx) {}
 *
 *   @ModCommand
 *   void commandB(final CommandContextWrapper ctx) {}
 * }</pre>
 * <h2>Additional and Non-Literal Command Arguments</h2>
 * <p>
 *   CatLib will automatically resolve additional arguments from your method.
 * </p>
 * <p>
 *   For example, the following methods generate the following commands:
 *   <ul>
 *     <li><code>/&lt;root&gt; one a|A|b|B|c|C</code></li>
 *     <li><code>/&lt;root&gt; two &lt;block&gt;</code></li>
 *   </ul>
 * </p>
 * <pre>{@code
 *   @ModCommand
 *   void one(final CommandContextWrapper ctx, Letter arg) {}
 *
 *   @ModCommand
 *   void two(final CommandContextWrapper ctx, Block arg) {}
 *
 *   enum Letter { A, B, C }
 * }</pre>
 * <p>
 *   Other supported argument types include:
 *   <ul>
 *     <li>Primitives and boxed primitives</li>
 *     <li>Enums</li>
 *     <li>Registry types</li>
 *     <li>Arrays, lists, Optionals, and &#064;Nullables of all of the above.</li>
 *   </ul>
 * </p>
 * <p>
 *   If your argument type is not supported, or if you wish to provide
 *   constraints or command suggestions, you must pass it into the <code>branch</code>.
 *   See documentation below.
 * </p>
 * <p>
 *   Note that in some cases, you will have to provide the full node branch,
 *   excluding the root and literal command name.
 * </p>
 * <h2>Sided Commands</h2>
 * <p>
 *   To have one command be sided to the server or client, there is a <code>side</code>
 *   value; however, it is generally easier to defer to {@link Environment} or another
 *   such annotation. For example,
 * </p>
 * <pre>{@code
 *   @ModCommand
 *   @Environment(EnvType.CLIENT)
 *   void x(final CommandContextWrapper ctx) {}
 * }</pre>
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
     * Optional subtext to display on the help page. Overrides the generated subtext.
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
     *
     * <p>
     *   <b>Note</b>: it is perfectly acceptable to annotate with {@link Environment}
     *   or another such annotation instead, as this will prevent exposure to the
     *   evaluator in the first place.
     * </p>
     */
    CommandSide side() default CommandSide.EITHER;
}

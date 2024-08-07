package personthecat.catlib.command.annotations;

import com.mojang.brigadier.arguments.ArgumentType;
import personthecat.catlib.command.arguments.ListArgumentBuilder;
import personthecat.catlib.command.arguments.ArgumentSupplier;
import personthecat.catlib.command.CommandContextWrapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a single argument node in a command builder tree.
 * <p>
 *   Constructing this annotation requires a single parameter: <code>name</code>.
 *   If no other values are provided, this node will be treated as a literal
 *   argument.
 * </p><pre>
 *     branch = @Node(name = "argument")
 * </pre><p>
 *   For required arguments, the annotation provides several options for
 *   generating builders. <b>You must chose one</b>, or else the value of
 *   <code>type</code> will be chosen, as it comes first.
 * </p><pre>
 *     branch = @Node(name = "block", type = BlockStateArgument.class)
 * </pre><p>
 *   See the documentation on the individual parameters for more info on using
 *   them.
 * </p><p>
 *   In addition, the {@link Node} annotation provides support for generating
 *   list arguments (written as <code>/cmd arg1 arg2 arg3 ...</code>). This
 *   can be achieved by providing an <code>intoList</code> value, as follows:
 * </p><pre>
 *     branch = @Node(
 *         name = "files",
 *         descriptor = ArgumentSuppliers.XjsFile.class,
 *         intoList = @ListInfo(size = 3)
 *     )
 * </pre><p>
 *   Note that, in order to use a list argument type, the node must either be
 *   the last argument in the branch, or be followed by a literal argument
 *   <b>and then</b> another regular argument.
 * </p><pre>
 *     branch = {
 *       &#064;Node(
 *           name = "numbers",
 *           descriptor = ArgumentSuppliers.AnyInt.class,
 *           intoList = @ListInfo
 *       ),
 *       &#064;Node(name = "in"),
 *       &#064;Node(name = "format", type = MyArgumentType.class)
 *     }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Node {

    /**
     * The key used to retrieve this argument in-code
     */
    String name() default "";

    /**
     * Optional alias for <code>name</code> used for declaring literal argument types.
     * <pre>
     *     branch = @Node("exact")
     * </pre>
     */
    String value() default "";

    /**
     * Whether the given argument is optional. When this value is <code>true</code>, the
     * command will be able to execute <b>before</b> reaching this node.
     *
     * @see CommandContextWrapper#getOptional
     */
    boolean optional() default false;

    // Chose one

    /**
     * A <b>single</b> argument type which will parse the value for this node. This type
     * <b>must have a no-argument constructor</b> to be valid. Otherwise, you must create
     * a new {@link ArgumentSupplier} class.
     * <pre>
     *     branch = @Node(name = "a", type = ItemArgument.class)
     * </pre>
     */
    Class<? extends ArgumentType<?>>[] type() default {};

    /**
     * A <b>single</b> argument supplier which provides information about argument type
     * and suggestions needed for this node. Note that this type <b>must provide a
     * no-argument constructor</b> to be valid.
     * <pre>
     *     branch = @Node(name = "b", descriptor = ArgumentSuppliers.XjsFile.class)
     * </pre>
     */
    Class<? extends ArgumentSupplier<?>>[] descriptor() default {};

    /**
     * A <b>single</b> element type which will generate a registry argument. Note that,
     * on the Forge platform, this argument <b>will</b> pull from the Forge registries.
     * <pre>
     *     branch = @Node(name = "feature", registry = Feature.class)
     * </pre><p>
     *     This can be used as:
     * </p><pre>
     *     /&lt;mod&gt; &lt;cmd&gt; minecraft:ore_feature
     * </pre>
     */
    Class<?>[] registry() default {};

    /**
     * A <b>single</b> integer range for the current node.
     * <pre>
     *     branch = @Node(name = "x", intRange = @IntRange(max = 5))
     * </pre>
     */
    IntRange[] intRange() default {};

    /**
     * A <b>single</b> decimal range for the current node.
     * <pre>
     *     branch = @Node(name = "y", doubleRange = @DoubleRange(min = -1.0, max = 1.0))
     * </pre>
     */
    DoubleRange[] doubleRange() default {};

    /**
     * Converts this argument node into a boolean argument type.
     * <pre>
     *     branch = @Node(name = "toggle", isBoolean = true)
     * </pre>
     */
    boolean isBoolean() default false;

    /**
     * A <b>single</b> string value type for the current node.
     * <pre>
     *     branch = @Node(name = "text", stringValue = @StringValue(type = Type.GREEDY))
     * </pre>
     */
    StringValue[] stringValue() default {};

    /**
     * A <b>single</b> enum value type for the current node.
     * <pre>
     *     branch = @Node(name = "stage", enumValue = GenerationStep.Decoration.class)
     * </pre>
     */
    Class<? extends Enum<?>>[] enumValue() default {};

    /**
     * Converts the given node into a list argument which can be repeated up to a
     * maximum number of 32 times. This node must either be the last argument in
     * the branch or be followed by a literal argument <b>and then</b> any other
     * required argument type.
     * <pre>
     *     branches = {
     *         &#064;Node(name = "numbers", intVal = @IntRange(max = 100), intoList = @ListInfo),
     *         &#064;Node(name = "in"),
     *         &#064;Node(name = "format", descriptor = MyArgumentSupplier.class)
     *     }
     * </pre>
     */
    ListInfo intoList() default @ListInfo(useList = false);

    @interface IntRange {
        int min() default 0;
        int max() default Integer.MAX_VALUE;
    }

    @interface DoubleRange {
        double min() default 0.0;
        double max() default Double.MAX_VALUE;
    }

    @interface StringValue {
        Type value() default Type.WORD;
        enum Type { STRING, GREEDY, WORD }
    }

    @interface ListInfo {
        boolean useList() default true;
        int size() default ListArgumentBuilder.MAX_LIST_DEPTH;
    }
}

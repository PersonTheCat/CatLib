package personthecat.catlib.command;

import com.google.common.primitives.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import lombok.experimental.UtilityClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import personthecat.catlib.command.annotations.CommandBuilder;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.command.annotations.Node;
import personthecat.catlib.command.arguments.ArgumentDescriptor;
import personthecat.catlib.command.arguments.EnumArgument;
import personthecat.catlib.command.arguments.ListArgumentBuilder;
import personthecat.catlib.command.arguments.RegistryArgument;
import personthecat.catlib.command.LibCommandBuilder.CommandGenerator;
import personthecat.catlib.command.function.CommandFunction;
import personthecat.catlib.data.IntRef;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.LibStringUtils;
import personthecat.catlib.util.Shorthand;
import personthecat.catlib.util.unsafe.CachingReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import static personthecat.catlib.util.Shorthand.f;
import static personthecat.catlib.util.unsafe.CachingReflectionHelper.tryInstantiate;

@UtilityClass
public class CommandClassEvaluator {

    public static List<LibCommandBuilder> getBuilders(final ModDescriptor mod, final Class<?>... classes) {
        final List<LibCommandBuilder> builders = new ArrayList<>();
        for (final Class<?> c : classes) {
            final MutableObject<Object> instance = new MutableObject<>();
            addCommandBuilders(builders, instance, c);
            addModCommands(mod, builders, instance, c);
        }
        return builders;
    }

    public static List<LibCommandBuilder> getBuilders(final ModDescriptor mod, final Object... instances) {
        final List<LibCommandBuilder> builders = new ArrayList<>();
        for (final Object o : instances) {
            final MutableObject<Object> instance = new MutableObject<>(o);
            addCommandBuilders(builders, instance, o.getClass());
            addModCommands(mod, builders, instance, o.getClass());
        }
        return builders;
    }

    private static void addCommandBuilders(List<LibCommandBuilder> builders, MutableObject<Object> instance, Class<?> c) {
        forEachAnnotated(c, CommandBuilder.class, (m, a) -> {
            if (m.getParameterCount() > 0) {
                throw new CommandClassEvaluationException("{} must have no parameters", m.getName());
            }
            if (!LibCommandBuilder.class.isAssignableFrom(m.getReturnType())) {
                throw new CommandClassEvaluationException("{} must return a LibCommandBuilder", m.getName());
            }
            if (instance.getValue() == null && !Modifier.isStatic(m.getModifiers())) {
                instance.setValue(CachingReflectionHelper.tryInstantiate(c));
            }
            builders.add(getValue(m, instance.getValue()));
        });
    }

    private static void addModCommands(ModDescriptor mod, List<LibCommandBuilder> builders, MutableObject<Object> instance, Class<?> c) {
        forEachAnnotated(c, ModCommand.class, (m, a) -> {
            if (instance.getValue() == null && !Modifier.isStatic(m.getModifiers())) {
                instance.setValue(CachingReflectionHelper.tryInstantiate(c));
            }
            builders.add(createBuilder(mod, instance.getValue(), m, a));
        });
    }

    private static <A extends Annotation> void forEachAnnotated(Class<?> c, Class<A> a, BiConsumer<Method, A> command) {
        for (final Method m : c.getDeclaredMethods()) {
            final A annotation = m.getAnnotation(a);
            if (annotation != null) {
                command.accept(m, annotation);
            }
        }
    }

    private static LibCommandBuilder createBuilder(ModDescriptor mod, Object instance, Method m, ModCommand a) {
        final List<String> tokens = LibStringUtils.tokenize(m.getName());
        final CommandFunction cmd = createConsumer(instance, m);
        final List<ParsedNode> entries = createEntries(tokens, a);
        return LibCommandBuilder.named(getCommandName(tokens, a))
            .arguments(getArgumentText(entries, a))
            .description(String.join(" ", a.description()))
            .linter(a.linter().length == 0 ? mod.getDefaultLinter() : tryInstantiate(a.linter()[0]))
            .type(a.type())
            .side(a.side())
            .generate(createBranch(entries, cmd));
    }

    private static String getCommandName(List<String> tokens, ModCommand a) {
        if (!a.name().isEmpty()) return a.name();
        if (!a.value().isEmpty()) return a.value();
        return tokens.get(0);
    }

    private static String getArgumentText(List<ParsedNode> entries, ModCommand a) {
        if (!a.arguments().isEmpty()) return a.arguments();
        final StringBuilder sb = new StringBuilder();
        for (final ParsedNode entry : entries) {
            if (sb.length() > 0) sb.append(' ');
            if (entry.arg.isLiteral()) {
                sb.append(entry.name);
                continue;
            }
            if (entry.optional) sb.append('[');
            sb.append('<');
            sb.append(entry.name);
            if (entry.isList) sb.append("...");
            sb.append('>');
            if (entry.optional) sb.append(']');
        }
        return sb.toString();
    }

    private static CommandGenerator<CommandSourceStack> createBranch(List<ParsedNode> entries, CommandFunction fn) {
        return (builder, utl) -> {
            ArgumentBuilder<CommandSourceStack, ?> nextArg = null;
            final IntRef index = new IntRef(entries.size() - 1);
            final Command<CommandSourceStack> cmd = utl.wrap(fn);
            boolean optional = true;

            while (index.get() >= 0) {
                final ArgumentBuilder<CommandSourceStack, ?> argument = createArgument(entries, nextArg, cmd, index);
                if (optional) {
                    argument.executes(cmd);
                    optional = false;
                }
                final ParsedNode entry = entries.get(index.get());
                if (entry.optional || (entry.isList && index.get() == entries.size() - 1)) {
                    optional = true; // if index < end && nextEntry.canBeOmitted()
                }
                nextArg = nextArg != null ? argument.then(nextArg) : argument;
                index.decrement();
            }
            if (optional || entries.isEmpty()) {
                builder.executes(cmd);
            }
            return entries.isEmpty() ? builder : builder.then(nextArg);
        };
    }

    private static List<ParsedNode> createEntries(List<String> tokens, ModCommand a) {
        final List<ParsedNode> entries = new ArrayList<>();
        if (a.name().isEmpty() && a.value().isEmpty()) {
            addEntriesFromMethod(entries, tokens, a);
        }
        for (final Node node : a.branch()) {
            final ArgumentDescriptor<?> arg = createDescriptor(node);
            final String name = getArgumentName(node, arg.getType());
            entries.add(new ParsedNode(node, name, arg));
        }
        return entries;
    }

    private static void addEntriesFromMethod(List<ParsedNode> entries, List<String> tokens, ModCommand a) {
        for (int i = 1; i < tokens.size(); i++) {
            final String token = tokens.get(i).toLowerCase();
            for (final Node node : a.branch()) {
                if (node.name().equals(token) || node.value().equals(token)) {
                    return;
                }
            }
            entries.add(new ParsedNode(token));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ArgumentDescriptor<?> createDescriptor(Node node) {
        if (node.type().length > 0) {
            return new ArgumentDescriptor<>(tryInstantiate((Class<ArgumentType<?>>)node.type()[0]));
        } else if (node.descriptor().length > 0) {
            return tryInstantiate(node.descriptor()[0]).get();
        } else if (node.intRange().length > 0) {
            final Node.IntRange range = node.intRange()[0];
            return new ArgumentDescriptor<>(IntegerArgumentType.integer(range.min(), range.max()));
        } else if (node.doubleRange().length > 0) {
            final Node.DoubleRange range = node.doubleRange()[0];
            return new ArgumentDescriptor<>(DoubleArgumentType.doubleArg(range.min(), range.max()));
        } else if (node.isBoolean()) {
            return new ArgumentDescriptor<>(BoolArgumentType.bool());
        } else if (node.stringValue().length > 0) {
            return new ArgumentDescriptor<>(createStringArgumentType(node));
        } else if (node.enumValue().length > 0) {
            return new ArgumentDescriptor<>(EnumArgument.of((Class) node.enumValue()[0]));
        } else if (node.registry().length > 0) {
            return new ArgumentDescriptor<>(RegistryArgument.getOrThrow(node.registry()[0]));
        } else {
            return ArgumentDescriptor.LITERAL;
        }
    }

    private static ArgumentType<?> createStringArgumentType(Node node) {
        final Node.StringValue value = node.stringValue()[0];
        if (value.value() == Node.StringValue.Type.GREEDY) {
            return StringArgumentType.greedyString();
        } else if (value.value() == Node.StringValue.Type.STRING) {
            return StringArgumentType.string();
        }
        return StringArgumentType.word();
    }

    @SuppressWarnings("unchecked")
    private static ArgumentBuilder<CommandSourceStack, ?> createArgument(
            List<ParsedNode> entries, ArgumentBuilder<CommandSourceStack, ?> next,
            Command<CommandSourceStack> cmd, IntRef index) {

        final ParsedNode entry = entries.get(index.get());
        final ArgumentBuilder<CommandSourceStack, ?> argument;
        final ArgumentDescriptor<?> descriptor = entry.arg;
        final ArgumentType<?> type = descriptor.getType();

        if (entry.isList) {
            argument = createList(entries, next, cmd, index.get());
        } else if (descriptor.isLiteral()) {
            argument = Commands.literal(entry.name);
        } else {
            argument = Commands.argument(entry.name, type);
        }
        if (descriptor.getSuggestions() != null) {
            ((RequiredArgumentBuilder<CommandSourceStack, ?>) argument)
                    .suggests(descriptor.getSuggestions());
        }
        return argument;
    }

    private static String getArgumentName(Node node, ArgumentType<?> type) {
        if (!node.name().isEmpty()) return node.name();
        if (!node.value().isEmpty()) return node.value();
        return type.getClass().getSimpleName();
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createList(
            List<ParsedNode> entries, ArgumentBuilder<CommandSourceStack, ?> next,
            Command<CommandSourceStack> cmd, int index) {

        final ParsedNode entry = entries.get(index);
        final ListArgumentBuilder listBuilder =
            ListArgumentBuilder.create(entry.name, entry.arg.getType());

        if (index < entries.size() - 1) {
            final ParsedNode nextEntry = entries.get(index + 1);
            if (!nextEntry.arg.isLiteral()) {
                throw new InvalidListNodeException(entry.name);
            }
            return listBuilder.terminatedBy(next).build();
        } else {
            return listBuilder.executes(cmd).build();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getValue(Method m, Object instance) {
        m.setAccessible(true);
        try {
            return (T) m.invoke(instance);
        } catch (final IllegalAccessException | InvocationTargetException ignored) {
            throw new CommandClassEvaluationException("Could not invoke: {}", m.getName());
        }
    }

    private static CommandFunction createConsumer(Object instance, Method m) {
        m.setAccessible(true);
        final Parameter[] params = removeImplicit(m.getParameters());
        if (params.length == 0) {
            return ctx -> m.invoke(instance);
        } else if (params.length == 1 && params[0].getType().isAssignableFrom(CommandContextWrapper.class)) {
            return ctx -> m.invoke(instance, ctx);
        }
        return ctx -> m.invoke(instance, getArgs(ctx, params));
    }

    private static Parameter[] removeImplicit(Parameter[] params) {
        if (params.length == 0) return params;
        if (params[0].isImplicit()) return ArrayUtils.subarray(params, 1, params.length);
        return params;
    }

    private static Object[] getArgs(CommandContextWrapper ctx, Parameter[] params) {
        final Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            final Parameter param = params[i];
            final Class<?> type = param.getType();
            final String name = getName(param);
            if (type.isAssignableFrom(CommandContextWrapper.class)) {
                args[i] = ctx;
            } else if (type.isAssignableFrom(Optional.class)) {
                args[i] = getOptional(ctx, name, getTypeArg(param));
            } else if (type.isAssignableFrom(List.class)) {
                args[i] = getList(ctx, name, getTypeArg(param));
            } else if (type.isArray() || param.isVarArgs()) {
                final Class<?> arg = type.getComponentType();
                args[i] = toArray(arg, getList(ctx, name, arg));
            } else if (isNullable(param)) {
                args[i] = getOptional(ctx, name, type).orElse(null);
            } else {
                args[i] = get(ctx, name, type);
            }
        }
        return args;
    }

    private static String getName(final Parameter param) {
        if (!param.isNamePresent()) {
            final String methodName = param.getDeclaringExecutable().getName();
            throw new CommandClassEvaluationException("Unable to resolve real name for {}({}). Compile with -parameters", methodName, param.getName());
        }
        return param.getName();
    }

    private static Class<?> getTypeArg(final Parameter param) {
        return (Class<?>) ((ParameterizedType) param.getParameterizedType()).getActualTypeArguments()[0];
    }

    private static Object get(final CommandContextWrapper ctx, final String name, final Class<?> type) {
        if (type.isAssignableFrom(BlockState.class)) {
            final Optional<BlockState> o = ctx.getOptional(name, BlockInput.class).map(BlockInput::getState);
            if (o.isPresent()) return o.get();
        } else if (type.isAssignableFrom(Item.class)) {
            final Optional<Item> o = ctx.getOptional(name, ItemInput.class).map(ItemInput::getItem);
            if (o.isPresent()) return o.get();
        }
        return ctx.get(name, type);
    }

    private static Optional<?> getOptional(final CommandContextWrapper ctx, final String name, final Class<?> type) {
        if (type.isAssignableFrom(BlockState.class)) {
            final Optional<BlockState> o = ctx.getOptional(name, BlockInput.class).map(BlockInput::getState);
            if (o.isPresent()) return o;
        } else if (type.isAssignableFrom(Item.class)) {
            final Optional<Item> o = ctx.getOptional(name, ItemInput.class).map(ItemInput::getItem);
            if (o.isPresent()) return o;
        }
        return ctx.getOptional(name, type);
    }

    private static List<?> getList(final CommandContextWrapper ctx, final String name, final Class<?> type) {
        if (type.isAssignableFrom(BlockState.class)) {
            if (ctx.getOptional(name + "0", BlockInput.class).isPresent()) {
                return Shorthand.map(ctx.getList(name, BlockInput.class), BlockInput::getState);
            }
        } else if (type.isAssignableFrom(Item.class)) {
            if (ctx.getOptional(name + "0", ItemInput.class).isPresent()) {
                return Shorthand.map(ctx.getList(name, ItemInput.class), ItemInput::getItem);
            }
        }
        return ctx.getList(name, type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object toArray(final Class<?> cmpType, final List<?> list) {
        if (!cmpType.isPrimitive()) {
            return list.toArray();
        }
        if (cmpType.isAssignableFrom(int.class)) {
            return Ints.toArray((Collection) list);
        } else if (cmpType.isAssignableFrom(double.class)) {
            return Doubles.toArray((Collection) list);
        } else if (cmpType.isAssignableFrom(float.class)) {
            return Floats.toArray((Collection) list);
        } else if (cmpType.isAssignableFrom(boolean.class)) {
            return Booleans.toArray((Collection) list);
        } else if (cmpType.isAssignableFrom(byte.class)) {
            return Bytes.toArray((Collection) list);
        } else if (cmpType.isAssignableFrom(short.class)) {
            return Shorts.toArray((Collection) list);
        } else if (cmpType.isAssignableFrom(long.class)) {
            return Longs.toArray((Collection) list);
        }
        return list.toArray();
    }

    private static boolean isNullable(final Parameter param) {
        for (final Annotation a : param.getAnnotations()) {
            if (a.annotationType().getSimpleName().equalsIgnoreCase("nullable")) {
                return true;
            }
        }
        return false;
    }

    private static class ParsedNode {
        final ArgumentDescriptor<?> arg;
        final String name;
        final boolean optional;
        final boolean isList;

        ParsedNode(Node node, String name, ArgumentDescriptor<?> arg) {
            this.arg = arg;
            this.name = name;
            this.optional = node.optional();
            this.isList = node.intoList().useList();
        }

        ParsedNode(String name) {
            this.arg = ArgumentDescriptor.LITERAL;
            this.name = name;
            this.optional = false;
            this.isList = false;
        }
    }

    private static class CommandClassEvaluationException extends IllegalArgumentException {
        CommandClassEvaluationException(String msg, Object... args) {
            super(f(msg, args));
        }
    }

    private static class InvalidListNodeException extends CommandClassEvaluationException {
        InvalidListNodeException(String name) {
            super("List node {} must be the last node or followed by a literal then any other argument", name);
        }
    }
}

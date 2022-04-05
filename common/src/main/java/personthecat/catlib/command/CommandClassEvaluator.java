package personthecat.catlib.command;

import com.google.common.primitives.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.command.annotations.CommandBuilder;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.command.annotations.Node;
import personthecat.catlib.command.arguments.ArgumentDescriptor;
import personthecat.catlib.command.arguments.EnumArgument;
import personthecat.catlib.command.arguments.ListArgumentBuilder;
import personthecat.catlib.command.arguments.RegistryArgument;
import personthecat.catlib.command.LibCommandBuilder.CommandGenerator;
import personthecat.catlib.command.function.CommandFunction;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.FormattedException;
import personthecat.catlib.exception.UncheckedFormattedException;
import personthecat.catlib.util.LibStringUtils;
import personthecat.catlib.util.Shorthand;
import personthecat.catlib.util.unsafe.CachingReflectionHelper;
import personthecat.fresult.functions.ThrowingBiConsumer;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static personthecat.catlib.util.unsafe.CachingReflectionHelper.tryInstantiate;

public class CommandClassEvaluator {

    private static final String COMMAND_CATEGORY = "catlib.errorMenu.commands";
    private static final String GENERIC_ERROR = "catlib.errorText.commandClass";
    private static final String NO_PARAMETERS = "catlib.errorText.noParameters";
    private static final String EXPECTED_BUILDER = "catlib.errorText.expectedBuilder";
    private static final String COULD_NOT_INVOKE = "catlib.errorText.noInvoke";
    private static final String MISSING_COMPILE_ARG = "catlib.errorText.missingCompileArg";
    private static final String COMMAND_NODE = "catlib.errorText.commandNode";
    private static final String LIST_NODE = "catlib.errorText.listNode";
    private static final String DISPLAY_NODE = "catlib.errorText.displayNode";

    private static final Mapping<?, ?>[] AUTOMATIC_MAPPINGS = {
        new Mapping<>(BlockInput.class, BlockState.class, BlockInput::getState),
        new Mapping<>(ItemInput.class, Item.class, ItemInput::getItem)
    };

    private final List<LibCommandBuilder> builders;
    private final ModDescriptor mod;
    private Object instance;
    private Class<?> clazz;

    private CommandClassEvaluator(final ModDescriptor mod) {
        this.builders = new ArrayList<>();
        this.mod = mod;
        this.instance = null;
    }

    public static List<LibCommandBuilder> getBuilders(final ModDescriptor mod, final Class<?>... classes) {
        final CommandClassEvaluator evaluator = new CommandClassEvaluator(mod);
        for (final Class<?> c : classes) {
            evaluator.instance = null;
            evaluator.clazz = c;
            evaluator.addCommandBuilders();
            evaluator.addModCommands();
        }
        return evaluator.builders;
    }

    public static List<LibCommandBuilder> getBuilders(final ModDescriptor mod, final Object... instances) {
        final CommandClassEvaluator evaluator = new CommandClassEvaluator(mod);
        for (final Object o : instances) {
            evaluator.instance = o;
            evaluator.clazz = o.getClass();
            evaluator.addCommandBuilders();
            evaluator.addModCommands();
        }
        return evaluator.builders;
    }

    private void addCommandBuilders() {
        forEachAnnotated(CommandBuilder.class, (m, a) -> {
            if (m.getParameterCount() > 0) {
                throw new CommandClassEvaluationException(NO_PARAMETERS, m);
            }
            if (!LibCommandBuilder.class.isAssignableFrom(m.getReturnType())) {
                throw new CommandClassEvaluationException(EXPECTED_BUILDER, m);
            }
            if (this.instance == null && !Modifier.isStatic(m.getModifiers())) {
                this.instance = CachingReflectionHelper.tryInstantiate(this.clazz);
            }
            builders.add(getValue(m));
        });
    }

    private void addModCommands() {
        forEachAnnotated(ModCommand.class, (m, a) -> {
            if (this.instance == null && !Modifier.isStatic(m.getModifiers())) {
                this.instance = CachingReflectionHelper.tryInstantiate(this.clazz);
            }
            builders.add(createBuilder(m, a));
        });
    }

    private <A extends Annotation> void forEachAnnotated(Class<A> a, ThrowingBiConsumer<Method, A, FormattedException> command) {
        for (final Method m : this.clazz.getDeclaredMethods()) {
            final A annotation = m.getAnnotation(a);
            if (annotation != null) {
                try {
                    command.accept(m, annotation);
                } catch (final UncheckedFormattedException e) {
                    LibErrorContext.error(this.mod, e.getCause());
                } catch (final FormattedException e) {
                    LibErrorContext.error(this.mod, e);
                }
            }
        }
    }

    private LibCommandBuilder createBuilder(Method m, ModCommand a) {
        final List<String> tokens = LibStringUtils.tokenize(m.getName());
        final CommandFunction cmd = createConsumer(this.instance, m);
        final List<ParsedNode> entries = createEntries(tokens, a);
        return LibCommandBuilder.named(getCommandName(tokens, a))
            .arguments(getArgumentText(entries, a))
            .description(String.join(" ", a.description()))
            .linter(a.linter().length == 0 ? this.mod.getDefaultLinter() : tryInstantiate(a.linter()[0]))
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

    private CommandGenerator<CommandSourceStack>
            createBranch(List<ParsedNode> entries, CommandFunction fn) {

        return (builder, utl) -> {
            ArgumentBuilder<CommandSourceStack, ?> nextArg = null;
            final MutableInt index = new MutableInt(entries.size() - 1);
            final Command<CommandSourceStack> cmd = utl.wrap(fn);
            boolean optional = true;

            while (index.getValue() >= 0) {
                final ArgumentBuilder<CommandSourceStack, ?> argument;
                try {
                    argument = createArgument(entries, nextArg, cmd, index);
                } catch (final FormattedException e) {
                    throw new UncheckedFormattedException(e);
                }

                if (optional) {
                    argument.executes(cmd);
                    optional = false;
                }
                final ParsedNode entry = entries.get(index.getValue());
                if (entry.optional || (entry.isList && index.getValue() == entries.size() - 1)) {
                    optional = true;
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

    private List<ParsedNode> createEntries(List<String> tokens, ModCommand a) {
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

    private void addEntriesFromMethod(List<ParsedNode> entries, List<String> tokens, ModCommand a) {
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
    private ArgumentDescriptor<?> createDescriptor(Node node) {
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

    private ArgumentType<?> createStringArgumentType(Node node) {
        final Node.StringValue value = node.stringValue()[0];
        if (value.value() == Node.StringValue.Type.GREEDY) {
            return StringArgumentType.greedyString();
        } else if (value.value() == Node.StringValue.Type.STRING) {
            return StringArgumentType.string();
        }
        return StringArgumentType.word();
    }

    @SuppressWarnings("unchecked")
    private ArgumentBuilder<CommandSourceStack, ?> createArgument(
            List<ParsedNode> entries, ArgumentBuilder<CommandSourceStack, ?> next,
            Command<CommandSourceStack> cmd, MutableInt index) throws FormattedException {

        final ParsedNode entry = entries.get(index.getValue());
        final ArgumentBuilder<CommandSourceStack, ?> argument;
        final ArgumentDescriptor<?> descriptor = entry.arg;
        final ArgumentType<?> type = descriptor.getType();

        if (entry.isList) {
            argument = createList(entries, next, cmd, index.getValue());
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

    private String getArgumentName(Node node, ArgumentType<?> type) {
        if (!node.name().isEmpty()) return node.name();
        if (!node.value().isEmpty()) return node.value();
        return type.getClass().getSimpleName();
    }

    private ArgumentBuilder<CommandSourceStack, ?> createList(
            List<ParsedNode> entries, ArgumentBuilder<CommandSourceStack, ?> next,
            Command<CommandSourceStack> cmd, int index) throws FormattedException {

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
    private <T> T getValue(Method m) throws CommandClassEvaluationException {
        m.setAccessible(true);
        try {
            return (T) m.invoke(this.instance);
        } catch (final IllegalAccessException | InvocationTargetException ignored) {
            throw new CommandClassEvaluationException(COULD_NOT_INVOKE, m);
        }
    }

    private CommandFunction createConsumer(Object instance, Method m) {
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

    private Object[] getArgs(CommandContextWrapper ctx, Parameter[] params) throws CommandClassEvaluationException {
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

    private String getName(final Parameter param) throws CommandClassEvaluationException {
        if (!param.isNamePresent()) {
            final Method m = (Method) param.getDeclaringExecutable();
            throw new CommandClassEvaluationException(MISSING_COMPILE_ARG, m);
        }
        return param.getName();
    }

    private static Class<?> getTypeArg(final Parameter param) {
        return (Class<?>) ((ParameterizedType) param.getParameterizedType()).getActualTypeArguments()[0];
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object get(final CommandContextWrapper ctx, final String name, final Class<?> type) {
        for (final Mapping<?, ?> mapping : AUTOMATIC_MAPPINGS) {
            if (type.isAssignableFrom(mapping.to)) {
                final Optional<?> o = ctx.getOptional(name, mapping.from).map((Function) mapping.mapper);
                if (o.isPresent()) return o.get();
                break;
            }
        }
        return ctx.get(name, type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Optional<?> getOptional(final CommandContextWrapper ctx, final String name, final Class<?> type) {
        for (final Mapping<?, ?> mapping : AUTOMATIC_MAPPINGS) {
            if (type.isAssignableFrom(mapping.to)) {
                final Optional<?> o = ctx.getOptional(name, mapping.from).map((Function) mapping.mapper);
                if (o.isPresent()) return o;
                break;
            }
        }
        return ctx.getOptional(name, type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<?> getList(final CommandContextWrapper ctx, final String name, final Class<?> type) {
        for (final Mapping<?, ?> mapping : AUTOMATIC_MAPPINGS) {
            if (type.isAssignableFrom(mapping.to)) {
                if (ctx.getOptional(name + "0", mapping.from).isPresent()) {
                    return Shorthand.map((List) ctx.getList(name, mapping.from), mapping.mapper);
                }
                break;
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

    private static class Mapping<T, R> {
        final Class<T> from;
        final Class<R> to;
        final Function<T, R> mapper;

        Mapping(Class<T> from, Class<R> to, Function<T, R> mapper) {
            this.from = from;
            this.to = to;
            this.mapper = mapper;
        }
    }

    private class CommandClassEvaluationException extends FormattedException {
        final Method method;

        CommandClassEvaluationException(String msg, Method method) {
            super(msg);
            this.method = method;
        }

        @Override
        public @NotNull String getCategory() {
            return COMMAND_CATEGORY;
        }

        @Override
        public @NotNull Component getDisplayMessage() {
            return new TextComponent(this.getFullMethod());
        }

        @Override
        public @Nullable Component getTooltip() {
            return new TranslatableComponent(this.getMessage(), this.method.getName());
        }

        @Override
        public @NotNull Component getTitleMessage() {
            return new TranslatableComponent(GENERIC_ERROR, this.getFullMethod());
        }

        private String getFullMethod() {
            return clazz.getSimpleName() + "#" + this.method.getName();
        }
    }

    private static class InvalidListNodeException extends FormattedException {
        private final String name;

        InvalidListNodeException(String name) {
            super(COMMAND_NODE);
            this.name = name;
        }

        @Override
        public @NotNull String getCategory() {
            return COMMAND_CATEGORY;
        }

        @Override
        public @NotNull Component getDisplayMessage() {
            return new TranslatableComponent(DISPLAY_NODE, this.name);
        }

        @Override
        public @Nullable Component getTooltip() {
            return new TranslatableComponent(LIST_NODE, this.name);
        }

        @Override
        public @NotNull Component getTitleMessage() {
            return new TranslatableComponent(COMMAND_NODE, this.name);
        }
    }
}

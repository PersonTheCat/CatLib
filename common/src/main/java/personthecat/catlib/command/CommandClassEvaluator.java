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
import personthecat.catlib.command.arguments.FileArgument;
import personthecat.catlib.command.arguments.ListArgumentBuilder;
import personthecat.catlib.command.arguments.RegistryArgument;
import personthecat.catlib.command.LibCommandBuilder.CommandGenerator;
import personthecat.catlib.command.function.CommandFunction;
import personthecat.catlib.config.ConfigUtil;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.FormattedException;
import personthecat.catlib.exception.UncheckedFormattedException;
import personthecat.catlib.util.LibStringUtils;
import personthecat.catlib.util.unsafe.CachingReflectionHelper;
import personthecat.fresult.functions.ThrowingBiConsumer;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static personthecat.catlib.util.unsafe.CachingReflectionHelper.tryInstantiate;

public class CommandClassEvaluator {

    private static final String COMMAND_CATEGORY = "catlib.errorMenu.commands";
    private static final String GENERIC_ERROR = "catlib.errorText.commandClass";
    private static final String NO_PARAMETERS = "catlib.errorText.noParameters";
    private static final String EXPECTED_BUILDER = "catlib.errorText.expectedBuilder";
    private static final String COULD_NOT_INVOKE = "catlib.errorText.noInvoke";
    private static final String MISSING_COMPILE_ARG = "catlib.errorText.missingCompileArg";
    private static final String COMMAND_NODE = "catlib.errorText.commandNode";
    private static final String MISC_NODE = "catlib.errorText.node";
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
        this.forEachAnnotated(CommandBuilder.class, (m, a) -> {
            if (m.getParameterCount() > 0) {
                throw new CommandClassEvaluationException(NO_PARAMETERS, m);
            }
            if (!LibCommandBuilder.class.isAssignableFrom(m.getReturnType())) {
                throw new CommandClassEvaluationException(EXPECTED_BUILDER, m);
            }
            if (this.instance == null && !Modifier.isStatic(m.getModifiers())) {
                this.instance = CachingReflectionHelper.tryInstantiate(this.clazz);
            }
            this.builders.add(getValue(m));
        });
    }

    private void addModCommands() {
        this.forEachAnnotated(ModCommand.class, (m, a) -> {
            if (this.instance == null && !Modifier.isStatic(m.getModifiers())) {
                this.instance = CachingReflectionHelper.tryInstantiate(this.clazz);
            }
            this.builders.add(createBuilder(m, a));
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

    private LibCommandBuilder createBuilder(Method m, ModCommand a) throws FormattedException {
        final List<String> tokens = LibStringUtils.tokenize(m.getName());
        final CommandFunction cmd = this.createConsumer(this.instance, m);
        final List<ParsedNode> entries = this.createEntries(m, tokens, a);
        return LibCommandBuilder.named(getCommandName(tokens, a))
            .arguments(getArgumentText(entries, a))
            .description(String.join(" ", a.description()))
            .linter(a.linter().length == 0 ? this.mod.defaultLinter() : tryInstantiate(a.linter()[0]))
            .type(a.type())
            .side(a.side())
            .generate(createBranch(entries, cmd));
    }

    private static String getCommandName(List<String> tokens, ModCommand a) {
        if (!a.name().isEmpty()) return a.name();
        if (!a.value().isEmpty()) return a.value();
        return tokens.getFirst();
    }

    private static String getArgumentText(List<ParsedNode> entries, ModCommand a) {
        if (!a.arguments().isEmpty()) return a.arguments();
        final StringBuilder sb = new StringBuilder();
        for (final ParsedNode entry : entries) {
            if (!sb.isEmpty()) sb.append(' ');
            if (entry.arg.isLiteral()) {
                sb.append(entry.name);
                continue;
            }
            if (entry.isOptional) sb.append('[');
            sb.append('<');
            sb.append(entry.name);
            if (entry.isList) sb.append("...");
            sb.append('>');
            if (entry.isOptional) sb.append(']');
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
                if (entry.isOptional || (entry.isList && index.getValue() == entries.size() - 1)) {
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

    private List<ParsedNode> createEntries(
            Method m, List<String> tokens, ModCommand a) throws FormattedException {
        final var entries = new LinkedHashMap<String, ParsedNode>();
        if (a.name().isEmpty() && a.value().isEmpty()) {
            this.addLiteralsFromTokens(entries, tokens);
        }
        final var explicitArgs = this.getExplicitArgParams(m);
        if (explicitArgs.size() > a.branch().length) {
            this.addArgsFromParams(entries, explicitArgs);
        }
        this.addArgsFromBranch(entries, a.branch());
        return new ArrayList<>(entries.values());
    }

    private void addLiteralsFromTokens(Map<String, ParsedNode> entries, List<String> tokens) {
        for (int i = 1; i < tokens.size(); i++) {
            final var token = tokens.get(i).toLowerCase();
            entries.put(token, new ParsedNode(token));
        }
    }

    private void addArgsFromParams(
            Map<String, ParsedNode> entries, List<Parameter> params) throws FormattedException {
        for (final var param : params) {
            final var name = this.getName(param);
            var type = param.getType();
            var isOptional = false;
            var isList = false;
            if (Optional.class.isAssignableFrom(type)) {
                isOptional = true;
                type = getTypeArg(param);
            } else if (List.class.isAssignableFrom(type)) {
                isList = true;
                type = getTypeArg(param);
            } else if (type.isArray() || param.isVarArgs()) {
                isList = true;
                type = type.getComponentType();
            } else if (isNullable(param)) {
                isOptional = true;
            }
            final var arg = this.createArgFromType(param, type);
            entries.put(name, new ParsedNode(name, arg, isOptional, isList));
        }
    }

    private List<Parameter> getExplicitArgParams(Method m) {
        return Stream.of(m.getParameters()).filter(this::isExplicitArgParam).toList();
    }

    private boolean isExplicitArgParam(Parameter param) {
        return !param.isImplicit() && !CommandContextWrapper.class.isAssignableFrom(param.getType());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentDescriptor<?> createArgFromType(Parameter param, Class<?> type) throws FormattedException {
        final var node = param.getAnnotation(Node.class);
        if (node != null && (node.type().length > 0 || node.descriptor().length > 0)) {
            return this.createArgFromNode(node);
        } else if (ConfigUtil.isInteger(type) && (node == null || node.intRange().length == 0)) {
            return new ArgumentDescriptor<>(IntegerArgumentType.integer());
        } else if (ConfigUtil.isLong(type) && (node == null || node.intRange().length == 0)) {
            return new ArgumentDescriptor<>(LongArgumentType.longArg());
        } else if (ConfigUtil.isDouble(type) && (node == null || node.doubleRange().length == 0)) {
            return new ArgumentDescriptor<>(DoubleArgumentType.doubleArg());
        } else if (ConfigUtil.isFloat(type) && (node == null || node.doubleRange().length == 0)) {
            return new ArgumentDescriptor<>(FloatArgumentType.floatArg());
        } else if (ConfigUtil.isBoolean(type)) {
            return new ArgumentDescriptor<>(BoolArgumentType.bool());
        } else if (String.class.isAssignableFrom(type) && (node == null || node.stringValue().length == 0)) {
            return new ArgumentDescriptor<>(StringArgumentType.string());
        } else if (File.class.isAssignableFrom(type)) {
            return new ArgumentDescriptor<>(new FileArgument(this.mod.configFolder(), false));
        } else if (type.isEnum()) {
            return new ArgumentDescriptor<>(EnumArgument.of((Class) type));
        }
        final RegistryArgument<?> ra = RegistryArgument.lookup(type);
        if (ra != null) {
            return new ArgumentDescriptor<>(ra);
        } else if (node != null) {
            return this.createArgFromNode(node);
        }
        throw new InvalidNodeException(this.getName(param));
    }

    private void addArgsFromBranch(Map<String, ParsedNode> entries, Node[] branch) {
        for (final Node node : branch) {
            final ArgumentDescriptor<?> arg = this.createArgFromNode(node);
            final String name = this.getArgumentName(node, arg.type());
            entries.put(name, new ParsedNode(name, node, arg));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentDescriptor<?> createArgFromNode(Node node) {
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
            return new ArgumentDescriptor<>(createStringArgumentType(node.stringValue()[0]));
        } else if (node.enumValue().length > 0) {
            return new ArgumentDescriptor<>(EnumArgument.of((Class) node.enumValue()[0]));
        } else if (node.registry().length > 0) {
            return new ArgumentDescriptor<>(RegistryArgument.getOrThrow(node.registry()[0]));
        } else {
            return ArgumentDescriptor.LITERAL;
        }
    }

    private ArgumentType<?> createStringArgumentType(Node.StringValue value) {
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
        final ArgumentType<?> type = descriptor.type();

        if (entry.isList) {
            argument = createList(entries, next, cmd, index.getValue());
        } else if (descriptor.isLiteral()) {
            argument = Commands.literal(entry.name);
        } else {
            argument = Commands.argument(entry.name, type);
        }
        if (descriptor.suggestions() != null) {
            ((RequiredArgumentBuilder<CommandSourceStack, ?>) argument)
                .suggests(descriptor.suggestions());
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
            ListArgumentBuilder.create(entry.name, entry.arg.type());

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
            final String name = this.getName(param);
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
                    return ((List) ctx.getList(name, mapping.from)).stream().map(mapping.mapper).toList();
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

    private record ParsedNode(String name, ArgumentDescriptor<?> arg, boolean isOptional, boolean isList) {
        ParsedNode(String name, Node node, ArgumentDescriptor<?> arg) {
            this(name, arg, node.optional(), node.intoList().useList());
        }

        ParsedNode(String name) {
            this(name, ArgumentDescriptor.LITERAL, false, false);
        }
    }

    private record Mapping<T, R>(Class<T> from, Class<R> to, Function<T, R> mapper) {}

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
            return Component.literal(this.getFullMethod());
        }

        @Override
        public @Nullable Component getTooltip() {
            return Component.translatable(this.getMessage(), this.method.getName());
        }

        @Override
        public @NotNull Component getTitleMessage() {
            return Component.translatable(GENERIC_ERROR, this.getFullMethod());
        }

        private String getFullMethod() {
            return clazz.getSimpleName() + "#" + this.method.getName();
        }
    }

    private static class InvalidNodeException extends FormattedException {
        protected final String name;

        InvalidNodeException(String name) {
            super(COMMAND_NODE);
            this.name = name;
        }

        @Override
        public @NotNull String getCategory() {
            return COMMAND_CATEGORY;
        }

        @Override
        public @NotNull Component getDisplayMessage() {
            return Component.translatable(DISPLAY_NODE, this.name);
        }

        @Override
        public @Nullable Component getTooltip() {
            return Component.translatable(MISC_NODE, this.name);
        }

        @Override
        public @NotNull Component getTitleMessage() {
            return Component.translatable(COMMAND_NODE, this.name);
        }
    }

    private static class InvalidListNodeException extends InvalidNodeException {
        InvalidListNodeException(String name) {
            super(name);
        }

        @Override
        public @Nullable Component getTooltip() {
            return Component.translatable(LIST_NODE, this.name);
        }
    }
}

package personthecat.catlib.command;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import lombok.experimental.UtilityClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import personthecat.catlib.command.annotations.CommandBuilder;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.command.LibCommandBuilder.CommanBuilder;
import personthecat.catlib.command.annotations.Node;
import personthecat.catlib.command.arguments.ArgumentDescriptor;
import personthecat.catlib.command.arguments.EnumArgument;
import personthecat.catlib.command.arguments.ListArgumentBuilder;
import personthecat.catlib.command.arguments.RegistryArgument;
import personthecat.catlib.command.function.CommandFunction;
import personthecat.catlib.data.IntRef;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.LibStringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static personthecat.catlib.util.Shorthand.f;
import static personthecat.catlib.util.unsafe.CachingReflectionHelper.tryInstantiate;

@UtilityClass
public class CommandClassEvaluator {

    public static List<LibCommandBuilder> getBuilders(final ModDescriptor mod, final Class<?>... classes) {
        final List<LibCommandBuilder> builders = new ArrayList<>();
        for (final Class<?> c : classes) {
            addCommandBuilders(builders, c);
            addModCommands(mod, builders, c);
        }
        return builders;
    }

    private static void addCommandBuilders(final List<LibCommandBuilder> builders, final Class<?> c) {
        forEachAnnotated(c, CommandBuilder.class, (m, a) -> {
            if (m.getParameterCount() > 0) {
                throw new CommandClassEvaluationException("{} must have no parameters", m.getName());
            }
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new CommandClassEvaluationException("{} must be static", m.getName());
            }
            if (!LibCommandBuilder.class.isAssignableFrom(m.getReturnType())) {
                throw new CommandClassEvaluationException("{} must return a LibCommandBuilder", m.getName());
            }
            builders.add(getValue(m));
        });
    }

    private static void addModCommands(final ModDescriptor mod, final List<LibCommandBuilder> builders, final Class<?> c) {
        forEachAnnotated(c, ModCommand.class, (m, a) -> {
            if (m.getParameterCount() != 1) {
                throw new CommandClassEvaluationException("{} must have exactly 1 parameter", m.getName());
            }
            if (!CommandContextWrapper.class.equals(m.getParameterTypes()[0])) {
                throw new CommandClassEvaluationException("{} must accept a CommandContextWrapper", m.getName());
            }
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new CommandClassEvaluationException("{} must be static", m.getName());
            }
            builders.add(createBuilder(mod, createConsumer(m), m, a));
        });
    }

    private static <A extends Annotation> void forEachAnnotated(final Class<?> c, final Class<A> a, final BiConsumer<Method, A> command) {
        for (final Method m : c.getDeclaredMethods()) {
            final A annotation = m.getAnnotation(a);
            if (annotation != null) {
                command.accept(m, annotation);
            }
        }
    }

    private static LibCommandBuilder createBuilder(final ModDescriptor mod, final CommandFunction cmd, final Method m, final ModCommand a) {
        final List<String> tokens = LibStringUtils.tokenize(m.getName());
        final List<ParsedNode> entries = createEntries(tokens, a);
        return LibCommandBuilder.named(getCommandName(tokens, a))
            .arguments(getArgumentText(entries, a))
            .description(String.join(" ", a.description()))
            .linter(a.linter().length == 0 ? mod.getDefaultLinter() : tryInstantiate(a.linter()[0]))
            .wrap("", cmd)
            .type(a.type())
            .side(a.side())
            .generate(createBranch(entries));
    }

    private static String getCommandName(final List<String> tokens, final ModCommand a) {
        if (!a.name().isEmpty()) return a.name();
        if (!a.value().isEmpty()) return a.value();
        return tokens.get(0);
    }

    private static String getArgumentText(final List<ParsedNode> entries, final ModCommand a) {
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

    private static CommanBuilder<CommandSourceStack> createBranch(final List<ParsedNode> entries) {

        return (builder, wrappers) -> {
            final List<ArgumentBuilder<CommandSourceStack, ?>> arguments = new ArrayList<>();
            ArgumentBuilder<CommandSourceStack, ?> lastArg = builder;
            final IntRef index = new IntRef(0);

            while (index.get() < entries.size()) {
                final ParsedNode entry = entries.get(index.get());
                if (entry.optional) {
                    lastArg.executes(wrappers.get(""));
                }

                final ArgumentBuilder<CommandSourceStack, ?> argument = createArgument(entries, index);
                arguments.add(argument);
                lastArg = argument;
                index.increment();
            }
            lastArg.executes(wrappers.get(""));

            if (!arguments.isEmpty()) {
                ArgumentBuilder<CommandSourceStack, ?> nextArg = arguments.get(arguments.size() - 1);
                for (int i = arguments.size() - 2; i >= 0; i--) {
                    nextArg = arguments.get(i).then(nextArg);
                }
                builder.then(nextArg);
            }
            return builder;
        };
    }

    private static List<ParsedNode> createEntries(final List<String> tokens, final ModCommand a) {
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

    private static void addEntriesFromMethod(final List<ParsedNode> entries, final List<String> tokens, final ModCommand a) {
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
    private static ArgumentDescriptor<?> createDescriptor(final Node node) {
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

    private static ArgumentType<?> createStringArgumentType(final Node node) {
        final Node.StringValue value = node.stringValue()[0];
        if (value.value() == Node.StringValue.Type.GREEDY) {
            return StringArgumentType.greedyString();
        } else if (value.value() == Node.StringValue.Type.STRING) {
            return StringArgumentType.string();
        }
        return StringArgumentType.word();
    }

    @SuppressWarnings("unchecked")
    private static ArgumentBuilder<CommandSourceStack, ?> createArgument(final List<ParsedNode> entries, final IntRef index) {
        final ParsedNode entry = entries.get(index.get());
        final ArgumentBuilder<CommandSourceStack, ?> argument;
        final ArgumentDescriptor<?> descriptor = entry.arg;
        final ArgumentType<?> type = descriptor.getType();

        if (entry.isList) {
            argument = createList(entries, index.get());
            index.add(2);
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

    private static String getArgumentName(final Node node, final ArgumentType<?> type) {
        if (!node.name().isEmpty()) return node.name();
        if (!node.value().isEmpty()) return node.value();
        return type.getClass().getSimpleName();
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createList(final List<ParsedNode> entries, final int index) {
        final ParsedNode entry = entries.get(index);
        final ListArgumentBuilder listBuilder =
            ListArgumentBuilder.create(entry.name, entry.arg.getType());

        if (entries.size() > index + 1) {
            final ParsedNode nextEntry = entries.get(index + 1);
            if (!nextEntry.arg.isLiteral() || entries.size() <= index + 2) {
                throw new InvalidListNodeException(entry.name);
            }
            final ParsedNode followingEntry = entries.get(index + 2);
            final ArgumentBuilder<CommandSourceStack, ?> termination =
                Commands.argument(followingEntry.name, followingEntry.arg.getType());

            return listBuilder.terminatedBy(nextEntry.name, termination).build();
        } else {
            return listBuilder.build();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getValue(final Method m) {
        m.setAccessible(true);
        try {
            return (T) m.invoke(null);
        } catch (final IllegalAccessException | InvocationTargetException ignored) {
            throw new CommandClassEvaluationException("Could not invoke: {}", m.getName());
        }
    }

    private static CommandFunction createConsumer(final Method m) {
        m.setAccessible(true);
        return wrapper -> m.invoke(null, wrapper);
    }

    private static class ParsedNode {
        final ArgumentDescriptor<?> arg;
        final String name;
        final boolean optional;
        final boolean isList;

        ParsedNode(final Node node, final String name, final ArgumentDescriptor<?> arg) {
            this.arg = arg;
            this.name = name;
            this.optional = node.optional();
            this.isList = node.intoList().useList();
        }

        ParsedNode(final String name) {
            this.arg = ArgumentDescriptor.LITERAL;
            this.name = name;
            this.optional = false;
            this.isList = false;
        }
    }

    private static class CommandClassEvaluationException extends IllegalArgumentException {
        CommandClassEvaluationException(final String msg, final Object... args) {
            super(f(msg, args));
        }
    }

    private static class InvalidListNodeException extends CommandClassEvaluationException {
        InvalidListNodeException(final String name) {
            super("List node {} must be the last node or followed by a literal then any other argument", name);
        }
    }
}

package personthecat.catlib.command;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import lombok.experimental.UtilityClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.apache.commons.lang3.tuple.Pair;
import personthecat.catlib.command.annotations.CommandBuilder;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.command.LibCommandBuilder.CommandFunction;
import personthecat.catlib.command.annotations.Node;
import personthecat.catlib.command.arguments.ArgumentDescriptor;
import personthecat.catlib.command.arguments.ListArgumentBuilder;
import personthecat.catlib.data.IntRef;
import personthecat.catlib.util.SyntaxLinter;
import personthecat.fresult.functions.ThrowingConsumer;

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

    public static List<LibCommandBuilder> getBuilders(final Class<?>... classes) {
        final List<LibCommandBuilder> builders = new ArrayList<>();
        for (final Class<?> c : classes) {
            addCommandBuilders(builders, c);
            addModCommands(builders, c);
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

    private static void addModCommands(final List<LibCommandBuilder> builders, final Class<?> c) {
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
            builders.add(createBuilder(createConsumer(m), a));
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

    private static LibCommandBuilder createBuilder(final ThrowingConsumer<CommandContextWrapper, Throwable> cmd, final ModCommand a) {
        return LibCommandBuilder.named(a.name())
            .arguments(a.arguments())
            .description(String.join(" ", a.description()))
            .linter(a.linter().length == 0 ? SyntaxLinter.DEFAULT_LINTER : tryInstantiate(a.linter()[0]))
            .wrap("", cmd)
            .type(a.type())
            .side(a.side())
            .generate(createBranch(a));
    }

    private static CommandFunction<CommandSourceStack> createBranch(final ModCommand a) {
        return (builder, wrappers) -> {
            final List<Pair<Node, ArgumentDescriptor<?>>> entries = createEntries(a.branch());
            final List<ArgumentBuilder<CommandSourceStack, ?>> arguments = new ArrayList<>();
            ArgumentBuilder<CommandSourceStack, ?> lastArg = builder;
            final IntRef index = new IntRef(0);

            while (index.get() < entries.size()) {
                final Pair<Node, ArgumentDescriptor<?>> entry = entries.get(index.get());
                if (entry.getKey().optional()) {
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

    private static List<Pair<Node, ArgumentDescriptor<?>>> createEntries(final Node[] nodes) {
        final List<Pair<Node, ArgumentDescriptor<?>>> entries = new ArrayList<>();
        for (final Node node : nodes) {
            entries.add(Pair.of(node, createDescriptor(node)));
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
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
        } else if (node.stringVal().length > 0) {
            return new ArgumentDescriptor<>(createStringArgumentType(node));
        } else {
            return ArgumentDescriptor.LITERAL;
        }
    }

    private static ArgumentType<?> createStringArgumentType(final Node node) {
        final Node.StringValue value = node.stringVal()[0];
        if (value.value() == Node.StringValue.Type.GREEDY) {
            return StringArgumentType.greedyString();
        } else if (value.value() == Node.StringValue.Type.STRING) {
            return StringArgumentType.string();
        }
        return StringArgumentType.word();
    }

    @SuppressWarnings("unchecked")
    private static ArgumentBuilder<CommandSourceStack, ?> createArgument(
        final List<Pair<Node, ArgumentDescriptor<?>>> entries, final IntRef index) {

        final Pair<Node, ArgumentDescriptor<?>> entry = entries.get(index.get());
        final ArgumentBuilder<CommandSourceStack, ?> argument;
        if (entry.getKey().intoList().useList()) {
            argument = createList(entries, index.get());
            index.add(2);
        } else {
            argument = Commands.argument(entry.getKey().name(), entry.getValue().getType());
        }
        if (entry.getValue().getSuggestions() != null) {
            ((RequiredArgumentBuilder<CommandSourceStack, ?>) argument)
                .suggests(entry.getValue().getSuggestions());
        }
        return argument;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createList(
            final List<Pair<Node, ArgumentDescriptor<?>>> entries, final int index) {

        final Pair<Node, ArgumentDescriptor<?>> entry = entries.get(index);
        final ListArgumentBuilder listBuilder =
            ListArgumentBuilder.create(entry.getKey().name(), entry.getValue().getType());

        if (entries.size() > index + 1) {
            final Pair<Node, ArgumentDescriptor<?>> nextEntry = entries.get(index + 1);
            if (!nextEntry.getValue().isLiteral() || entries.size() <= index + 2) {
                throw new InvalidListNodeException(entry.getKey().name());
            }
            final Pair<Node, ArgumentDescriptor<?>> followingEntry = entries.get(index + 2);
            final ArgumentBuilder<CommandSourceStack, ?> termination =
                Commands.argument(followingEntry.getKey().name(), followingEntry.getValue().getType());

            return listBuilder.terminatedBy(nextEntry.getKey().name(), termination).build();
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

    private static ThrowingConsumer<CommandContextWrapper, Throwable> createConsumer(final Method m) {
        m.setAccessible(true);
        return wrapper -> m.invoke(null, wrapper);
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

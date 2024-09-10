package personthecat.catlib.command.arguments;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jetbrains.annotations.Nullable;

public class ListArgumentBuilder {

    /** The maximum number of values in a list argument. */
    public static final int MAX_LIST_DEPTH = 32;

    @Nullable private ArgumentBuilder<CommandSourceStack, ?> exitNode;
    @Nullable private Command<CommandSourceStack> cmd;

    private final String name;
    private final ArgumentType<?> type;
    private int size = MAX_LIST_DEPTH;

    ListArgumentBuilder(final String name, final ArgumentType<?> type) {
        this.name = name;
        this.type = type;
    }

    public static ListArgumentBuilder create(final String name, final ArgumentType<?> type) {
        return new ListArgumentBuilder(name, type);
    }

    public ListArgumentBuilder terminatedBy(final String key) {
        this.exitNode = Commands.literal(key);
        return this;
    }

    public ListArgumentBuilder terminatedBy(final String key, final ArgumentBuilder<CommandSourceStack, ?> arg) {
        this.exitNode = Commands.literal(key).then(arg);
        return this;
    }

    public ListArgumentBuilder terminatedBy(final ArgumentBuilder<CommandSourceStack, ?> arg) {
        this.exitNode = arg;
        return this;
    }

    public ListArgumentBuilder executes(final Command<CommandSourceStack> cmd) {
        this.cmd = cmd;
        return this;
    }

    public ListArgumentBuilder withSize(final int size) {
        this.size = Math.max(1, Math.min(size, MAX_LIST_DEPTH));
        return this;
    }

    public ArgumentBuilder<CommandSourceStack, ?> build() {
        ArgumentBuilder<CommandSourceStack, ?> nextArg = this.createNode(this.size);
        for (int i = this.size - 1; i >= 0; i--) {
            nextArg = this.createNode(i).then(nextArg);
        }
        return nextArg;
    }

    private ArgumentBuilder<CommandSourceStack, ?> createNode(final int index) {
        final ArgumentBuilder<CommandSourceStack, ?> arg = Commands.argument(this.name + index, this.type);
        if (this.cmd != null) {
            arg.executes(cmd);
        }
        if (this.exitNode == null) {
            return arg;
        }
        return arg.then(this.exitNode);
    }
}

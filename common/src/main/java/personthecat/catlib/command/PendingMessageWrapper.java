package personthecat.catlib.command;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import static personthecat.catlib.util.LibUtil.f;

@SuppressWarnings("UnusedReturnValue")
public class PendingMessageWrapper {
    final CommandContextWrapper ctx;
    final MutableComponent msg;

    protected PendingMessageWrapper(final CommandContextWrapper ctx, final MutableComponent msg) {
        this.ctx = ctx;
        this.msg = msg;
    }

    public PendingMessageWrapper append(final String msg) {
        this.msg.append(Component.literal(msg));
        return this;
    }

    public PendingMessageWrapper append(final String msg, final Object... args) {
        return this.append(f(msg, args));
    }

    public PendingMessageWrapper append(final String msg, final Style style) {
        this.msg.append(Component.literal(msg).setStyle(style));
        return this;
    }

    public PendingMessageWrapper append(final Component component) {
        this.msg.append(component);
        return this;
    }

    public CommandContextWrapper sendMessage() {
        this.ctx.sendMessage(this.msg);
        return this.ctx;
    }

    public CommandContextWrapper sendError() {
        this.ctx.sendError(this.msg);
        return this.ctx;
    }
}

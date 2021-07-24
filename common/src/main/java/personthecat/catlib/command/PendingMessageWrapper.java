package personthecat.catlib.command;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;

import static personthecat.catlib.util.Shorthand.f;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class PendingMessageWrapper {
    final CommandContextWrapper ctx;
    final TextComponent msg;

    protected PendingMessageWrapper(final CommandContextWrapper ctx, final TextComponent msg) {
        this.ctx = ctx;
        this.msg = msg;
    }

    public PendingMessageWrapper append(final String msg) {
        this.msg.append(new TextComponent(msg));
        return this;
    }

    public PendingMessageWrapper append(final String msg, final Object... args) {
        return this.append(f(msg, args));
    }

    public PendingMessageWrapper append(final String msg, final Style style) {
        this.msg.append(new TextComponent(msg).setStyle(style));
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

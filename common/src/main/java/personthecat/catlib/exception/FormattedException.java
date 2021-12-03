package personthecat.catlib.exception;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.client.gui.ErrorDetailPage;
import personthecat.catlib.util.StackTraceLinter;

import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class FormattedException extends Exception {

    public FormattedException(final String msg) {
        super(msg);
    }

    public FormattedException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    public FormattedException(final Throwable cause) {
        super(cause);
    }

    @NotNull
    public String getCategory() {
        return "catlib.errorMenu.misc";
    }

    @NotNull
    public abstract Component getDisplayMessage();

    @NotNull
    public Component getTitleMessage() {
        return this.getDisplayMessage();
    }

    @Nullable
    public Component getTooltip() {
        return null;
    }

    @NotNull
    public Component getDetailMessage() {
        return StackTraceLinter.format(this.readStacktrace());
    }

    @NotNull
    public Screen getDetailsScreen(final Screen parent) {
        return new ErrorDetailPage(parent, this.getTitleMessage(), this.getDetailMessage());
    }

    public void onErrorReceived(final Logger log) {}

    protected String readStacktrace() {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        final Throwable cause = this.getCause();
        (cause != null ? cause : this).printStackTrace(pw);
        return sw.toString().replace("\t", "    ").replace("\r", "");
    }
}
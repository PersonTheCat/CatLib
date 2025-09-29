package personthecat.catlib.serialization.codec.context;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.datafixers.util.Either;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import personthecat.catlib.linting.Linters;
import personthecat.catlib.util.McUtils;
import xjs.data.JsonValue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static personthecat.catlib.command.CommandUtils.displayOnHover;

@Slf4j
public final class DecodeContextRenderer {
    private static final String ROOT_ERRORS = "catlib.errorText.rootErrors";
    private static final String LEAF_ERRORS = "catlib.errorText.leafErrors";
    private static final String SPLIT_ERRORS = "catlib.errorText.splitErrors";
    private static final String LEAVES_NOT_AVAILABLE = "catlib.errorText.leavesNotAvailable";
    private static final String ERROR_NUMBER_X = "catlib.errorText.errorNumber";
    private static final String ORIGINAL_DATA = "catlib.errorText.originalData";

    private DecodeContextRenderer() {}

    public static Component render(DecodeContext ctx) {
        final var root = ctx.getErrorRoot();
        final var comp = Component.empty();

        if (!root.messageSuppliers.isEmpty()) {
            comp.append(Component.translatable(ROOT_ERRORS).append(":").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            comp.append("\n\n");
            final var counter = new AtomicInteger();
            for (final var msg : root.messages.get()) {
                comp.append(Component.literal("  " + (counter.incrementAndGet()) + ". ").withStyle(ChatFormatting.DARK_GRAY));
                comp.append(Component.literal(msg).withStyle(ChatFormatting.RED));
                comp.append("\n");
            }
            comp.append("\n");
        }
        if (!ctx.getErrors().isEmpty()) {
            for (final var errors : ctx.getErrors()) {
                comp.append(renderErrors(errors));
            }
        } else if (McUtils.getPlatform().isForgeLike()) {
            comp.append(renderSplitRoot(root));
        }
        final var data = ctx.getOriginalInput();
        if (data != null) {
            comp.append("\n");
            comp.append(Component.translatable(ORIGINAL_DATA).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            comp.append("\n\n");
            comp.append(renderData(data));
        }
        return comp;
    }

    private static Component renderErrors(CategorizedErrors errors) {
        final var comp = Component.empty();
        if (errors.erredLeaves().isEmpty()) {
            return comp;
        }
        if (errors.category() != null) {
            comp.append("\n");
            comp.append(Component.translatable(errors.category()).append(":").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            comp.append("\n");
        }
        comp.append("\n");
        comp.append(Component.translatable(LEAF_ERRORS).append(":").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        comp.append("\n\n");

        errors.erredLeaves().forEach((path, messages) -> {
            comp.append(Component.literal("  - ").withStyle(ChatFormatting.DARK_GRAY));
            comp.append(renderPath(path));
            comp.append("\n");
            for (final var msg : messages) {
                comp.append(Component.literal("    - ").withStyle(ChatFormatting.DARK_GRAY));
                comp.append(Component.literal(msg).withStyle(ChatFormatting.RED));
                comp.append("\n");
            }
        });
        return comp;
    }

    private static Component renderPath(List<Either<String, Integer>> path) {
        if (path.isEmpty()) {
            return Component.literal("<root>").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
        }
        final var comp = Component.empty();
        for (int i = 0; i < path.size(); i++) {
            final int fi = i;
            path.get(i)
                .ifLeft(k -> { if (fi != 0) comp.append("."); comp.append(Component.literal(k).withStyle(ChatFormatting.AQUA)); })
                .ifRight(ix -> comp.append("[").append(Component.literal(ix.toString()).withStyle(ChatFormatting.AQUA)).append("]"));
        }
        return comp;
    }

    private static Component renderSplitRoot(ErrorNode root) {
        final var comp = Component.empty();
        final var notAvailableStyle =
            Style.EMPTY.withColor(ChatFormatting.RED).withBold(true).withHoverEvent(displayOnHover(":("));
        comp.append("\n");
        comp.append(Component.translatable(LEAF_ERRORS).append(":").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        comp.append("\n\n");
        comp.append(Component.literal("  - ").withStyle(ChatFormatting.DARK_GRAY));
        comp.append(Component.translatable(LEAVES_NOT_AVAILABLE, McUtils.getPlatform().formatted()).withStyle(notAvailableStyle));
        comp.append("\n");
        if (!root.messageSuppliers.isEmpty()) {
            comp.append("\n");
            comp.append(Component.translatable(SPLIT_ERRORS).append(":").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            comp.append("\n");
            final var counter = new AtomicInteger();
            for (final var msg : root.messages.get()) {
                comp.append("\n");
                comp.append(Component.literal("  - ").withStyle(ChatFormatting.DARK_GRAY));
                comp.append(Component.translatable(ERROR_NUMBER_X, counter.incrementAndGet()).withStyle(ChatFormatting.AQUA));
                comp.append(":\n");
                for (final var split : msg.split("; ")) {
                    comp.append(Component.literal("    - ").withStyle(ChatFormatting.DARK_GRAY));
                    comp.append(Component.literal(split).withStyle(ChatFormatting.RED));
                    comp.append("\n");
                }
            }
        }
        return comp;
    }

    private static Component renderData(Object data) {
        try {
            if (data instanceof JsonElement e) { // gson
                final var sw = new StringWriter();
                final var writer = new JsonWriter(sw);
                writer.setLenient(true);
                writer.setIndent("  ");
                try {
                    Streams.write(e, writer);
                } catch (IOException ignored) { /* unreachable */ }
                return Linters.JSON.lint(sw.toString());
            } else if (data instanceof JsonValue v) { // xjs
                return Linters.DJS.lint(v.toString("djs"));
            }
            final var s = new GsonBuilder().setPrettyPrinting().setLenient().create().toJson(data);
            return Linters.JSON.lint(s);
        } catch (Exception e) {
            log.error("Error rendering original data: {}", data, e);
            return Component.empty();
        }
    }
}

package personthecat.catlib.serialization.codec.context;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.datafixers.util.Either;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import personthecat.catlib.linting.SyntaxLinter;
import xjs.data.JsonFormat;
import xjs.data.JsonValue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public final class DecodeContextRenderer {
    private static final String ROOT_ERRORS = "catlib.errorText.rootErrors";
    private static final String LEAF_ERRORS = "catlib.errorText.leafErrors";
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
        for (final var errors : ctx.getErrors()) {
            comp.append(renderErrors(errors));
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

    private static Component renderData(Object data) {
        if (data instanceof JsonElement e) { // gson
            final var sw = new StringWriter();
            final var writer = new JsonWriter(sw);
            writer.setLenient(true);
            writer.setIndent("  ");
            try {
                Streams.write(e, writer);
            } catch (IOException ignored) { /* unreachable */ }
            return SyntaxLinter.DEFAULT_LINTER.lint(sw.toString());
        } else if (data instanceof JsonValue v) { // xjs
            return SyntaxLinter.DEFAULT_LINTER.lint(v.toString(JsonFormat.JSON_FORMATTED));
        }
        try {
            final var s = new GsonBuilder().setPrettyPrinting().setLenient().create().toJson(data);
            return SyntaxLinter.DEFAULT_LINTER.lint(s);
        } catch (Exception e) {
            log.error("Error rendering original data: {}", data, e);
            return Component.empty();
        }
    }
}

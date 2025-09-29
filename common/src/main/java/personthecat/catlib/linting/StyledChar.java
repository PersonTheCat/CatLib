package personthecat.catlib.linting;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

record StyledChar(int c, MutableObject<Style> style) {
    static List<StyledChar> fromText(String text) {
        return text.codePoints().mapToObj(i -> new StyledChar(i, new MutableObject<>(Style.EMPTY))).toList();
    }

    static void applyOverlay(List<StyledChar> chars, Component comp) {
        final var idx = new AtomicInteger();
        comp.visit((style, string) -> {
            for (int i = 0; i < string.length(); i++) {
                chars.get(idx.intValue() + i).applyOverlay(style);
            }
            idx.addAndGet(string.length());
            return Optional.empty();
        }, Style.EMPTY);
    }

    void applyOverlay(Style overlay) {
        this.style.setValue(overlay.applyTo(this.style.getValue()));
    }

    static Component toComponent(List<StyledChar> chars) {
        final var comp = Component.empty();
        if (chars.isEmpty()) {
            return comp;
        }
        final var sb = new StringBuilder();
        var current = chars.getFirst().style.getValue();
        for (final var ch : chars) {
            if (!ch.style.getValue().equals(current)) {
                comp.append(Component.literal(sb.toString()).withStyle(current));
                sb.setLength(0);
                current = ch.style.getValue();
            }
            sb.append(Character.toChars(ch.c));
        }
        if (!sb.isEmpty()) {
            comp.append(Component.literal(sb.toString()).withStyle(current));
        }
        return comp;
    }
}


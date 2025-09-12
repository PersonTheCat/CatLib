package personthecat.catlib.serialization.codec.context;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ErrorNode {
    public final Either<String, Integer> pathElement;
    public final Map<Either<String, Integer>, ErrorNode> children = new HashMap<>();
    public final List<Supplier<String>> messageSuppliers = new ArrayList<>();
    public final Supplier<Set<String>> messages;
    public final @Nullable ErrorNode parent;
    public volatile @Nullable String category;

    private ErrorNode(Either<String, Integer> pathElement, @Nullable ErrorNode parent) {
        this.pathElement = pathElement;
        this.parent = parent;
        if (parent != null) this.category = parent.category;
        this.messages = Suppliers.memoize(this::readMessages);
    }

    public static ErrorNode root() {
        return new ErrorNode(Either.left("<root>"), null);
    }

    public ErrorNode child(Either<String, Integer> element) {
        return this.children.computeIfAbsent(element, e -> new ErrorNode(e, this));
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    private Set<String> readMessages() {
        if (this.messageSuppliers.isEmpty()) {
            return Collections.emptySet();
        }
        if (this.parent == null) { // root gets full messages
            return this.messageSuppliers.stream()
                .map(Supplier::get)
                .collect(Collectors.toSet());
        }
        return this.messageSuppliers.stream() // child gets split messages
            .flatMap(s -> Stream.of(s.get().split("; ")))
            .collect(Collectors.toSet());
    }

    public boolean hasNestedErrors() {
        for (final var child : this.children.values()) {
            if (!child.messageSuppliers.isEmpty() || child.hasNestedErrors()) {
                return true;
            }
        }
        return false;
    }
}
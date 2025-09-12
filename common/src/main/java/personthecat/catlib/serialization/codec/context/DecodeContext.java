package personthecat.catlib.serialization.codec.context;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.Component;
import personthecat.catlib.command.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class DecodeContext {
    private final Deque<ErrorNode> stack = new ArrayDeque<>();
    private final ErrorNode root = ErrorNode.root();
    private final Map<String, CategorizedErrors> errors = new HashMap<>();
    private final List<String> categories = new ArrayList<>();
    private volatile @Nullable Object data;

    public void push(String key) {
        this.push(Either.left(key));
    }

    public void push(int index) {
        this.push(Either.right(index));
    }

    public void push(Either<String, Integer> pathElement) {
        this.stack.push(this.peek().child(pathElement));
    }

    public void pop() {
        if (this.stack.size() <= 1) { // including root
            throw new IllegalStateException("Nothing pushed");
        }
        final var leaf = this.stack.peek();
        if (!leaf.hasNestedErrors() && !leaf.messageSuppliers.isEmpty()) {
            this.errors.computeIfAbsent(leaf.category, CategorizedErrors::new)
                .erredLeaves()
                .addAll(this.currentPath(), leaf.messages.get());
        }
        this.stack.pop();
        if (this.stack.size() <= 1) { // reset root category
            this.root.setCategory(null);
        }
    }

    public void pushCategory(String category) {
        this.categories.add(category);
        this.resetCategory();
    }

    public void popCategory() {
        this.categories.removeLast();
        this.resetCategory();
    }

    private void resetCategory() {
        final var cat = this.categories.isEmpty() ? null : this.categories.getLast();
        this.peek().setCategory(cat);
    }

    public void reportError(String message) {
        this.reportError(() -> message);
    }

    public void reportError(DataResult.Error<?> error) {
        this.reportError(error.messageSupplier());
    }

    public void reportError(Supplier<String> error) {
        this.peek().messageSuppliers.add(error);
    }

    public void recordInput(Object data) {
        if (this.data != null) {
            throw new IllegalStateException("Data already present in decode context");
        }
        this.data = data;
    }

    public @Nullable Object getOriginalInput() {
        return this.data;
    }

    public ErrorNode getErrorRoot() {
        return this.root;
    }

    public Collection<CategorizedErrors> getErrors() {
        return this.errors.values();
    }

    public List<Either<String, Integer>> currentPath() {
        return this.stack.stream().takeWhile(n -> n.parent != null).map(n -> n.pathElement).toList().reversed();
    }

    public ErrorNode peek() {
        if (this.stack.isEmpty()) {
            this.stack.push(this.root);
        }
        return this.stack.peek();
    }

    public Component render() {
        return DecodeContextRenderer.render(this);
    }
}

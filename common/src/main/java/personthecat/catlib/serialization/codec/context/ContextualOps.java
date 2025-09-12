package personthecat.catlib.serialization.codec.context;

import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.DelegatingOps;

public interface ContextualOps<T> extends DynamicOps<T> {
    DecodeContext catlib$getContext();
    void catlib$resetContext();

    default boolean catlib$hasContext() {
        final var ctx = this.catlib$getContext();
        return !ctx.getErrorRoot().messageSuppliers.isEmpty() || !ctx.getErrors().isEmpty();
    }

    static <T> ContextualOps<T> create(DynamicOps<T> ops) {
        return new Implementation<>(ops);
    }

    class Implementation<T> extends DelegatingOps<T> implements ContextualOps<T> {
        private volatile DecodeContext context = new DecodeContext();

        private Implementation(DynamicOps<T> parent) {
            super(parent);
        }

        @Override
        public DecodeContext catlib$getContext() {
            return this.context;
        }

        @Override
        public void catlib$resetContext() {
            this.context = new DecodeContext();
        }
    }
}

package personthecat.catlib.serialization.codec.capture;

import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class Captures {
    private final KeyMap<Supplier<DataResult<?>>> map = new KeyMap<>();
    private final KeyMap<TypeSuggestion<?>> types = new KeyMap<>();

    Captures() {}

    @SuppressWarnings("unchecked")
    public <T> void put(Key<T> key, Supplier<DataResult<T>> supplier) {
        this.map.put(key, (Supplier<DataResult<?>>) (Object) supplier);
    }

    @SuppressWarnings("unchecked")
    public <T> void putIfAbsent(Key<T> key, Supplier<DataResult<T>> supplier) {
        this.map.putIfAbsent(key, (Supplier<DataResult<?>>) (Object) supplier);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable DataResult<T> get(Key<T> key) {
        return (DataResult<T>) this.map.getOrDefault(key, () -> null).get();
    }

    public <T> void putType(Key<T> key, TypeSuggestion<? extends T> type) {
        this.types.put(key, type);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable TypeSuggestion<T> getType(Key<T> key) {
        return (TypeSuggestion<T>) this.types.get(key);
    }

}

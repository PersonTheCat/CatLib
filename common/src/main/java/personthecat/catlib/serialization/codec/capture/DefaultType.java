package personthecat.catlib.serialization.codec.capture;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import personthecat.catlib.command.annotations.Nullable;

import java.util.Map;

public record DefaultType<A, B extends A>(Key<A> key, TypeSuggestion<B> type) implements Captor<A> {

    @Override
    public void capture(Captures captures) {
        captures.putType(this.key, this.type);
        captures.putIfAbsent(this.key, this::readEmptyMap);
    }

    private @Nullable DataResult<A> readEmptyMap() {
        return this.type.codec().compressedDecode(JavaOps.INSTANCE, Map.of())
            .mapOrElse(DataResult::success, e -> null);
    }
}

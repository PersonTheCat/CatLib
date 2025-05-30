package personthecat.catlib.serialization.codec.capture;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;

import java.util.Optional;

public record DefaultGetter<T>(Key<T> key, MapCodec<Optional<T>> codec) implements Captor<T> {

    @Override
    public <O> void capture(Captures captures, DynamicOps<O> ops, MapLike<O> input) {
        captures.put(this.key, () -> this.codec.decode(ops, input).mapOrElse(
            o -> o.map(DataResult::success).orElse(null),
            e -> DataResult.error(e.messageSupplier())
        ));
    }
}

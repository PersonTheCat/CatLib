package personthecat.catlib.serialization.codec.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@FunctionalInterface
public interface Receiver<T> extends Supplier<DataResult<T>> {
    default MapCodec<T> wrap(String name, Codec<T> codec) {
        return this.wrap(name, codec.optionalFieldOf(name));
    }

    default MapCodec<T> wrap(String name, MapCodec<Optional<T>> codec) {
        return codec.flatXmap(
            o -> o.map(DataResult::success).orElseGet(() -> this.get().mapError(e -> "No key " + name + "; " + e)),
            t -> DataResult.success(this.isDefaultResult(t) ? Optional.empty() : Optional.of(t))
        );
    }

    default boolean isDefaultResult(T actual) {
        return this.get().mapOrElse(t -> Objects.equals(actual, t), e -> false);
    }

    interface OptionalReceiver<T> extends Receiver<Optional<T>> {
        default MapCodec<Optional<T>> wrapOptional(String name, Codec<T> codec) {
            return this.wrapOptional(name, codec.optionalFieldOf(name));
        }

        default MapCodec<Optional<T>> wrapOptional(String name, MapCodec<Optional<T>> codec) {
            return codec.flatXmap(
                o -> o.map(t -> DataResult.success(Optional.of(t)))
                    .orElseGet(() -> this.get().mapError(e -> "Error on default for optional field " + name + "; " + e)),
                t -> DataResult.success(this.isDefaultResult(t) ? Optional.empty() : t)
            );
        }

         default Receiver<T> nullable() {
            return () -> this.get().mapOrElse(o -> o.map(DataResult::success).orElse(null), e -> e.map(Optional::orElseThrow));
        }
    }
}

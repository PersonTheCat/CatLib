package personthecat.catlib.serialization.codec.capture;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;

public interface Captor<T> {
    Key<T> key();

    default void capture(Captures captures) {}

    default <O> void capture(Captures captures, DynamicOps<O> ops, MapLike<O> input) {
        this.capture(captures);
    }
}

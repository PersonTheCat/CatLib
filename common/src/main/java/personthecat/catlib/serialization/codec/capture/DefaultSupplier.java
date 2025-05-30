package personthecat.catlib.serialization.codec.capture;

import com.mojang.serialization.DataResult;

public record DefaultSupplier<T>(Key<T> key, T hardDefault) implements Captor<T> {

    @Override
    public void capture(Captures captures) {
        captures.put(this.key, () -> DataResult.success(this.hardDefault));
    }
}

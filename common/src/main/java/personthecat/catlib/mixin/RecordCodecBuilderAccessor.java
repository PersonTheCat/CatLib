package personthecat.catlib.mixin;

import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Mixin(value = RecordCodecBuilder.class, remap = false)
public interface RecordCodecBuilderAccessor<O, F> {

    @Accessor(value = "getter", remap = false)
    Function<O, F> getter();

    @Accessor(value = "encoder", remap = false)
    Function<O, MapEncoder<F>> encoder();

    @Accessor(value = "decoder", remap = false)
    MapDecoder<F> decoder();
}

package personthecat.catlib.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.OptionalFieldCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import personthecat.catlib.serialization.codec.context.ContextualOps;

import java.util.Optional;

@Mixin(value = OptionalFieldCodec.class, remap = false)
public class OptionalFieldCodecMixin<A> {
    @Shadow @Final private String name;

    @WrapMethod(method = "decode", remap = false)
    private <T>DataResult<Optional<A>> wrapDecode(
            DynamicOps<T> ops, MapLike<T> input, Operation<DataResult<Optional<A>>> decode) {
        if (!(ops instanceof ContextualOps<T> c)) {
            return decode.call(ops, input);
        }
        c.catlib$getContext().push(this.name);
        try {
            return decode.call(ops, input)
                .ifError(c.catlib$getContext()::reportError);
        } finally {
            c.catlib$getContext().pop();
        }
    }
}

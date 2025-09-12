package personthecat.catlib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.catlib.serialization.codec.context.ContextualOps;

@Mixin(targets = "com.mojang.serialization.codecs.ListCodec$DecoderState", remap = false)
public class ListCodecMixin<E, T> {
    @Shadow private int totalCount;

    @WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;decode(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"))
    private DataResult<Pair<E, T>> wrapDecodeElement(
            Codec<E> elementCodec,
            DynamicOps<T> ops,
            T value,
            Operation<DataResult<Pair<E, T>>> decodeElement) {
        if (!(ops instanceof ContextualOps<T> c)) {
            return (decodeElement).call(elementCodec, ops, value); // args are correct
        }
        c.catlib$getContext().push(this.totalCount - 1);
        try {
            return (decodeElement).call(elementCodec, ops, value)
                .ifError(c.catlib$getContext()::reportError);
        } finally {
            c.catlib$getContext().pop();
        }
    }
}

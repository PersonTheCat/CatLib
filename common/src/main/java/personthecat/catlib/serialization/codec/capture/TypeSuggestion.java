package personthecat.catlib.serialization.codec.capture;

import com.mojang.serialization.MapCodec;

public record TypeSuggestion<A>(MapCodec<? extends A> codec, Class<? extends A> type, Class<A> parentType) {
}

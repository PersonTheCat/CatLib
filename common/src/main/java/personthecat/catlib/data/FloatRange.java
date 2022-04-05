package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.floats.FloatLists;
import lombok.EqualsAndHashCode;
import personthecat.catlib.serialization.codec.CodecUtils;

import java.util.List;
import java.util.Random;

import static personthecat.catlib.util.Shorthand.f;
import static personthecat.catlib.util.Shorthand.numBetween;

@EqualsAndHashCode
@SuppressWarnings("unused")
public class FloatRange {

    public static final Codec<FloatRange> CODEC =
        CodecUtils.FLOAT_LIST.xmap(FloatRange::fromList, FloatRange::toList);

    public final float min, max;

    public FloatRange(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public FloatRange(float a) {
        this(a, a);
    }

    public static FloatRange fromList(final List<Float> floats) {
        if (floats.isEmpty()) return new FloatRange(0.0F);
        float min = floats.get(0);
        float max = min;
        for (int i = 1; i < floats.size(); i++) {
            final float n = floats.get(i);
            min = Math.min(min, n);
            max = Math.max(max, n);
        }
        return new FloatRange(min, max);
    }

    public float rand(Random rand) {
        return numBetween(rand, min, max);
    }

    public float diff() {
        return max - min;
    }

    public FloatList toList() {
        return this.min == this.max ? FloatLists.singleton(this.min) : FloatArrayList.wrap(new float[] {this.min, this.max});
    }

    @Override
    public String toString() {
        return f("Range[{}~{}]", min, max);
    }
}

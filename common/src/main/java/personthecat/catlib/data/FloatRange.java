package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.floats.FloatLists;
import net.minecraft.util.Mth;
import personthecat.catlib.serialization.codec.CodecUtils;

import java.util.List;
import java.util.Random;

import static personthecat.catlib.util.LibUtil.f;
import static personthecat.catlib.util.LibUtil.numBetween;

public record FloatRange(float min, float max) {

    private static final FloatRange EMPTY = new FloatRange(0);
    public static final Codec<FloatRange> CODEC =
        CodecUtils.easyList(Codec.FLOAT).xmap(FloatRange::fromList, FloatRange::toList);

    public FloatRange(float a) {
        this(a, a);
    }

    public static FloatRange of(float a, float b) {
        if (a == b) return FloatRange.empty();
        if (a > b) return new FloatRange(b, a);
        return new FloatRange(a, b);
    }

    public static FloatRange of(float a) {
        return new FloatRange(a);
    }

    public static FloatRange fromList(final List<Float> floats) {
        if (floats.isEmpty()) return empty();
        float min = floats.getFirst();
        float max = min;
        for (int i = 1; i < floats.size(); i++) {
            final float n = floats.get(i);
            min = Math.min(min, n);
            max = Math.max(max, n);
        }
        return new FloatRange(min, max);
    }

    public static FloatRange empty() {
        return EMPTY;
    }

    public float rand(Random rand) {
        return numBetween(rand, this.min, this.max);
    }

    public boolean contains(float num) {
        return num >= this.min && num < this.max;
    }

    public float diff() {
        return this.max - this.min;
    }

    public float clamp(float f) {
        return Mth.clamp(f, this.min, this.max);
    }

    public FloatList toList() {
        return this.min == this.max ? FloatLists.singleton(this.min) : FloatArrayList.wrap(new float[] {this.min, this.max});
    }

    @Override
    public String toString() {
        return f("Range[{}~{}]", min, max);
    }
}

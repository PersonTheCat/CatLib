package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.floats.FloatLists;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.mutable.MutableFloat;
import personthecat.catlib.serialization.CodecUtils;

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
        final MutableFloat min = new MutableFloat(0);
        final MutableFloat max = new MutableFloat(0);
        floats.forEach(i -> {
            min.setValue(Math.min(min.getValue(), i));
            max.setValue(Math.max(max.getValue(), i));
        });
        return new FloatRange(min.getValue(), max.getValue());
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

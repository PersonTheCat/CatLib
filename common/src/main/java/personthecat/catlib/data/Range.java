package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.serialization.codec.CodecUtils;

import java.util.*;

import static personthecat.catlib.util.LibUtil.f;
import static personthecat.catlib.util.LibUtil.numBetween;

public record Range(int min, int max) implements Iterable<Integer> {

    private static final Range EMPTY = new Range(0);
    public static final Codec<Range> CODEC =
        CodecUtils.easyList(Codec.INT).xmap(Range::fromList, Range::toList);

    public Range(int max) {
        this(max, max);
    }

    public static Range of(int a, int b) {
        if (a == b) return empty();
        if (a > b) return new Range(b, a);
        return new Range(a, b);
    }

    public static Range of(int max) {
        return new Range(max);
    }

    public static FloatRange of(float a, float b) {
        return FloatRange.of(a, b);
    }

    public static FloatRange of(float a) {
        return FloatRange.of(a);
    }

    public static Range fromList(final List<Integer> ints) {
        if (ints.isEmpty()) return empty();
        int min = ints.getFirst();
        int max = min;
        for (int i = 1; i < ints.size(); i++) {
            final int n = ints.get(i);
            min = Math.min(min, n);
            max = Math.max(max, n);
        }
        return new Range(min, max);
    }

    public static Range empty() {
        return EMPTY;
    }

    public int rand(Random rand) {
        return numBetween(rand, min, max);
    }

    public boolean contains(int num) {
        return num >= min && num < max;
    }

    public int diff() {
        return max - min;
    }

    public boolean isEmpty() {
        return false;
    }

    public int clamp(int i) {
        return Mth.clamp(i, this.min, this.max);
    }

    public IntList toList() {
        return this.min == this.max ? IntLists.singleton(this.min) : IntArrayList.wrap(new int[] {this.min, this.max});
    }

    public <T> DataResult<T> validate(final T t, final Range... ranges) {
        for (final Range range : ranges) {
            if (!(this.contains(range.min) && this.contains(range.max))) {
                return DataResult.error(() -> f("Value outside of range: [{}~{}] is not in [{}~{}]",
                    range.min, range.max, this.min, this.max));
            }
        }
        return DataResult.success(t);
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            int i = min;

            @Override
            public boolean hasNext() {
                return i < max;
            }

            @Override
            public Integer next() {
                return i++;
            }
        };
    }

    @Override
    public String toString() {
        return f("Range[{}~{}]", min, max);
    }
}
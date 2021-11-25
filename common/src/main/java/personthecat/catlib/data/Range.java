package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.serialization.CodecUtils;

import java.util.*;

import static personthecat.catlib.util.Shorthand.f;
import static personthecat.catlib.util.Shorthand.numBetween;

@EqualsAndHashCode
@SuppressWarnings("unused")
public class Range implements Iterable<Integer> {

    public static final Codec<Range> CODEC =
        CodecUtils.INT_LIST.xmap(Range::fromList, Range::toList);

    public final int min, max;

    public Range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public Range(int max) {
        this(max, max);
    }

    public static Range of(int a, int b) {
        return a > b ? new Range(b, a) : new Range(a, b);
    }

    public static Range of(int max) {
        return new Range(max);
    }

    public static FloatRange of(float a, float b) {
        return a > b ? new FloatRange(b, a) : new FloatRange(a, b);
    }

    public static FloatRange of(float a) {
        return new FloatRange(a);
    }

    public static Range fromList(final List<Integer> ints) {
        if (ints.isEmpty()) return empty();
        final IntRef min = new IntRef(ints.get(0));
        final IntRef max = new IntRef(ints.get(0));
        for (int i = 1; i < ints.size(); i++) {
            final int n = ints.get(i);
            min.set(Math.min(min.get(), n));
            max.set(Math.max(max.get(), n));
        }
        return new Range(min.get(), max.get());
    }

    public static Range checkedOrEmpty(int min, int max) {
        return max > min ? new Range(min, max) : EmptyRange.get();
    }

    public static EmptyRange empty() {
        return EmptyRange.get();
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

    public IntList toList() {
        return this.min == this.max ? IntLists.singleton(this.min) : IntArrayList.wrap(new int[] {this.min, this.max});
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
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
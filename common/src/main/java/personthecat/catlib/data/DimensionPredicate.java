package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import personthecat.catlib.data.IdMatcher.InvertibleEntry;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.util.DimInjector;

import java.util.List;

public class DimensionPredicate extends IdList<DimensionType> {

    public static final Codec<DimensionPredicate> CODEC =
        codecFromTypes(Registries.DIMENSION_TYPE, IdMatcher.DEFAULT_TYPES, true, (Constructor<DimensionType, DimensionPredicate>) DimensionPredicate::new);
    public static final DimensionPredicate ALL_DIMENSIONS = builder().blacklist(true).build();

    protected DimensionPredicate(
            final ResourceKey<? extends Registry<DimensionType>> key,
            final List<InvertibleEntry<DimensionType>> entries,
            final boolean blacklist,
            final Format format) {
        super(key, entries, blacklist, format);
    }

    public boolean test(final LevelStem stem) {
        return this.test(stem.type());
    }

    public boolean test(final LevelReader level) {
        return this.test(level.dimensionType());
    }

    public boolean test(final ChunkAccess chunk) {
        if (chunk instanceof DimInjector) {
            final Holder<DimensionType> type = ((DimInjector) chunk).getType();
            if (type != null) {
                return this.test(type);
            }
        } else if (chunk instanceof LevelChunk levelChunk) {
            return this.test(levelChunk.getLevel().dimensionTypeRegistration());
        }
        return this.isEmpty();
    }

    public boolean test(final DimensionType type) {
        final Holder<DimensionType> holder = DynamicRegistries.DIMENSION_TYPE.getHolder(type);
        return holder != null && this.test(holder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public DimensionPredicate withBlacklist(final boolean blacklist) {
        return new DimensionPredicate(Registries.DIMENSION_TYPE, this.entries, blacklist, this.format);
    }

    public static class Builder extends IdList.Builder<DimensionType> {
        public Builder() {
            super(Registries.DIMENSION_TYPE);
        }

        @Override
        public Builder addEntry(final InvertibleEntry<DimensionType> entry) {
            return (Builder) super.addEntry(entry);
        }

        @Override
        public Builder addEntries(final List<InvertibleEntry<DimensionType>> entries) {
            return (Builder) super.addEntries(entries);
        }

        @Override
        public Builder format(final IdList.Format format) {
            return (Builder) super.format(format);
        }

        @Override
        public Builder blacklist(final boolean blacklist) {
            return (Builder) super.blacklist(blacklist);
        }

        @Override
        public DimensionPredicate build() {
            return this.build(DimensionPredicate::new);
        }
    }
}

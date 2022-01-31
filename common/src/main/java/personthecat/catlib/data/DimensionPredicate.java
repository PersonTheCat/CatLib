package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.registry.DynamicRegistries;
import personthecat.catlib.serialization.CodecUtils;
import personthecat.catlib.util.DimInjector;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static personthecat.catlib.serialization.CodecUtils.codecOf;
import static personthecat.catlib.serialization.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.FieldDescriptor.defaultGet;

@Getter
@Builder
@NotThreadSafe
@FieldNameConstants
@AllArgsConstructor
@RequiredArgsConstructor
public class DimensionPredicate implements Predicate<DimensionType> {

    @With private final boolean blacklist;

    // Todo: additional conditions
    @NotNull private final List<ResourceLocation> names;
    @NotNull private final List<String> mods;

    @Nullable private Set<DimensionType> compiled;

    public static final DimensionPredicate ALL_DIMENSIONS = builder().build();

    private static final Codec<DimensionPredicate> OBJECT_CODEC = codecOf(
        defaulted(Codec.BOOL, Fields.blacklist, false, DimensionPredicate::isBlacklist),
        defaultGet(CodecUtils.ID_LIST, Fields.names, Collections::emptyList, DimensionPredicate::getNames),
        defaultGet(CodecUtils.STRING_LIST, Fields.mods, Collections::emptyList, DimensionPredicate::getMods),
        DimensionPredicate::new
    );

    private static final Codec<DimensionPredicate> ID_CODEC =
        CodecUtils.ID_LIST.xmap(ids -> builder().names(ids).build(), DimensionPredicate::getNames);

    public static final Codec<DimensionPredicate> CODEC = simpleEither(ID_CODEC, OBJECT_CODEC)
        .withEncoder(dp -> dp.isNamesOnly() ? ID_CODEC : OBJECT_CODEC);

    public boolean test(final LevelStem stem) {
        return this.test(stem.type());
    }

    public boolean test(final LevelReader level) {
        return this.test(level.dimensionType());
    }

    public boolean test(final ChunkAccess chunk) {
        if (chunk instanceof DimInjector) {
            final DimensionType type = ((DimInjector) chunk).getType();
            if (type != null) {
                return this.test(type);
            }
        } else if (chunk instanceof LevelChunk) {
            return this.test(((LevelChunk) chunk).getLevel());
        }
        return this.isEmpty();
    }

    @Override
    public boolean test(final DimensionType type) {
        if (this.compiled == null) return this.compile().contains(type);
        return this.compiled.contains(type);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public synchronized Set<DimensionType> compile() {
        final Set<DimensionType> all = new HashSet<>();
        DynamicRegistries.DIMENSION_TYPES.forEach(all::add);

        if (this.isEmpty()) {
            return new InfinitySet<>(all);
        }
        final Set<DimensionType> matching = new HashSet<>();
        DynamicRegistries.DIMENSION_TYPES.forEach((id, type) -> {
            if (this.matches(type, id)) {
                matching.add(type);
            }
        });
        final boolean needsInit = this.compiled == null;
        this.compiled = new InvertibleSet<>(matching, this.blacklist).optimize(all);

        if (needsInit) {
            DynamicRegistries.listen(DynamicRegistries.DIMENSION_TYPES, this).accept(registry -> this.compile());
        }
        return this.compiled;
    }

    @NotNull
    public Set<DimensionType> getCompiled() {
        if (this.compiled == null) return this.compile();
        return this.compiled;
    }

    public boolean isEmpty() {
        return this.names.isEmpty() && this.mods.isEmpty();
    }

    public boolean matches(final DimensionType type, final ResourceLocation id) {
        return this.matchesName(id) && this.matchesMod(id);
    }

    public boolean matchesName(final ResourceLocation id) {
        return this.names.isEmpty() || this.names.contains(id);
    }

    public boolean matchesMod(final ResourceLocation id) {
        return this.mods.isEmpty() || this.mods.contains(id.getNamespace());
    }

    public boolean isNamesOnly() {
        return !this.blacklist && this.mods.isEmpty();
    }

    @Override
    public int hashCode() {
        if (this.compiled == null) return this.compile().hashCode();
        return this.compiled.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof DimensionPredicate) {
            return this.getCompiled().equals(((DimensionPredicate) o).getCompiled());
        }
        return false;
    }

    public static class DimensionPredicateBuilder {
        @SuppressWarnings("ConstantConditions")
        public DimensionPredicate build() {
            if (this.names == null) this.names = Collections.emptyList();
            if (this.mods == null) this.mods = Collections.emptyList();
            return new DimensionPredicate(this.blacklist, this.names, this.mods);
        }
    }
}

package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.registry.DynamicRegistries;
import personthecat.catlib.serialization.CodecUtils;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static personthecat.catlib.serialization.CodecUtils.codecOf;
import static personthecat.catlib.serialization.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.FieldDescriptor.defaultGet;
import static personthecat.catlib.serialization.FieldDescriptor.defaulted;

@Getter
@Builder
@NotThreadSafe
@FieldNameConstants
@AllArgsConstructor
@RequiredArgsConstructor
public class BiomePredicate implements Predicate<Biome> {

    @With private final boolean blacklist;

    // Todo: additional conditions
    private final List<ResourceLocation> names;
    private final List<String> mods;
    private final List<Biome.BiomeCategory> types;

    @Nullable private Set<Biome> compiled;

    public static final BiomePredicate ALL_BIOMES = builder().build();

    private static final Codec<BiomePredicate> OBJECT_CODEC = codecOf(
        defaulted(Codec.BOOL, Fields.blacklist, false, BiomePredicate::isBlacklist),
        defaultGet(CodecUtils.ID_LIST, Fields.names, Collections::emptyList, BiomePredicate::getNames),
        defaultGet(CodecUtils.STRING_LIST, Fields.mods, Collections::emptyList, BiomePredicate::getMods),
        defaultGet(CodecUtils.CATEGORY_LIST, Fields.types, Collections::emptyList, BiomePredicate::getTypes),
        BiomePredicate::new
    );

    private static final Codec<BiomePredicate> ID_CODEC =
        CodecUtils.ID_LIST.xmap(ids -> builder().names(ids).build(), BiomePredicate::getNames);

    public static final Codec<BiomePredicate> CODEC = simpleEither(ID_CODEC, OBJECT_CODEC)
        .withEncoder(bp -> bp.isNamesOnly() ? ID_CODEC : OBJECT_CODEC);

    @Override
    public boolean test(final Biome biome) {
        if (this.compiled == null) this.compiled = this.compile();
        return this.compiled.contains(biome);
    }

    @NotNull
    @SuppressWarnings("UnusedReturnValue")
    public Set<Biome> compile() {
        final Set<Biome> all = new HashSet<>();
        DynamicRegistries.BIOMES.forEach(all::add);

        if (this.isEmpty()) {
            return new InfinitySet<>(all);
        }
        final Set<Biome> matching = new HashSet<>();
        DynamicRegistries.BIOMES.forEach((id, biome) -> {
            if (this.matches(biome, id)) {
                matching.add(biome);
            }
        });
        return new InvertibleSet<>(matching, this.blacklist).optimize(all);
    }

    public boolean isEmpty() {
        return this.names.isEmpty() && this.types.isEmpty() && this.mods.isEmpty();
    }

    public boolean matches(final Biome biome, final ResourceLocation id) {
        return this.matchesName(id)
            && this.matchesMod(id)
            && this.matchesType(biome.getBiomeCategory());
    }

    public boolean matchesName(final ResourceLocation id) {
        return this.names.isEmpty() || this.names.contains(id);
    }

    public boolean matchesMod(final ResourceLocation id) {
        return this.mods.isEmpty() || this.mods.contains(id.getNamespace());
    }

    public boolean matchesType(final Biome.BiomeCategory type) {
        return this.types.isEmpty() || this.types.contains(type);
    }

    public boolean isNamesOnly() {
        return !this.blacklist && this.mods.isEmpty() && this.types.isEmpty();
    }
}

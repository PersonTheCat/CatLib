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
import personthecat.catlib.util.McUtils;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.function.Predicate;

import static personthecat.catlib.serialization.CodecUtils.codecOf;
import static personthecat.catlib.serialization.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.FieldDescriptor.defaultGet;
import static personthecat.catlib.serialization.FieldDescriptor.defaulted;

@Getter
@Builder
@NotThreadSafe
@FieldNameConstants
@RequiredArgsConstructor
public class BiomePredicate implements Predicate<Biome> {

    @With private final boolean blacklist;

    // Todo: additional conditions
    @NotNull private final List<ResourceLocation> names;
    @NotNull private final List<String> mods;
    @NotNull private final List<Biome.BiomeCategory> types;

    @Nullable private Set<Biome> compiled;

    private int registryTracker = DynamicRegistries.BIOMES.getId();

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
        return this.getCompiled().contains(biome);
    }

    @NotNull
    @SuppressWarnings("UnusedReturnValue")
    public synchronized Set<Biome> compile() {
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
        this.registryTracker = DynamicRegistries.BIOMES.getId();
        return this.compiled = new InvertibleSet<>(matching, this.blacklist).optimize(all);
    }

    @NotNull
    public Set<Biome> getCompiled() {
        if (this.compiled == null || this.registryTracker != DynamicRegistries.BIOMES.getId()) {
            return this.compile();
        }
        return this.compiled;
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

    public BiomePredicate simplify() {
        if (this.equals(ALL_BIOMES)) {
            return ALL_BIOMES;
        }
        final InvertibleSet<Biome> biomes = this.reconstruct();
        final MultiValueMap<Biome.BiomeCategory, Biome> categories = categorize(biomes);
        final List<ResourceLocation> names = new ArrayList<>();
        final List<Biome.BiomeCategory> types = new ArrayList<>();

        for (final Map.Entry<Biome.BiomeCategory, List<Biome>> entry : categories.entrySet()) {
            final List<Biome> possible = McUtils.getBiomes(entry.getKey());
            if (possible.size() == entry.getValue().size()) {
                types.add(entry.getKey());
            } else {
                for (final Biome biome : entry.getValue()) {
                    names.add(DynamicRegistries.BIOMES.getKey(biome));
                }
            }
        }
        return new BiomePredicate(biomes.isBlacklist(), names, Collections.emptyList(), types);
    }

    private InvertibleSet<Biome> reconstruct() {
        final Set<Biome> compiled = this.compiled != null ? this.compiled : this.compile();
        final Set<Biome> all = new HashSet<>();
        DynamicRegistries.BIOMES.forEach(all::add);
        if (compiled.size() > all.size() / 2) {
            final Set<Biome> inverted = new HashSet<>(all);
            inverted.removeAll(compiled);
            return InvertibleSet.wrap(inverted).blacklist(true);
        }
        return InvertibleSet.wrap(compiled);
    }

    private static MultiValueMap<Biome.BiomeCategory, Biome> categorize(final Collection<Biome> biomes) {
        final MultiValueMap<Biome.BiomeCategory, Biome> categories = new MultiValueHashMap<>();
        for (final Biome biome : biomes) {
            categories.add(biome.getBiomeCategory(), biome);
        }
        return categories;
    }

    @Override
    public int hashCode() {
        return this.getCompiled().hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof BiomePredicate) {
            return this.getCompiled().equals(((BiomePredicate) o).getCompiled());
        }
        return false;
    }

    public static class BiomePredicateBuilder {
        @SuppressWarnings("ConstantConditions")
        public BiomePredicate build() {
            if (this.names == null) this.names = Collections.emptyList();
            if (this.mods == null) this.mods = Collections.emptyList();
            if (this.types == null) this.types = Collections.emptyList();
            return new BiomePredicate(this.blacklist, this.names, this.mods, this.types);
        }
    }
}

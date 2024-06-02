package personthecat.catlib.data;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import personthecat.catlib.data.IdMatcher.InvertibleEntry;
import personthecat.catlib.data.collections.InvertibleSet;
import personthecat.catlib.data.collections.MultiValueHashMap;
import personthecat.catlib.data.collections.MultiValueMap;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.util.McUtils;
import personthecat.catlib.util.LibUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BiomePredicate extends IdList<Biome> {

    private static final List<IdMatcher.Info<?>> TYPES =
        ImmutableList.<IdMatcher.Info<?>>builder()
            .addAll(IdMatcher.DEFAULT_TYPES).add(TypeMatcher.INFO).build();
    public static final Codec<BiomePredicate> CODEC =
        codecFromTypes(Registries.BIOME, TYPES, (Constructor<Biome, BiomePredicate>) BiomePredicate::new);
    public static final BiomePredicate ALL_BIOMES = builder().build();

    protected BiomePredicate(
            final ResourceKey<? extends Registry<Biome>> key,
            final List<IdMatcher.InvertibleEntry> entries,
            final boolean blacklist,
            final Format format) {
        super(key, entries, blacklist, format);
    }

    @Deprecated
    public boolean test(final Biome biome) {
        final Holder<Biome> holder = DynamicRegistries.BIOMES.getHolder(biome);
        return holder != null && this.test(holder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public BiomePredicate withBlacklist(final boolean blacklist) {
        return new BiomePredicate(Registries.BIOME, this.entries, blacklist, this.format);
    }

    // matches previous behavior for generated lists. may improve api in the future
    public BiomePredicate simplify() {
        if (this.equals(ALL_BIOMES)) {
            return ALL_BIOMES;
        }
        if (this.format == Format.LIST) {
            return this;
        }
        final InvertibleSet<Holder<Biome>> biomes = this.reconstruct();
        final MultiValueMap<BiomeType, Holder<Biome>> categories = categorize(biomes.entries());
        final List<InvertibleEntry> entries = new ArrayList<>();

        for (final Map.Entry<BiomeType, List<Holder<Biome>>> entry : categories.entrySet()) {
            final List<Holder<Biome>> possible = McUtils.getBiomes(entry.getKey());
            if (possible.size() == entry.getValue().size()) {
                entries.add(new InvertibleEntry(false, new TypeMatcher(entry.getKey())));
            } else {
                for (final Holder<Biome> biome : entry.getValue()) {
                    entries.add(new InvertibleEntry(false, new IdMatcher.Id(DynamicRegistries.BIOMES.keyOf(biome))));
                }
            }
        }
        return new BiomePredicate(Registries.BIOME, entries, biomes.blacklist(), Format.OBJECT);
    }

    private InvertibleSet<Holder<Biome>> reconstruct() {
        final HolderSet<Biome> compiled = this.compiled != null ? this.compiled : this.compile();
        final Set<Holder<Biome>> all = new HashSet<>(DynamicRegistries.BIOMES.holders());
        if (compiled.size() > all.size() / 2) {
            final Set<Holder<Biome>> inverted = new HashSet<>(all);
            compiled.forEach(inverted::remove);
            return new InvertibleSet<>(inverted, true);
        }
        return new InvertibleSet<>(compiled.stream().collect(Collectors.toSet()), false);
    }

    private static MultiValueMap<BiomeType, Holder<Biome>> categorize(final Collection<Holder<Biome>> biomes) {
        final MultiValueMap<BiomeType, Holder<Biome>> categories = new MultiValueHashMap<>();
        for (final Holder<Biome> biome : biomes) {
            BiomeType.getCategories(biome).forEach(t -> categories.add(t, biome));
        }
        return categories;
    }

    public static class Builder extends IdList.Builder<Biome> {
        public Builder() {
            super(Registries.BIOME);
        }

        @Override
        public Builder addEntries(final InvertibleEntry... entries) {
           return (Builder) super.addEntries(entries);
        }

        @Override
        public Builder addEntries(final List<InvertibleEntry> entries) {
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
        public BiomePredicate build() {
            return this.build(BiomePredicate::new);
        }
    }

    public static InvertibleEntry type(final boolean invert, final BiomeType biomeType) {
        return new InvertibleEntry(invert, new TypeMatcher(biomeType));
    }

    public record TypeMatcher(BiomeType biomeType) implements IdMatcher.Typed<Biome> {
        public static final Info<TypeMatcher> INFO = new StringRepresentable<>() {
            @Override
            public String valueOf(final TypeMatcher typeMatcher) {
                return typeMatcher.biomeType.name().toLowerCase();
            }

            @Override
            public TypeMatcher newFromString(final String s) {
                return LibUtil.getEnumConstant(s, BiomeType.class)
                    .map(TypeMatcher::new)
                    .orElse(null);
            }

            @Override
            public String fieldName() {
                return "types";
            }

            @Override
            public String toString() {
                return "TYPE";
            }
        };

        @Override
        public ResourceKey<? extends Registry<Biome>> type() {
            return Registries.BIOME;
        }

        @Override
        public void addTyped(final RegistryHandle<Biome> handle, final Set<ResourceLocation> out) {
            this.biomeType.getTag().forEach(holder -> out.add(handle.keyOf(holder)));
        }

        @Override
        public Info<? extends IdMatcher> info() {
            return INFO;
        }
    }
}

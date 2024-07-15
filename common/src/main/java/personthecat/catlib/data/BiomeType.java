package personthecat.catlib.data;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.DynamicRegistries;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum BiomeType {
    BADLANDS,
    BEACH,
    DEEP_OCEAN,
    END,
    FOREST,
    HILL,
    JUNGLE,
    MOUNTAIN,
    NETHER,
    OCEAN,
    OVERWORLD,
    RIVER,
    SAVANNA,
    TAIGA;

    private static final Map<ResourceLocation, Collection<BiomeType>> TYPE_MAP = new ConcurrentHashMap<>();

    private final TagKey<Biome> key = TagKey.create(Registries.BIOME,
        new ResourceLocation("is_" + this.name().toLowerCase()));

    public static @Nullable BiomeType getFirstCategory(final Holder<Biome> b) {
        final Collection<BiomeType> types = getCategories(b);
        return types.isEmpty() ? null : types.iterator().next();
    }

    public static Collection<BiomeType> getCategories(final Holder<Biome> b) {
        return TYPE_MAP.computeIfAbsent(DynamicRegistries.BIOME.keyOf(b), key ->
            Stream.of(values())
                .filter(type -> type.matches(b))
                .collect(Collectors.toSet()));
    }

    public boolean matches(final Holder<Biome> b) {
        return this.getTag().contains(b);
    }

    public boolean matches(final Biome b) {
        final Holder<Biome> holder = DynamicRegistries.BIOME.getHolder(b);
        return holder != null && this.matches(holder);
    }

    public TagKey<Biome> getKey() {
        return this.key;
    }

    public HolderSet.Named<Biome> getTag() {
        return DynamicRegistries.BIOME.getTags().get(this.key);
    }
}

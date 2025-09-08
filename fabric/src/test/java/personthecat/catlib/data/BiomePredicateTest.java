package personthecat.catlib.data;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import personthecat.catlib.data.IdMatcher.InvertibleEntry;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.registry.DynamicRegistryHandle;
import personthecat.catlib.registry.MojangRegistryHandle;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.test.McBootstrapExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static personthecat.catlib.test.TestUtils.encode;

@ExtendWith(McBootstrapExtension.class)
public class BiomePredicateTest {

    @BeforeAll
    @SuppressWarnings("ConstantConditions")
    public static void setup() {
        final Registry<Biome> biomes = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        final RegistryHandle<Biome> handle = new MojangRegistryHandle<>(biomes);

        // add test biomes
        handle.register(Biomes.FOREST, dummyBiome());
        handle.register(Biomes.BIRCH_FOREST, dummyBiome());
        handle.register(Biomes.PLAINS, dummyBiome());
        handle.register(Biomes.DESERT, dummyBiome());
        handle.register(Biomes.SWAMP, dummyBiome());
        handle.register(Biomes.JUNGLE, dummyBiome());
        handle.register(Biomes.SNOWY_PLAINS, dummyBiome());
        handle.register(Biomes.OCEAN, dummyBiome());
        handle.register(Biomes.DEEP_OCEAN, dummyBiome());

        // register test tags
        biomes.getOrCreateTag(BiomeTags.IS_FOREST);
        biomes.getOrCreateTag(BiomeTags.IS_OCEAN);

        // bind tag values
        final Map<TagKey<Biome>, List<Holder<Biome>>> map = new HashMap<>();
        Stream.of(BiomeType.values())
            .forEach(type -> map.put(type.getKey(), List.of()));
        map.put(BiomeTags.IS_FOREST, List.of(
            handle.getHolder(Biomes.FOREST),
            handle.getHolder(Biomes.BIRCH_FOREST)));
        map.put(BiomeTags.IS_OCEAN, List.of(
            handle.getHolder(Biomes.OCEAN),
            handle.getHolder(Biomes.DEEP_OCEAN)));
        biomes.bindTags(map);

        ((DynamicRegistryHandle<Biome>) DynamicRegistries.BIOME).updateRegistry(handle);
    }

    @Test
    public void biomePredicate_matchesBiomeTypes() {
        final RegistryHandle<Biome> biomes = DynamicRegistries.BIOME;
        final BiomePredicate predicate =
            BiomePredicate.builder()
                .addEntry(BiomePredicate.type(false, BiomeType.OCEAN))
                .build();
        assertTrue(predicate.test(biomes.getHolder(Biomes.OCEAN)));
        assertTrue(predicate.test(biomes.getHolder(Biomes.DEEP_OCEAN)));
        assertFalse(predicate.test(biomes.getHolder(Biomes.FOREST)));
    }

    @Test
    public void allBiomes_alwaysReturnsTrue() {
        assertTrue(BiomePredicate.ALL_BIOMES.test(DynamicRegistries.BIOME.getHolder(new ResourceLocation("forest"))));
    }

    @Test
    public void simplify_sortsNamedBiomes_intoCategories() {
        final BiomePredicate predicate = BiomePredicate.builder()
            .addEntries(List.of(
                IdMatcher.id(false, Biomes.FOREST),
                IdMatcher.id(false, Biomes.OCEAN),
                IdMatcher.id(false, Biomes.DEEP_OCEAN)))
            .format(IdList.Format.OBJECT)
            .build();
        // type is simplified because of tag values in beforeEach
        final BiomePredicate expected = BiomePredicate.builder()
            .addEntries(List.of(
                IdMatcher.id(false, Biomes.FOREST),
                BiomePredicate.type(false, BiomeType.OCEAN)))
            .format(IdList.Format.OBJECT)
            .build();
        // compare encoded data due to order issues
        assertEquals(
            encode(BiomePredicate.CODEC, expected),
            encode(BiomePredicate.CODEC, predicate.simplify()));
    }

    @Test
    public void simplify_whenMostPossibleEntriesArePresent_convertsToBlacklist() {
        final List<InvertibleEntry<Biome>> entries = new ArrayList<>();
        // user listed all entries except minecraft:forest
        DynamicRegistries.BIOME.forEach((key, holder) ->
            entries.add(IdMatcher.id(false, key)));
        entries.removeIf(entry ->
            ((IdMatcher.Id<Biome>) entry.matcher()).id().equals(Biomes.FOREST));
        final BiomePredicate predicate =
            BiomePredicate.builder()
                .addEntries(entries)
                .format(IdList.Format.OBJECT)
                .build();
        final BiomePredicate expected =
            BiomePredicate.builder()
                .addEntry(IdMatcher.id(false, Biomes.FOREST))
                .blacklist(true)
                .format(IdList.Format.OBJECT)
                .build();
        assertEquals(expected, predicate.simplify());
    }

    private static Biome dummyBiome() {
        return new Biome.BiomeBuilder()
            .temperature(0)
            .downfall(0)
            .specialEffects(
                new BiomeSpecialEffects.Builder()
                    .fogColor(0)
                    .waterColor(0)
                    .waterFogColor(0)
                    .skyColor(0)
                    .build())
            .mobSpawnSettings(
                new MobSpawnSettings.Builder()
                    .build())
            .generationSettings(
                new BiomeGenerationSettings.PlainBuilder()
                    .build())
            .build();
    }
}

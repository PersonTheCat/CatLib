package personthecat.catlib.data;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(McBootstrapExtension.class)
public class BiomePredicateTest {

    @BeforeAll
    @SuppressWarnings("ConstantConditions")
    public static void setup() {
        final Registry<Biome> biomes = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        final RegistryHandle<Biome> handle = new MojangRegistryHandle<>(biomes);

        // add test biomes
        handle.register(new ResourceLocation("forest"), dummyBiome());
        handle.register(new ResourceLocation("plains"), dummyBiome());
        handle.register(new ResourceLocation("desert"), dummyBiome());
        handle.register(new ResourceLocation("swamp"), dummyBiome());
        handle.register(new ResourceLocation("jungle"), dummyBiome());
        handle.register(new ResourceLocation("tundra"), dummyBiome());
        handle.register(new ResourceLocation("ocean"), dummyBiome());
        handle.register(new ResourceLocation("deep_ocean"), dummyBiome());

        // register test tags
        final TagKey<Biome> isForest = TagKey.create(Registries.BIOME, new ResourceLocation("is_forest"));
        final TagKey<Biome> isOcean = TagKey.create(Registries.BIOME, new ResourceLocation("is_ocean"));
        biomes.getOrCreateTag(isForest);
        biomes.getOrCreateTag(isOcean);

        // bind tag values
        final Map<TagKey<Biome>, List<Holder<Biome>>> map = new HashMap<>();
        Stream.of(BiomeType.values())
            .forEach(type -> map.put(type.getKey(), List.of()));
        map.put(isForest, List.of(
            handle.getHolder(new ResourceLocation("forest"))));
        map.put(isOcean, List.of(
            handle.getHolder(new ResourceLocation("ocean")),
            handle.getHolder(new ResourceLocation("deep_ocean"))));
        biomes.bindTags(map);

        ((DynamicRegistryHandle<Biome>) DynamicRegistries.BIOMES).updateRegistry(handle);
    }

    @Test
    public void biomePredicate_matchesBiomeTypes() {
        final RegistryHandle<Biome> biomes = DynamicRegistries.BIOMES;
        final BiomePredicate predicate =
            BiomePredicate.builder()
                .addEntries(BiomePredicate.type(false, BiomeType.OCEAN))
                .build();
        assertTrue(predicate.test(biomes.getHolder(new ResourceLocation("ocean"))));
        assertTrue(predicate.test(biomes.getHolder(new ResourceLocation("deep_ocean"))));
        assertFalse(predicate.test(biomes.getHolder(new ResourceLocation("forest"))));
    }

    @Test
    public void simplify_sortsNamedBiomes_intoCategories() {
        final BiomePredicate predicate = BiomePredicate.builder()
            .addEntries(
                IdMatcher.id(false, new ResourceLocation("forest")),
                IdMatcher.id(false, new ResourceLocation("ocean")),
                IdMatcher.id(false, new ResourceLocation("deep_ocean")))
            .format(IdList.Format.OBJECT)
            .build();
        // type is simplified because of tag values in beforeEach
        final BiomePredicate expected = BiomePredicate.builder()
            .addEntries(
                IdMatcher.id(false, new ResourceLocation("forest")),
                BiomePredicate.type(false, BiomeType.OCEAN))
            .format(IdList.Format.OBJECT)
            .build();
        assertEquals(expected, predicate.simplify());
    }

    @Test
    public void simplify_whenMostPossibleEntriesArePresent_convertsToBlacklist() {
        final List<IdMatcher.InvertibleEntry> entries = new ArrayList<>();
        // user listed all entries except minecraft:forest
        DynamicRegistries.BIOMES.forEach((id, holder) ->
            entries.add(IdMatcher.id(false, id)));
        entries.removeIf(entry ->
            ((IdMatcher.Id) entry.matcher()).id().equals(new ResourceLocation("forest")));
        final BiomePredicate predicate =
            BiomePredicate.builder()
                .addEntries(entries)
                .format(IdList.Format.OBJECT)
                .build();
        final BiomePredicate expected =
            BiomePredicate.builder()
                .addEntries(IdMatcher.id(false, new ResourceLocation("forest")))
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

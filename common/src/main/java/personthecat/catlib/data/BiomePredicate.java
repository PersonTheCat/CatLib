package personthecat.catlib.data;

import lombok.Builder;
import lombok.With;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.event.registry.DynamicRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Builder
public class BiomePredicate implements Predicate<Biome>, BiPredicate<Biome, ResourceLocation> {

    @With private final boolean negate;
    @With private final boolean blacklist;

    private final List<ResourceLocation> names;
    private final List<Biome.BiomeCategory> types;

    @Override
    public boolean test(final Biome biome, final ResourceLocation id) {
        return (this.names.isEmpty() || this.names.contains(id)) && this.test(biome);
    }

    @Override
    public boolean test(final Biome biome) {
        // Todo: additional conditions
        return this.types.isEmpty() || this.types.contains(biome.getBiomeCategory());
    }

    @NotNull
    @Override
    public BiomePredicate negate() {
        return this.withNegate(!this.negate);
    }

    @NotNull
    @SuppressWarnings("UnusedReturnValue")
    public Set<Biome> compile() {
        final Set<Biome> all = new HashSet<>();
        DynamicRegistries.BIOMES.forEach(all::add);

        if (this.names.isEmpty() && this.types.isEmpty()) {
            return new InfinitySet<>(all);
        }
        final Set<Biome> matching = new HashSet<>();
        DynamicRegistries.BIOMES.forEach((id, biome) -> {
            if (this.test(biome, id)) {
                matching.add(biome);
            }
        });
        return new InvertibleSet<>(matching, this.blacklist).optimize(all);
    }
}

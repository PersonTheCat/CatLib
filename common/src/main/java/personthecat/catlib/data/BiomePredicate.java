package personthecat.catlib.data;

import lombok.Builder;
import lombok.With;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.registry.DynamicRegistries;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@Builder
@NotThreadSafe
public class BiomePredicate implements Predicate<Biome> {

    @With private final boolean blacklist;

    // Todo: additional conditions
    private final List<ResourceLocation> names;
    private final List<String> mods;
    private final List<Biome.BiomeCategory> types;

    @Nullable private Set<Biome> compiled;

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
}

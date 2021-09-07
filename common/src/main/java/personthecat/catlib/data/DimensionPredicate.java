package personthecat.catlib.data;

import lombok.Builder;
import lombok.With;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
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
public class DimensionPredicate implements Predicate<DimensionType> {

    @With private final boolean blacklist;

    // Todo: additional conditions
    private final List<ResourceLocation> names;
    private final List<String> mods;

    @Nullable private Set<DimensionType> compiled;

    @Override
    public boolean test(final DimensionType type) {
        if (this.compiled == null) this.compiled = this.compile();
        return this.compiled.contains(type);
    }

    @NotNull
    public Set<DimensionType> compile() {
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
        return new InvertibleSet<>(matching, this.blacklist).optimize(all);
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
}

package personthecat.catlib.event.world;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.IdMatcher;
import personthecat.catlib.event.LibEvent;
import personthecat.catlib.registry.DynamicRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

// todo: Support phases
public class FeatureModificationEvent {
    private static final LibEvent<Consumer<FeatureModificationContext>> GLOBAL_EVENT =
        LibEvent.create(callbacks -> ctx -> callbacks.forEach(c -> c.accept(ctx)));
    private static final List<Listener> BIOME_EVENTS = Collections.synchronizedList(new ArrayList<>());

    public static boolean hasAnyEvents() {
        return !GLOBAL_EVENT.isEmpty() || !BIOME_EVENTS.isEmpty();
    }

    public static boolean hasEvent(final ResourceLocation id) {
        if (!GLOBAL_EVENT.isEmpty()) return true;
        final Holder<Biome> holder = DynamicRegistries.BIOMES.getHolder(id);
        if (holder == null) return false;
        return BIOME_EVENTS.stream().anyMatch(l -> l.predicate.test(holder));
    }

    public static LibEvent<Consumer<FeatureModificationContext>> global() {
        return GLOBAL_EVENT;
    }

    public static Consumer<FeatureModificationContext> get(final ResourceLocation id) {
        final Holder<Biome> holder = DynamicRegistries.BIOMES.getHolder(id);
        if (holder == null) throw new IllegalStateException("No such biome in registry: " + id);
        return ctx -> BIOME_EVENTS.stream()
            .filter(listener -> listener.predicate.test(holder))
            .forEach(listener -> listener.listener.accept(ctx));
    }

    public static Registrar forBiome(final ResourceLocation id) {
        return forBiomes(new IdMatcher.Id(id));
    }

    public static Registrar forBiomes(final TagKey<Biome> tag) {
        return forBiomes(new IdMatcher.Tag(tag.location()));
    }

    public static Registrar forBiomes(final IdMatcher matcher) {
        return forBiomes(BiomePredicate.builder().addEntries(new IdMatcher.InvertibleEntry(false, matcher)).build());
    }

    public static Registrar forBiomes(final BiomePredicate predicate) {
        return listener -> BIOME_EVENTS.add(new Listener(predicate, listener));
    }

    public interface Registrar {
        void register(final Consumer<FeatureModificationContext> listener);
    }

    private record Listener(BiomePredicate predicate, Consumer<FeatureModificationContext> listener) {}
}

package personthecat.catlib.event.world;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.apache.commons.lang3.mutable.MutableBoolean;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.IdMatcher;
import personthecat.catlib.data.collections.ObserverSet;
import personthecat.catlib.event.LibEvent;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeatureModificationEvent {
    private static final Map<Phase, LibEvent<Listener>> EVENTS =
        Stream.of(Phase.values()).collect(Collectors.toMap(Function.identity(), p -> LibEvent.create(Invoker::new)));

    public static Listener get(final Phase phase) {
        return EVENTS.get(phase).invoker();
    }

    public static void register(final Listener.Global global) {
        register(Phase.MODIFICATIONS, global);
    }

    public static void register(final Phase phase, final Listener.Global global) {
        register(phase, (Listener) global);
    }

    private static void register(final Phase phase, final Listener listener) {
        EVENTS.get(phase).register(listener);
    }

    public static LocalRegistrar forBiome(final ResourceLocation id) {
        return forBiome(ResourceKey.create(Registries.BIOME, id));
    }

    public static LocalRegistrar forBiome(final ResourceKey<Biome> key) {
        return forBiomes(new IdMatcher.Id<>(key));
    }

    public static LocalRegistrar forBiomes(final TagKey<Biome> tag) {
        return forBiomes(new IdMatcher.Tag<>(tag));
    }

    public static LocalRegistrar forBiomes(final IdMatcher<Biome> matcher) {
        return forBiomes(BiomePredicate.builder().addEntry(matcher.entry(false)).build());
    }

    public static LocalRegistrar forBiomes(final Predicate<Holder<Biome>> filter) {
        return (phase, listener) -> register(phase, new ListenerRecord(filter, listener));
    }

    @FunctionalInterface
    public interface LocalRegistrar extends BiConsumer<Phase, Consumer<FeatureModificationContext>> {
        default LocalRegistrar register(final Phase phase, final Consumer<FeatureModificationContext> listener) {
            this.accept(phase, listener);
            return this;
        }

        default LocalRegistrar register(final Consumer<FeatureModificationContext> listener) {
            return this.register(Phase.MODIFICATIONS, listener);
        }
    }

    public interface Listener {
        boolean isValidForBiome(final Holder<Biome> biome);
        void modifyBiome(final FeatureModificationContext ctx);

        default void modifyBiomeIfValid(final FeatureModificationContext ctx) {
            if (this.isValidForBiome(ctx.getBiome())) {
                this.modifyBiome(ctx);
            }
        }

        @FunctionalInterface
        interface Global extends Listener {
            @Override
            default boolean isValidForBiome(final Holder<Biome> biome) {
                return true;
            }
        }
    }

    private record ListenerRecord(
            Predicate<Holder<Biome>> filter, Consumer<FeatureModificationContext> listener) implements Listener {

        @Override
        public boolean isValidForBiome(Holder<Biome> biome) {
            return this.filter.test(biome);
        }

        @Override
        public void modifyBiome(FeatureModificationContext ctx) {
            this.listener.accept(ctx);
        }
    }

    private record Invoker(ObserverSet<Listener> listeners) implements Listener {

        @Override
        public boolean isValidForBiome(Holder<Biome> biome) {
            final var anyValid = new MutableBoolean();
            this.listeners.forEach(listener -> {
                if (!anyValid.booleanValue() && listener.isValidForBiome(biome)) {
                    anyValid.setTrue();
                }
            });
            return anyValid.booleanValue();
        }

        @Override
        public void modifyBiome(FeatureModificationContext ctx) {
            this.listeners.forEach(listener -> listener.modifyBiomeIfValid(ctx));
        }
    }

    public enum Phase {
        ADDITIONS,
        REMOVALS,
        MODIFICATIONS,
    }
}

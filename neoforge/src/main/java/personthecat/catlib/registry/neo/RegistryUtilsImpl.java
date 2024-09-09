package personthecat.catlib.registry.neo;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;
import net.neoforged.neoforge.registries.holdersets.HolderSetType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.registry.MojangRegistryHandle;
import personthecat.catlib.registry.RegistryHandle;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RegistryUtilsImpl {
    private static final Map<Class<?>, RegistryHandle<?>> REGISTRY_BY_TYPE = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<? extends Registry<?>>, RegistryHandle<?>> NEO_REGISTRIES = new ConcurrentHashMap<>();

    static {
        // statically map the neo registries by hand since there's no parent registry for them
        mapNeo(EntityDataSerializer.class, Keys.ENTITY_DATA_SERIALIZERS, NeoForgeRegistries.ENTITY_DATA_SERIALIZERS);
        mapNeo(null, Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS);
        mapNeo(null, Keys.BIOME_MODIFIER_SERIALIZERS, NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS);
        mapNeo(null, Keys.STRUCTURE_MODIFIER_SERIALIZERS, NeoForgeRegistries.STRUCTURE_MODIFIER_SERIALIZERS);
        mapNeo(FluidType.class, Keys.FLUID_TYPES, NeoForgeRegistries.FLUID_TYPES);
        mapNeo(HolderSetType.class, Keys.HOLDER_SET_TYPES, NeoForgeRegistries.HOLDER_SET_TYPES);
        mapNeo(ItemDisplayContext.class, Keys.DISPLAY_CONTEXTS, NeoForgeRegistries.DISPLAY_CONTEXTS);
        mapNeo(IngredientType.class, Keys.INGREDIENT_TYPES, NeoForgeRegistries.INGREDIENT_TYPES);
        mapNeo(null, Keys.CONDITION_CODECS, NeoForgeRegistries.CONDITION_SERIALIZERS);
        mapNeo(null, Keys.ENTITY_DATA_SERIALIZERS, NeoForgeRegistries.ENTITY_DATA_SERIALIZERS);
        mapNeo(AttachmentType.class, Keys.ATTACHMENT_TYPES, NeoForgeRegistries.ATTACHMENT_TYPES);
    }

    private RegistryUtilsImpl() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> Optional<RegistryHandle<T>> tryGetHandle(final ResourceKey<? extends Registry<T>> key) {
        final RegistryHandle<?> neoHandle = NEO_REGISTRIES.get(key);
        if (neoHandle != null) {
            return Optional.of((RegistryHandle) neoHandle);
        }
        final Registry<T> builtinRegistry = (Registry<T>) BuiltInRegistries.REGISTRY.get(key.location());
        if (builtinRegistry != null) {
            return Optional.of(new MojangRegistryHandle<>(builtinRegistry));
        }
        return Optional.empty();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> Optional<RegistryHandle<T>> tryGetByType(final Class<T> clazz) {
        return Optional.ofNullable((RegistryHandle<T>) REGISTRY_BY_TYPE.computeIfAbsent(clazz, c -> {
            // neo types mapped statically
            final RegistryHandle<?> mojang = findMojang(clazz);
            return mojang != null ? mojang : findCreateDynamic(clazz);
        }));
    }

    private static void mapNeo(
            final @Nullable Class<?> type,
            final ResourceKey<? extends Registry<?>> key,
            final Registry<?> registry) {
        final RegistryHandle<?> handle = new MojangRegistryHandle<>(registry);
        if (type != null) {
            REGISTRY_BY_TYPE.put(type, handle);
        }
        NEO_REGISTRIES.put(key, handle);
    }

    @Nullable
    private static RegistryHandle<?> findMojang(final Class<?> clazz) {
        for (final Registry<?> r : BuiltInRegistries.REGISTRY) {
            if (clazz.isInstance(r.iterator().next())) {
                return new MojangRegistryHandle<>(r);
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RegistryHandle<?> findCreateDynamic(final Class<?> clazz) {
        for (final Field f : clazz.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && clazz.equals(resolveRegistryType(f))) {
                try {
                    final var key = (ResourceKey) f.get(null);
                    return DynamicRegistries.getOrCreate(key);
                } catch (final ReflectiveOperationException ignored) {}
            }
        }
        return null;
    }

    @Nullable
    private static Class<?> resolveRegistryType(final Field f) {
        if (ResourceKey.class.equals(f.getType())
                && f.getGenericType() instanceof ParameterizedType p1) {
            final var p1Args = p1.getActualTypeArguments();
            if (p1Args.length > 0
                    && p1Args[0] instanceof ParameterizedType p2
                    && Registry.class.equals(p2.getRawType())) {
                final var p2Args = p2.getActualTypeArguments();
                if (p2Args.length > 0 && p2Args[0] instanceof Class<?> c) {
                    return c;
                }
            }
        }
        return null;
    }
}

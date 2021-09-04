package personthecat.catlib.mixin;

import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess.RegistryHolder;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.RegistryReadOps.ResourceAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.event.world.RegistryAccessTracker;
import personthecat.catlib.event.world.RegistrySet;

import java.util.Set;
import java.util.function.Consumer;

@Log4j2
@Mixin(RegistryReadOps.class)
public class RegistryReadOpsMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "create(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/resources/RegistryReadOps$ResourceAccess;Lnet/minecraft/core/RegistryAccess$RegistryHolder;)Lnet/minecraft/resources/RegistryReadOps;", at = @At("RETURN"))
    private static <T> void afterCreation(DynamicOps<T> ops, ResourceAccess access, RegistryHolder holder, CallbackInfoReturnable<RegistryReadOps<T>> ci) {
        if (!FeatureModificationEvent.EVENT.isEmpty()) {
            final Consumer<FeatureModificationContext> event = FeatureModificationEvent.EVENT.invoker();
            final RegistrySet registries = new RegistrySet(holder);
            final Set<ResourceLocation> modifiedBiomes = ((RegistryAccessTracker) (Object) holder).getModifiedBiomes();

            holder.registryOrThrow(Registry.BIOME_REGISTRY).forEach(biome -> {
                if (!modifiedBiomes.add(biome.getRegistryName())) {
                    return; // Don't double modify;
                }
                final BiomeGenerationSettingsBuilder builder = new BiomeGenerationSettingsBuilder(biome.getGenerationSettings());
                event.accept(new FeatureModificationContext(biome, builder, registries));

                final BiomeGenerationSettingsAccessor settingsAccessor = (BiomeGenerationSettingsAccessor) biome.getGenerationSettings();
                final BiomeGenerationSettingsBuilderAccessor builderAccessor = (BiomeGenerationSettingsBuilderAccessor) builder;
                settingsAccessor.setCarvers(builderAccessor.getCarvers());
                settingsAccessor.setFeatures(builderAccessor.getFeatures());
                settingsAccessor.setStructureStarts(builder.getStructures());
            });
        }
    }
}

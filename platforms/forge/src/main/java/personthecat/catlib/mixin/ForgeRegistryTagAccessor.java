package personthecat.catlib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(targets = "net.minecraftforge.registries.ForgeRegistryTag")
public interface ForgeRegistryTagAccessor<T> {

    @Accessor
    List<T> getContents();
}

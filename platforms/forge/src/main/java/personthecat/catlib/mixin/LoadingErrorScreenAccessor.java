package personthecat.catlib.mixin;

import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.client.gui.screen.LoadingErrorScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(LoadingErrorScreen.class)
public interface LoadingErrorScreenAccessor {

    @Accessor
    List<ModLoadingException> getModLoadErrors();
}

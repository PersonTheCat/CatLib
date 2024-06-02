package personthecat.catlib.config.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import personthecat.catlib.CatLib;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

@Environment(EnvType.CLIENT)
public class AutoModMenuCompat implements ModMenuApi {
    private static final Map<String, ConfigScreenFactory<?>> SCREEN_PROVIDERS =
        new ConcurrentHashMap<>();

    public static void registerScreen(String name, UnaryOperator<Screen> fn) {
        SCREEN_PROVIDERS.put(name, fn::apply);
    }

    public static ConfigScreenFactory<?> getScreenFactory(String name) {
        return SCREEN_PROVIDERS.get(name);
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return getScreenFactory(CatLib.ID);
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return SCREEN_PROVIDERS;
    }
}

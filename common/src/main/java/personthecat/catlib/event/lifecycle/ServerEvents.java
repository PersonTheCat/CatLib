package personthecat.catlib.event.lifecycle;

import net.minecraft.server.MinecraftServer;
import personthecat.catlib.event.LibEvent;

import java.util.function.Consumer;

public class ServerEvents {
    public static final LibEvent<Consumer<MinecraftServer>> LOAD =
        LibEvent.create(fs -> s -> fs.forEach(f -> f.accept(s)));
    public static final LibEvent<Consumer<MinecraftServer>> UNLOAD =
        LibEvent.create(fs -> s -> fs.forEach(f -> f.accept(s)));
}

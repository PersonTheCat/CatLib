package personthecat.catlib.event.lifecycle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import personthecat.catlib.event.LibEvent;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ClientTickEvent {

    public static final LibEvent<Consumer<Minecraft>> END =
        LibEvent.nonRecursive(fs -> mc -> fs.forEach(f -> f.accept(mc)));

    public static void registerSingle(final Consumer<Minecraft> f) {
        END.register(new SingleTickListener(f));
    }

    private record SingleTickListener(Consumer<Minecraft> wrapped) implements Consumer<Minecraft> {
        @Override
        public void accept(final Minecraft mc) {
            this.wrapped.accept(mc);
            END.deregister(this);
        }
    }
}

package personthecat.catlib.event.player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface PlayerConnectedCallback {
    void accept(final Player player, final MinecraftServer server);
}

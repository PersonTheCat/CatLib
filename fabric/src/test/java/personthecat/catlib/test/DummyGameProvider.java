package personthecat.catlib.test;

import lombok.extern.log4j.Log4j2;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.apache.commons.lang3.time.StopWatch;

@Log4j2
public class DummyGameProvider extends MinecraftGameProvider {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void launch(ClassLoader loader) {
        log.info("Bootstrapping default registries");
        final StopWatch sw = StopWatch.createStarted();
        try {
            TestUtils.runFromMixinEnabledClassLoader(Delegate.class, "launch");
        } catch (final Throwable e) {
            throw new IllegalStateException("Error running bootstrap", e);
        }
        log.info("Finished bootstrapping registries in {}", sw);
    }

    @Override
    public boolean isObfuscated() {
        return false;
    }

    public static class Delegate {

        @SuppressWarnings("unused") // used reflectively
        public void launch() {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
        }
    }
}

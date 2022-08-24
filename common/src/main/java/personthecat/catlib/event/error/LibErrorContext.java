package personthecat.catlib.event.error;

import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import personthecat.catlib.command.CommandUtils;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.data.collections.MultiValueHashMap;
import personthecat.catlib.data.collections.MultiValueMap;
import personthecat.catlib.exception.FormattedException;
import personthecat.catlib.exception.GenericFormattedException;
import personthecat.catlib.exception.ModLoadException;
import personthecat.catlib.util.McUtils;
import personthecat.fresult.functions.ThrowingRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Log4j2
public class LibErrorContext {

    private static final Map<ModDescriptor, List<FormattedException>> COMMON_ERRORS = new ConcurrentHashMap<>();
    private static final Map<ModDescriptor, List<FormattedException>> FATAL_ERRORS = new ConcurrentHashMap<>();
    private static final Set<ModDescriptor> ERRED_MODS = ConcurrentHashMap.newKeySet();
    private static final AtomicLong LAST_BROADCAST = new AtomicLong(0L);
    private static final long BROADCAST_INTERVAL = 3000L;

    public static void warn(final ModDescriptor mod, final FormattedException e) {
        register(Severity.WARN, mod, e);
    }

    public static void error(final ModDescriptor mod, final FormattedException e) {
        register(Severity.ERROR, mod, e);
    }

    public static void fatal(final ModDescriptor mod, final FormattedException e) {
        register(Severity.FATAL, mod, e);
    }

    public static void register(final Severity level, final ModDescriptor mod, final FormattedException e) {
        e.onErrorReceived(level, mod, log);

        if (level == Severity.FATAL) {
            FATAL_ERRORS.computeIfAbsent(mod, m -> Collections.synchronizedList(new ArrayList<>())).add(e);
        } else if (level.isAtLeast(LibConfig.errorLevel())) {
            COMMON_ERRORS.computeIfAbsent(mod, m -> Collections.synchronizedList(new ArrayList<>())).add(e);
        } else {
            log.warn("Ignoring error at level: " + level, e);
            e.onErrorIgnored(level, mod, log);
            return;
        }
        ERRED_MODS.add(mod);

        if (McUtils.isClientSide()) {
            broadcastErrors();
        }
    }

    public static boolean run(final ModDescriptor mod, final ThrowingRunnable<FormattedException> f) {
        try {
            f.run();
        } catch (final FormattedException e) {
            fatal(mod, e);
            return true;
        }
        return false;
    }

    public static <E extends Throwable> boolean apply(final ModDescriptor mod, final ThrowingRunnable<E> f) {
        return apply(mod, f, GenericFormattedException::new);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> boolean apply(
            final ModDescriptor mod, final ThrowingRunnable<E> f, final Function<E, FormattedException> e) {

        try {
            f.run();
        } catch (final Throwable t) {
            final FormattedException formatted;
            try {
                formatted = e.apply((E) t);
            } catch (final ClassCastException ignored) {
                throw new RuntimeException(t);
            }
            fatal(mod, formatted);
            return true;
        }
        return false;
    }

    public static boolean hasErrors() {
        return !COMMON_ERRORS.isEmpty() || !FATAL_ERRORS.isEmpty();
    }

    public static boolean isFatal() {
        return !FATAL_ERRORS.isEmpty();
    }

    public static int numMods() {
        return ERRED_MODS.size();
    }

    public static List<ModDescriptor> getMods() {
        return new ArrayList<>(ERRED_MODS);
    }

    public static Collection<FormattedException> get(final Class<? extends FormattedException> type) {
        final Set<FormattedException> matching = new HashSet<>();
        COMMON_ERRORS.forEach((mod, errors) -> errors.forEach(error -> {
            if (type.isInstance(error)) matching.add(error);
        }));
        FATAL_ERRORS.forEach((mod, errors) -> errors.forEach(error -> {
            if (type.isInstance(error)) matching.add(error);
        }));
        return matching;
    }

    public static Collection<FormattedException> get(final ModDescriptor m, final Class<? extends FormattedException> type) {
        final Set<FormattedException> matching = new HashSet<>();
        final List<FormattedException> common = COMMON_ERRORS.get(m);
        if (common != null) {
            common.forEach(error -> {
                if (type.isInstance(error)) matching.add(error);
            });
        }
        final List<FormattedException> fatal = FATAL_ERRORS.get(m);
        if (fatal != null) {
            fatal.forEach(error -> {
                if (type.isInstance(error)) matching.add(error);
            });
        }
        return matching;
    }

    public static void clear(final Class<? extends FormattedException> type) {
        if (type == FormattedException.class || type == GenericFormattedException.class) {
            throw new UnsupportedOperationException("Operation affects too many mods (" + type.getSimpleName() + ")");
        }
        COMMON_ERRORS.forEach((mod, errors) -> errors.removeIf(type::isInstance));
        FATAL_ERRORS.forEach((mod, errors) -> errors.removeIf(type::isInstance));
    }

    public static void clear(final ModDescriptor m, final Class<? extends FormattedException> type) {
        COMMON_ERRORS.forEach((mod, errors) -> {
            if (mod.equals(m)) errors.removeIf(type::isInstance);
        });
        FATAL_ERRORS.forEach((mod, errors) -> {
            if (mod.equals(m)) errors.removeIf(type::isInstance);
        });
    }

    public static MultiValueMap<ModDescriptor, FormattedException> getCommon() {
        final MultiValueMap<ModDescriptor, FormattedException> common = new MultiValueHashMap<>();
        common.putAll(COMMON_ERRORS);
        return common;
    }

    public static MultiValueMap<ModDescriptor, FormattedException> getFatal() {
        final MultiValueMap<ModDescriptor, FormattedException> fatal = new MultiValueHashMap<>();
        fatal.putAll(FATAL_ERRORS);
        return fatal;
    }

    @ApiStatus.Internal
    public static void outputServerErrors(final boolean notify) {
        if (hasErrors()) {
            for (final Map.Entry<ModDescriptor, List<FormattedException>> entry : COMMON_ERRORS.entrySet()) {
                log.error("Encountered {} warnings for {}", entry.getValue().size(), entry.getKey().getModId());
                entry.getValue().forEach(log::warn);
            }
            for (final Map.Entry<ModDescriptor, List<FormattedException>> entry : FATAL_ERRORS.entrySet()) {
                log.error("Encountered {} errors for {}", entry.getValue().size(), entry.getKey().getModId());
                entry.getValue().forEach(log::fatal);
            }
            if (!FATAL_ERRORS.isEmpty()) {
                throw new ModLoadException(FATAL_ERRORS.size() + " mods encountered fatal errors");
            }
            dispose();
        } else if (notify) {
            log.info("No errors in context. Server init complete!");
        }
    }

    @Environment(EnvType.CLIENT)
    private static void broadcastErrors() {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            broadcastErrors(player);
        }
    }

    @ApiStatus.Internal
    @Environment(EnvType.CLIENT)
    public static void broadcastErrors(final Player player) {
        final long currentUpdate = System.currentTimeMillis();
        final long lastUpdate = LAST_BROADCAST.get();
        if (currentUpdate - lastUpdate > BROADCAST_INTERVAL) {
            player.sendMessage(new TranslatableComponent("catlib.errorText.clickHere")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)
                .withClickEvent(CommandUtils.clickToRun("/catlib errors"))),
                    Util.NIL_UUID);
            LAST_BROADCAST.set(currentUpdate);
        }
    }

    @ApiStatus.Internal
    public static void dispose() {
        COMMON_ERRORS.clear();
        FATAL_ERRORS.clear();
        ERRED_MODS.clear();
    }
}
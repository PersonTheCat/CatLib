package personthecat.catlib.event.error;

import lombok.extern.log4j.Log4j2;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.data.MultiValueHashMap;
import personthecat.catlib.data.MultiValueMap;
import personthecat.catlib.exception.FormattedException;
import personthecat.catlib.exception.GenericFormattedException;
import personthecat.catlib.util.McUtils;
import personthecat.fresult.functions.ThrowingRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Log4j2
public class LibErrorContext {

    private static final Map<ModDescriptor, List<FormattedException>> COMMON_ERRORS = new ConcurrentHashMap<>();
    private static final Map<ModDescriptor, List<FormattedException>> FATAL_ERRORS = new ConcurrentHashMap<>();
    private static final Set<ModDescriptor> ERRED_MODS = ConcurrentHashMap.newKeySet();

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
        e.onErrorReceived(log);

        if (McUtils.isDedicatedServer()) {
            throw new RuntimeException(e);
        }
        if (level == Severity.FATAL) {
            FATAL_ERRORS.computeIfAbsent(mod, m -> Collections.synchronizedList(new ArrayList<>())).add(e);
        } else if (level.isAtLeast(LibConfig.getErrorLevel())) {
            COMMON_ERRORS.computeIfAbsent(mod, m -> Collections.synchronizedList(new ArrayList<>())).add(e);
        } else {
            log.warn("Ignoring error at level: " + level, e);
        }
        ERRED_MODS.add(mod);
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
}
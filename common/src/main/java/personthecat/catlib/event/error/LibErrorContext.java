package personthecat.catlib.event.error;

import lombok.extern.log4j.Log4j2;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.data.MultiValueHashMap;
import personthecat.catlib.data.MultiValueMap;
import personthecat.catlib.exception.FormattedException;
import personthecat.catlib.util.McUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class LibErrorContext {

    private static final Map<ModDescriptor, List<FormattedException>> COMMON_ERRORS = new ConcurrentHashMap<>();
    private static final Map<ModDescriptor, List<FormattedException>> FATAL_ERRORS = new ConcurrentHashMap<>();
    private static final Set<ModDescriptor> ERRED_MODS = ConcurrentHashMap.newKeySet();

    public static void registerSingle(final ModDescriptor mod, final FormattedException e) {
        registerSingle(Severity.ERROR, mod, e);
    }

    public static void registerSingle(final Severity level, final ModDescriptor mod, final FormattedException e) {
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
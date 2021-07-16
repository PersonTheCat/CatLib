package personthecat.buildtools.processors;

import personthecat.buildtools.CtUtils;
import personthecat.buildtools.LauncherContext;
import personthecat.buildtools.annotations.OverwriteTarget;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtType;

import java.util.ArrayList;
import java.util.List;

public class OverwriteTargetProcessor {

    public static List<CtType<?>> getOverwriteTargets(final List<CtType<?>> classes) {
        final List<CtType<?>> targets = new ArrayList<>();
        for (final CtType<?> target : classes) {
            final CtAnnotation<?> a = CtUtils.getAnnotation(target, OverwriteTarget.class);
            if (a != null && (boolean) a.getValueAsObject("required")) {
                targets.add(target);
                target.removeAnnotation(a);
            }
        }
        return targets;
    }

    public static void processModel(final CtModel model) {
        final List<CtType<?>> targets = LauncherContext.getOverwriteTargets();
        if (targets.isEmpty()) {
            return;
        }
        final List<CtType<?>> overwrites = CtUtils.getAllClasses(model);
        for (final CtType<?> target : targets) {
            if (!projectOverwritesClass(overwrites, target)) {
                throw new MissingOverwriteException(target);
            }
        }
    }

    private static boolean projectOverwritesClass(final List<CtType<?>> overwrites, final CtType<?> target) {
        for (final CtType<?> overwrite : overwrites) {
            if (overwrite.getQualifiedName().equals(target.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }

    private static class MissingOverwriteException extends IllegalStateException {
        MissingOverwriteException(final CtType<?> type) {
            super("Project does not overwrite " + type.getSimpleName());
        }
    }
}

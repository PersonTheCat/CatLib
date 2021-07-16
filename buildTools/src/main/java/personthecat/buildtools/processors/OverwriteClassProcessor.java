package personthecat.buildtools.processors;

import personthecat.buildtools.CtUtils;
import personthecat.buildtools.LauncherContext;
import personthecat.buildtools.annotations.OverwriteClass;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtType;

public class OverwriteClassProcessor extends AbstractProcessor<CtType<?>> {

    @Override
    public void process(final CtType<?> type) {
        final CtAnnotation<?> a = CtUtils.getAnnotation(type, OverwriteClass.class);
        if (a != null) {
            LauncherContext.getOverwrittenClassOrThrow(type);
            type.removeAnnotation(a);
        }
    }
}

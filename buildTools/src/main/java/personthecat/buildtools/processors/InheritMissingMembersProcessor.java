package personthecat.buildtools.processors;

import personthecat.buildtools.CtUtils;
import personthecat.buildtools.LauncherContext;
import personthecat.buildtools.annotations.InheritMissingMembers;
import personthecat.buildtools.annotations.PlatformMustInherit;
import personthecat.buildtools.annotations.PlatformMustOverwrite;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

public class InheritMissingMembersProcessor extends AbstractProcessor<CtType<?>> {

    private static final String GENERATOR_NAME = InheritMissingMembersProcessor.class.getSimpleName();

    @Override
    public void process(final CtType<?> type) {
        if (type.isTopLevel()) {
            final CtAnnotation<?> a = CtUtils.getAnnotation(type, InheritMissingMembers.class);
            if (a != null) {
                final CtType<?> overwritten = LauncherContext.getOverwrittenClassOrThrow(type);
                this.inheritMembers(type, overwritten);
                this.inheritNestedClasses(type, overwritten);
                type.removeAnnotation(a);
            }
        }
    }

    private void inheritMembers(final CtType<?> type, final CtType<?> overwritten) {
        this.inheritMethods(type, overwritten);
        this.inheritFields(type, overwritten);
        this.inheritConstructors(type, overwritten);
        this.inheritRecursive(type, overwritten);
    }

    private void inheritMethods(final CtType<?> type, final CtType<?> overwritten) {
        for (final CtMethod<?> method : overwritten.getMethods()) {
            if (!CtUtils.hasAnnotation(type, method, PlatformMustOverwrite.class)) {
                if (!CtUtils.classOverridesMethod(type, method)) {
                    final CtMethod<?> cloned = method.clone();
                    cloned.removeAnnotation(CtUtils.getAnnotation(type, method, PlatformMustInherit.class));
                    type.addMethod(CtUtils.markGenerated(cloned, GENERATOR_NAME));
                }
            }
        }
    }

    private void inheritFields(final CtType<?> type, final CtType<?> overwritten) {
        for (final CtField<?> field : overwritten.getFields()) {
            if (!CtUtils.hasAnnotation(type, field, PlatformMustOverwrite.class)) {
                if (!CtUtils.classOverridesField(type, field)) {
                    final CtField<?> cloned = field.clone();
                    cloned.removeAnnotation(CtUtils.getAnnotation(type, field, PlatformMustInherit.class));
                    type.addField(CtUtils.markGenerated(field.clone(), GENERATOR_NAME));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void inheritConstructors(final CtType<?> type, final CtType<?> overwritten) {
        for (final CtConstructor<?> constructor : CtUtils.getConstructors(overwritten)) {
            if (!CtUtils.hasAnnotation(type, constructor, PlatformMustOverwrite.class)) {
                if (!CtUtils.classOverridesConstructor(type, constructor)) {
                    final CtConstructor<?> cloned = CtUtils.markGenerated(constructor.clone(), GENERATOR_NAME);
                    cloned.removeAnnotation(CtUtils.getAnnotation(type, constructor, PlatformMustInherit.class));
                    ((CtClass<Object>) type).addConstructor((CtConstructor<Object>) cloned);
                }
            }
        }
    }

    private void inheritRecursive(final CtType<?> type, final CtType<?> overwritten) {
        for (final CtType<?> nested : type.getNestedTypes()) {
            final CtType<?> replaced = overwritten.getNestedType(nested.getSimpleName());
            if (replaced != null) {
                this.inheritMembers(nested, replaced);
            }
        }
    }

    private void inheritNestedClasses(final CtType<?> type, final CtType<?> overwritten) {
        for (final CtType<?> nested : overwritten.getNestedTypes()) {
            final CtType<?> replacement = type.getNestedType(nested.getSimpleName());
            if (replacement == null) {
                type.addNestedType(nested.clone());
            } else {
                inheritNestedClasses(replacement, nested);
            }
        }
    }
}

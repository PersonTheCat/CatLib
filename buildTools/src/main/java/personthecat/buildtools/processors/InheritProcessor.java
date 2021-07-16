package personthecat.buildtools.processors;

import personthecat.buildtools.CtUtils;
import personthecat.buildtools.LauncherContext;
import personthecat.buildtools.annotations.Inherit;
import personthecat.buildtools.annotations.PlatformMustInherit;
import personthecat.buildtools.annotations.PlatformMustOverwrite;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

import javax.annotation.Nonnull;
import java.util.Objects;

public class InheritProcessor extends AbstractProcessor<CtType<?>> {

    private static final String GENERATOR_NAME = InheritProcessor.class.getSimpleName();

    @Override
    public void process(final CtType<?> type) {
        if (CtUtils.anyMemberIsAnnotated(type, Inherit.class)) {
            this.processMembers(type, LauncherContext.getOverwrittenClassOrThrow(type));
        }
    }

    private void processMembers(final CtType<?> type, final CtType<?> overwritten) {
        this.processMethods(type, overwritten);
        this.processFields(type, overwritten);
        this.processConstructors(type, overwritten);
        this.processRecursive(type, overwritten);
    }

    private void processMethods(final CtType<?> type, final CtType<?> overwritten) {
        for (final CtMethod<?> method : type.getMethods()) {
            final CtAnnotation<?> a = CtUtils.getAnnotation(type, method, Inherit.class);
            final CtMethod<?> inherited = CtUtils.getOverriddenMethod(overwritten, method);
            if (a != null) {
                method.setBody(this.validateInherited(type, inherited).getBody());
                CtUtils.markGenerated(method, GENERATOR_NAME);
                method.removeAnnotation(a);
            } else if (inherited != null) {
                this.validateOverwritten(type, inherited);
            }
        }
    }

    private void processFields(final CtType<?> type, final CtType<?> overwritten) {
        for (final CtField<?> field : type.getFields()) {
            final CtAnnotation<?> a = CtUtils.getAnnotation(type, field, Inherit.class);
            final CtField<?> inherited = overwritten.getField(field.getSimpleName());
            if (a != null) {
                final CtField<?> cloned = this.validateInherited(type, inherited).clone();
                cloned.setAnnotations(field.getAnnotations());
                cloned.removeAnnotation(a);
                type.removeField(field);
                type.addField(CtUtils.markGenerated(cloned, GENERATOR_NAME));
            } else if (inherited != null) {
                this.validateOverwritten(type, inherited);
            }
        }
    }

    private void processConstructors(final CtType<?> type, final CtType<?> overwritten) {
        for (final CtConstructor<?> constructor : CtUtils.getConstructors(type)) {
            final CtAnnotation<?> a = CtUtils.getAnnotation(type, constructor, Inherit.class);
            final CtConstructor<?> inherited = CtUtils.getOverriddenConstructor(overwritten, constructor);
            if (a != null) {
                constructor.setBody(this.validateInherited(type, inherited).getBody());
                CtUtils.markGenerated(constructor, GENERATOR_NAME);
                constructor.removeAnnotation(a);
            } else if (inherited != null) {
                this.validateOverwritten(type, inherited);
            }
        }
    }

    private void processRecursive(final CtType<?> type, final CtType<?> overwritten) {
        for (final CtType<?> nested : type.getNestedTypes()) {
            final CtType<?> replaced = overwritten.getNestedType(nested.getSimpleName());
            if (replaced != null) {
                this.processMembers(nested, replaced);
            }
        }
    }

    @Nonnull
    private <T extends CtTypeMember> T validateInherited(final CtType<?> type, final T member) {
        Objects.requireNonNull(member, "No overwrite target for member: " + type.getSimpleName());
        if (CtUtils.hasAnnotation(type, member, PlatformMustOverwrite.class)) {
            throw new IllegalOverwriteException(type, member);
        }
        return member;
    }

    private void validateOverwritten(final CtType<?> type, final CtTypeMember member) {
        if (CtUtils.hasAnnotation(type, member, PlatformMustInherit.class)) {
            throw new MissingInheritException(type, member);
        }
    }

    private static class IllegalOverwriteException extends IllegalStateException {
        IllegalOverwriteException(final CtType<?> type, final CtTypeMember member) {
            super("Member cannot be inherited: " + CtUtils.formatMember(type.getSimpleName(), member));
        }
    }

    private static class MissingInheritException extends IllegalStateException {
        MissingInheritException(final CtType<?> type, final CtTypeMember member) {
            super("Member must be inherited: " + CtUtils.formatMember(type.getSimpleName(), member));
        }
    }
}

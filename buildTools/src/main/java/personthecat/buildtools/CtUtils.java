package personthecat.buildtools;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import javax.annotation.Generated;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class CtUtils {

    private static final Factory FACTORY = new Launcher().getFactory();
    private static final String STRING_TYPE = "java.lang.String";

    private CtUtils() {}

    public static List<CtType<?>> getAllClasses(final CtModel model) {
        final List<CtType<?>> classes = new ArrayList<>();
        getClassesRecursive(model.getRootPackage(), classes);
        return classes;
    }

    private static void getClassesRecursive(final CtPackage ctPackage, final List<CtType<?>> classes) {
        for (final CtPackage pack : ctPackage.getPackages()) {
            getClassesRecursive(pack, classes);
        }
        classes.addAll(ctPackage.getTypes());
    }

    public static <T> Set<CtConstructor<T>> getConstructors(final CtType<T> type) {
        if (type instanceof CtClass) {
            return ((CtClass<T>) type).getConstructors();
        }
        return Collections.emptySet();
    }

    public static List<CtTypeMember> getPublicMembers(final CtType<?> ctClass) {
        final List<CtTypeMember> members = new ArrayList<>();
        for (final CtMethod<?> method : ctClass.getMethods()) {
            if (!method.isPrivate()) {
                members.add(method);
            }
        }
        for (final CtField<?> field : ctClass.getFields()) {
            if (!field.isPrivate() && !isConstant(field)) {
                members.add(field);
            }
        }
        for (final CtType<?> nested : ctClass.getNestedTypes()) {
            members.addAll(getPublicMembers(nested));
        }
        return members;
    }

    public static List<CtField<?>> getPublicConstants(final CtType<?> ctClass) {
        final List<CtField<?>> constants = new ArrayList<>();
        for (CtField<?> field : ctClass.getFields()) {
            if (!field.isPrivate() && isConstant(field)) {
                constants.add(field);
            }
        }
        return constants;
    }

    public static boolean isConstant(final CtField<?> field) {
        return field.getType().isPrimitive() || STRING_TYPE.equals(field.getType().getQualifiedName());
    }

    @Nullable
    public static CtMethod<?> getOverriddenMethod(final CtType<?> overwritten, final CtMethod<?> child) {
        for (final CtMethod<?> parent : overwritten.getMethodsByName(child.getSimpleName())) {
            if (canOverrideMethod(child, parent)) {
                return parent;
            }
        }
        return null;
    }

    @Nullable
    public static CtConstructor<?> getOverriddenConstructor(final CtType<?> overwritten, final CtConstructor<?> child) {
        for (final CtConstructor<?> parent : getConstructors(overwritten)) {
            if (canOverrideExecutable(child, parent)) {
                return parent;
            }
        }
        return null;
    }

    public static boolean classOverridesMember(final CtType<?> type, final CtTypeMember parent) {
        if (parent instanceof CtMethod<?>) {
            return classOverridesMethod(type, (CtMethod<?>) parent);
        } else if (parent instanceof CtField<?>) {
            return classOverridesField(type, (CtField<?>) parent);
        } else if (parent instanceof CtConstructor<?>) {
            return classOverridesConstructor(type, (CtConstructor<?>) parent);
        } else {
            throw new UnsupportedOperationException("Must be a field or method");
        }
    }

    public static boolean classOverridesMethod(final CtType<?> type, final CtMethod<?> parent) {
        for (final CtMethod<?> child : type.getMethods()) {
            if (canOverrideMethod(child, parent)) {
                return true;
            }
        }
        return false;
    }

    public static boolean classOverridesField(final CtType<?> type, final CtField<?> parent) {
        for (final CtField<?> child : type.getFields()) {
            if (canOverrideField(child, parent)) {
                return true;
            }
        }
        return false;
    }

    public static boolean classOverridesConstructor(final CtType<?> type, final CtConstructor<?> parent) {
        for (final CtConstructor<?> child : getConstructors(type)) {
            if (canOverrideExecutable(child, parent)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canOverrideMethod(final CtMethod<?> child, final CtMethod<?> parent) {
        if (!child.getSimpleName().equals(parent.getSimpleName())) {
            return false;
        }
        return canOverrideExecutable(child, parent);
    }

    public static boolean canOverrideField(final CtField<?> child, final CtField<?> parent) {
        if (!child.getSimpleName().equals(parent.getSimpleName())) {
            return false;
        }
        return isAssignableTo(child.getType(), parent.getType());
    }

    public static boolean canOverrideExecutable(final CtExecutable<?> child, final CtExecutable<?> parent) {
        final List<CtParameter<?>> childParams = child.getParameters();
        final List<CtParameter<?>> parentParams = parent.getParameters();
        if (childParams.size() != parentParams.size()) {
            return false;
        }
        for (int i = 0; i < childParams.size(); i++) {
            if (!isAssignableTo(childParams.get(i).getType(), parentParams.get(i).getType())) {
                return false;
            }
        }
        return isAssignableTo(child.getType(), parent.getType());
    }

    public static boolean isAssignableTo(final CtTypeReference<?> child, final CtTypeReference<?> parent) {
        return child.isSubtypeOf(parent);
    }

    @Nullable
    public static <A extends Annotation> CtAnnotation<?> getAnnotation(final CtType<?> type, final Class<A> a) {
        return getAnnotation(type, type, a);
    }

    /**
     * Retrieves an annotation (if available) when given an element and declaring type.
     * <p>
     *   This method resolves a fatal issue where types are unresolvable with collapsed
     *   imports. We achieve this by assuming the types are equivalent if one is declared
     *   in the current package (according to Spoon).
     * </p>
     * @param type The declaring type which contains this element.
     * @param e The element to get the annotation from.
     * @param a The class of the annotation be researched.
     * @param <A> The type of annotation being researched.
     * @return The actual annotation if present, or else <code>null</code>.
     */
    @Nullable
    public static <A extends Annotation> CtAnnotation<?> getAnnotation(final CtType<?> type, final CtElement e, final Class<A> a) {
        final CtTypeReference<A> normal = FACTORY.createCtTypeReference(a);
        final CtAnnotation<A> annotation = e.getAnnotation(normal);
        if (annotation != null) {
            return annotation;
        }
        final CtTypeReference<A> fromPack = normal.setPackage(type.getPackage().getReference());
        return e.getAnnotation(fromPack);
    }

    public static <A extends Annotation> boolean hasAnnotation(final CtType<?> type, final CtElement e, final Class<A> a) {
        return getAnnotation(type, e, a) != null;
    }

    public static boolean anyMemberIsAnnotated(final CtType<?> type, final Class<? extends Annotation> annotation) {
        for (final CtTypeMember member : type.getTypeMembers()) {
            if (hasAnnotation(type, member, annotation)) {
                return true;
            }
            if (member instanceof CtType<?>) {
                if (anyMemberIsAnnotated((CtType<?>) member, annotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static <C> CtTypeReference<C> createCtType(final Class<C> clazz) {
        return FACTORY.createCtTypeReference(clazz);
    }

    public static <T extends CtElement> T markGenerated(final T element, final String by) {
        return element.addAnnotation(createGeneratedMarker(by));
    }

    public static CtAnnotation<Generated> createGeneratedMarker(final String by) {
        return FACTORY.createAnnotation(createCtType(Generated.class)).addValue("value", by);
    }

    public static String formatMember(final String prefix, final CtTypeMember member)  {
        if (member instanceof CtField<?>) {
            return formatField(prefix, (CtField<?>) member);
        } else if (member instanceof CtMethod<?>) {
            return formatMethod(prefix, (CtMethod<?>) member);
        } else {
            throw new UnsupportedOperationException("Must be a field or method");
        }
    }

    public static String formatField(final String prefix, final CtField<?> field) {
        final String type = field.getType().toStringDebug();
        return prefix + '#' + field.getSimpleName() + " -> " + type;
    }

    public static String formatMethod(final String prefix, final CtMethod<?> method) {
        final StringBuilder sb = new StringBuilder(prefix)
            .append('#')
            .append(method.getSimpleName())
            .append('(');
        for (final CtParameter<?> param : method.getParameters()) {
            sb.append(',').append(param.getType().toStringDebug());
        }
        sb.append(") -> ").append(method.getType().toStringDebug());
        return sb.toString();
    }
}

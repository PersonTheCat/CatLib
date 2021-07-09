package personthecat.buildtools;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.*;
import spoon.support.compiler.FileSystemFolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class OverwriteValidator implements Plugin<Project> {

    /** Stores the AST of the common project. Avoids redundant parsing. */
    private static final AtomicReference<Cache> DATA = new AtomicReference<>();

    /** A property defined in the root project pointing to the common code. */
    private static final String COMMON_PROJECT_KEY = "common";

    /** The qualified name of regular Java strings. */
    private static final String STRING_TYPE = "java.lang.String";

    /** The header message for missing overrides. */
    private static final String MISSING_MEMBERS = "Missing members";

    /** The header message for unreachable overrides. */
    private static final String UNREACHABLE_MEMBERS = "Unreachable members";

    /** The header messages for classes marked as @OverwriteClass with no valid targets. */
    private static final String NO_TARGETS_FOUND = "No targets found";

    @Override
    public void apply(@Nonnull Project project) {
        final File javaDir = this.getJavaDirectory(project);
        if (!javaDir.exists()) {
            System.out.println("No source directory in " + project.getName() + ". Skipping.");
            return;
        }
        if (project.equals(this.getCommonProject(project))) {
            return;
        }
        this.initStatic(project);
        final ValidationContext ctx = this.validateAll(this.getAllClasses(this.getModel(javaDir)));
        if (!ctx.missing.isEmpty()) {
            System.err.println(this.createRedText(this.formatInvalidMembers(project, MISSING_MEMBERS, ctx.missing)));
        }
        if (!ctx.unreachable.isEmpty()) {
            System.err.println(this.createRedText(this.formatInvalidMembers(project, UNREACHABLE_MEMBERS, ctx.unreachable)));
        }
        if (!ctx.invalid.isEmpty()) {
            System.err.println(this.createRedText(this.formatNoTargetFound(project, ctx.invalid)));
        }
        if (ctx.anyErrors()) {
            throw new InvalidOverwritesException(project);
        }
    }

    private File getJavaDirectory(Project project) {
        return new File(project.getProjectDir(), "src/main/java");
    }

    private Project getCommonProject(Project project) {
        final Object value = project.findProperty(COMMON_PROJECT_KEY);
        if (value == null) {
            throw new MissingCommonProjectException();
        }
        if (!(value instanceof String)) {
            throw new InvalidCommonProjectException();
        }
        return project.project((String) value);
    }

    private synchronized void initStatic(Project project) {
        if (DATA.get() == null) {
            final CtModel model = getModel(getCommonProject(project));
            DATA.set(new Cache(model, getAllClasses(model)));
        }
    }

    private CtModel getModel(Project project) {
        final File javaDir = this.getJavaDirectory(project);
        if (!javaDir.exists()) {
            throw new MissingCommonJavaDirectoryException();
        }
        return this.getModel(javaDir);
    }

    private CtModel getModel(File javaDir) {
        final Launcher launcher = new Launcher();
        launcher.addInputResource(new FileSystemFolder(javaDir));
        return launcher.buildModel();
    }

    private ValidationContext validateAll(List<CtType<?>> classes) {
        final ValidationContext ctx = new ValidationContext();
        for (CtType<?> ctClass : classes) {
            final CtType<?> overwritten = this.getOverwrittenClass(ctClass);
            if (overwritten != null) {
                this.putMissingOverrides(ctx.missing, ctClass, overwritten);
                this.putUnreachableConstants(ctx.unreachable, ctClass, overwritten);
            } else if (ctClass.hasAnnotation(OverwriteClass.class)) {
                ctx.invalid.add(ctClass.getQualifiedName());
            }
        }
        return ctx;
    }

    private void putMissingOverrides(Map<String, List<String>> missing, CtType<?> ctClass, CtType<?> overwritten) {
        final List<String> sourceMembers = this.getPublicMembers(overwritten);
        final List<String> destMembers = this.getPublicMembers(ctClass);
        final List<String> missingInClass = new ArrayList<>();
        for (String member : sourceMembers) {
            if (!destMembers.contains(member)) {
                missingInClass.add(member);
            }
        }
        if (!missingInClass.isEmpty()) {
            missing.put(ctClass.getQualifiedName(), missingInClass);
        }
    }

    private void putUnreachableConstants(Map<String, List<String>> unreachable, CtType<?> ctClass, CtType<?> overwritten) {
        final List<CtField<?>> sourceFields = this.getPublicConstants(ctClass);
        final List<CtField<?>> destFields = this.getPublicConstants(overwritten);
        final List<String> unreachableInClass = new ArrayList<>();
        final String relativeName = this.getRelativeName(ctClass);
        for (CtField<?> dest : destFields) {
            final String path = this.formatField(relativeName, dest);
            for (CtField<?> source : sourceFields) {
                if (path.equals(this.formatField(relativeName, source))) {
                    if (!dest.getAssignment().equals(source.getAssignment())) {
                        unreachableInClass.add(path);
                    }
                }
            }
        }
        if (!unreachableInClass.isEmpty()) {
            unreachable.put(ctClass.getQualifiedName(), unreachableInClass);
        }
    }

    private boolean isConstant(CtField<?> field) {
        return field.getType().isPrimitive() || STRING_TYPE.equals(field.getType().getQualifiedName());
    }

    private List<CtType<?>> getCommonClasses() {
        return getCache().classes;
    }

    private Cache getCache() {
        return Objects.requireNonNull(DATA.get(), "Uninitialized");
    }

    @Nullable
    private CtType<?> getOverwrittenClass(CtType<?> ctClass) {
        for (CtType<?> t : this.getCommonClasses()) {
            if (t.getQualifiedName().equals(ctClass.getQualifiedName())) {
                return t;
            }
        }
        return null;
    }

    private List<CtType<?>> getAllClasses(CtModel model) {
        final List<CtType<?>> classes = new ArrayList<>();
        getClassesRecursive(model.getRootPackage(), classes);
        return classes;
    }

    private void getClassesRecursive(CtPackage ctPackage, List<CtType<?>> classes) {
        for (CtPackage pack : ctPackage.getPackages()) {
            getClassesRecursive(pack, classes);
        }
        classes.addAll(ctPackage.getTypes());
    }

    private List<String> getPublicMembers(CtType<?> ctClass) {
        final List<String> members = new ArrayList<>();
        final String relativeName = this.getRelativeName(ctClass);
        for (CtMethod<?> method : ctClass.getMethods()) {
            if (!method.isPrivate()) {
                members.add(this.formatMethod(relativeName, method));
            }
        }
        for (CtField<?> field : ctClass.getFields()) {
            if (!field.isPrivate() && !this.isConstant(field)) {
                members.add(this.formatField(relativeName, field));
            }
        }
        for (CtType<?> nested : ctClass.getNestedTypes()) {
            members.addAll(this.getPublicMembers(nested));
        }
        return members;
    }

    private List<CtField<?>> getPublicConstants(CtType<?> ctClass) {
        final List<CtField<?>> constants = new ArrayList<>();
        for (CtField<?> field : ctClass.getFields()) {
            if (!field.isPrivate() && this.isConstant(field)) {
                constants.add(field);
            }
        }
        return constants;
    }

    private String getRelativeName(CtType<?> ctClass) {
        return ctClass.getQualifiedName().substring(ctClass.getPackage().getQualifiedName().length() + 1);
    }

    private String formatInvalidMembers(Project project, String head, Map<String, List<String>> missing) {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : missing.entrySet()) {
            sb.append(this.buildInvalidMemberMessage(project, head, entry.getKey(), entry.getValue()));
        }
        return sb.substring(0, sb.length() - 1);
    }

    private String buildInvalidMemberMessage(Project project, String head, String className, List<String> invalid) {
        final StringBuilder msg = new StringBuilder("\n")
            .append(head)
            .append(" in '")
            .append(project.getName())
            .append("' @ ")
            .append(className)
            .append('\n');
        for (String member : invalid) {
            msg.append(" * ").append(member).append('\n');
        }
        return msg.toString();
    }

    private String formatNoTargetFound(Project project, List<String> invalid) {
        final StringBuilder msg = new StringBuilder("\n")
            .append(NO_TARGETS_FOUND)
            .append(" in '")
            .append(project.getName())
            .append("' for the following classes:\n");
        for (String path : invalid) {
            msg.append(" * ").append(path).append('\n');
        }
        return msg.substring(0, msg.length() - 1);
    }

    private String formatMethod(String prefix, CtMethod<?> method) {
        final StringBuilder sb = new StringBuilder(prefix)
            .append('#')
            .append(method.getSimpleName())
            .append('(');
        for (CtParameter<?> param : method.getParameters()) {
            sb.append(',').append(param.getType().getSimpleName());
        }
        sb.append(") -> ").append(method.getType().toString());
        return sb.toString();
    }

    private String formatField(String prefix, CtField<?> field) {
        final String type = field.getType().toString();
        return prefix + '#' + field.getSimpleName() + " -> " + type;
    }

    private String createRedText(String text) {
        return "\u001B[31m" + text + "\u001B[0m";
    }

    private static class Cache {
        final CtModel model;
        final List<CtType<?>> classes;

        Cache(CtModel model, List<CtType<?>> classes) {
            this.model = model;
            this.classes = classes;
        }
    }

    private static class MemberMap extends HashMap<String, List<String>> {}

    private static class PathList extends ArrayList<String> {}

    private static class ValidationContext {
        final MemberMap missing = new MemberMap();
        final MemberMap unreachable = new MemberMap();
        final PathList invalid = new PathList();

        boolean anyErrors() {
            return !(missing.isEmpty() && unreachable.isEmpty() && invalid.isEmpty());
        }
    }

    private static class MissingCommonProjectException extends IllegalStateException {
        MissingCommonProjectException() {
            super("You must define '" + COMMON_PROJECT_KEY + "' in your root build.gradle");
        }
    }

    private static class InvalidCommonProjectException extends IllegalArgumentException {
        InvalidCommonProjectException() {
            super("'" + COMMON_PROJECT_KEY + "' must of type " + String.class.getName());
        }
    }

    private static class MissingCommonJavaDirectoryException extends IllegalStateException {
        MissingCommonJavaDirectoryException() {
            super("Common project does not have a source directory");
        }
    }

    private static class InvalidOverwritesException extends IllegalStateException {
        InvalidOverwritesException(Project project) {
            super("Project '" + project.getName() + "' contains invalid overwrites");
        }
    }
}

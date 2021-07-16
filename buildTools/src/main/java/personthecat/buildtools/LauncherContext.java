package personthecat.buildtools;

import org.gradle.api.Project;
import personthecat.buildtools.processors.*;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.FileSystemFolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class LauncherContext {

    /** Stores the AST of the common project. Avoids redundant parsing. */
    private static final AtomicReference<Cache> DATA = new AtomicReference<>();

    /** A property defined in the root project pointing to the common code. */
    private static final String COMMON_PROJECT_KEY = "common";

    /** A message to display when the cache has not loaded in time. */
    private static final String OUT_OF_ORDER = "Plugins ran out of order";

    private LauncherContext() {}

    public static synchronized void initStatic(final Project project) {
        if (DATA.get() == null) {
            DATA.set(new Cache(createCommonModel(getCommonProject(project))));
        }
    }

    @Nullable
    public static CtType<?> getOverwrittenClass(final CtType<?> ctClass) {
        for (final CtType<?> t : LauncherContext.getCommonClasses()) {
            if (t.getQualifiedName().equals(ctClass.getQualifiedName())) {
                return t;
            }
        }
        return null;
    }

    @Nonnull
    public static CtType<?> getOverwrittenClassOrThrow(final CtType<?> type) {
        final CtType<?> overwritten = getOverwrittenClass(type);
        if (overwritten == null) {
            throw new MissingCommonClassException(type);
        }
        return overwritten;
    }

    @Nonnull
    public static List<CtType<?>> getOverwriteTargets() {
        return Objects.requireNonNull(DATA.get().overwriteTargets, OUT_OF_ORDER);
    }

    @Nonnull
    private static List<CtType<?>> getCommonClasses() {
        return Objects.requireNonNull(DATA.get().classes, OUT_OF_ORDER);
    }

    @Nonnull
    public static File getJavaDirectory(final Project project) {
        return new File(project.getProjectDir(), "src/main/java");
    }

    @Nonnull
    private static File getGeneratedSources(final Project project) {
        return new File(project.getBuildDir(), "generated");
    }

    @Nonnull
    public static Project getCommonProject(final Project project) {
        final Object value = project.findProperty(COMMON_PROJECT_KEY);
        if (value == null) {
            throw new MissingCommonProjectException();
        }
        if (!(value instanceof String)) {
            throw new InvalidCommonProjectException();
        }
        return project.project((String) value);
    }

    public static void process(final Project project) {
        final Launcher launcher = new Launcher();
        launcher.addInputResource(new FileSystemFolder(getJavaDirectory(project)));
        launcher.setSourceOutputDirectory(getGeneratedSources(project));
        launcher.getEnvironment().setAutoImports(true);
        launcher.addProcessor(new InheritMissingMembersProcessor());
        launcher.addProcessor(new InheritProcessor());
        launcher.addProcessor(new OverwriteClassProcessor());
        launcher.addProcessor(new OverwriteProcessor());
        launcher.run();

        final CtModel model = launcher.getModel();
        OverwriteTargetProcessor.processModel(model);
        MissingOverwriteProcessor.processModel(project, model);
        ManualImportProcessor.fixImports(project, launcher);
    }

    @Nonnull
    private static CtModel createCommonModel(final Project project) {
        final Launcher launcher = new Launcher();
        final File javaDir = getJavaDirectory(project);
        if (!javaDir.exists()) {
            throw new MissingJavaDirectoryException(project);
        }
        launcher.addInputResource(new FileSystemFolder(javaDir));
        return launcher.buildModel();
    }

    public static class Cache {
        final CtModel model;
        final List<CtType<?>> classes;
        final List<CtType<?>> overwriteTargets;

        Cache(final CtModel model) {
            this.model = model;
            this.classes = CtUtils.getAllClasses(model);
            this.overwriteTargets = OverwriteTargetProcessor.getOverwriteTargets(this.classes);
        }
    }

    private static class MissingCommonProjectException extends IllegalStateException {
        MissingCommonProjectException() {
            super("You must define '" + COMMON_PROJECT_KEY + "' in your root build.gradle");
        }
    }

    private static class InvalidCommonProjectException extends IllegalArgumentException {
        InvalidCommonProjectException() {
            super("'" + COMMON_PROJECT_KEY + "' should be a project path (e.g. :common)");
        }
    }

    private static class MissingJavaDirectoryException extends IllegalStateException {
        MissingJavaDirectoryException(final Project project) {
            super("Project " + project.getName() + " does not have a source directory");
        }
    }

    private static class MissingCommonClassException extends IllegalStateException {
        MissingCommonClassException(final CtType<?> type) {
            super("Class " + type.getSimpleName() + " has nothing to inherit");
        }
    }
}

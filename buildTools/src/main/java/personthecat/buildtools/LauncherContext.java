package personthecat.buildtools;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import personthecat.buildtools.processors.*;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.FileSystemFolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class LauncherContext {

    /** Stores the AST of the common project. Avoids redundant parsing. */
    private static final AtomicReference<Cache> DATA = new AtomicReference<>();

    /** A message to display when the cache has not loaded in time. */
    private static final String OUT_OF_ORDER = "Plugins ran out of order";

    private LauncherContext() {}

    public static synchronized void initStatic(final Project project) {
        if (DATA.get() == null) {
            final OverwriteValidatorExtension config = OverwriteValidatorExtension.get(project);
            DATA.set(new Cache(createCommonModel(config.getCommonProject())));
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

    public static Set<File> getMainSourceSet(final Project project) {
        try {
            final JavaPluginConvention javaPlugin = project.getConvention().getPlugin(JavaPluginConvention.class);
            final Set<File> sources = javaPlugin.getSourceSets().getAt("main").getAllJava().getSrcDirs();
            return validateOrEmpty(sources);
        } catch (IllegalStateException ignored) {
            return Collections.emptySet();
        }
    }

    private static Set<File> validateOrEmpty(final Set<File> sources) {
        for (final File source : sources) {
            if (!source.exists()) {
                return Collections.emptySet();
            }
        }
        return sources;
    }

    public static void process(final Project project) {
        final Launcher launcher = new Launcher();
        for (final File dir : getMainSourceSet(project)) {
            launcher.addInputResource(new FileSystemFolder(dir));
        }
        final OverwriteValidatorExtension config = OverwriteValidatorExtension.get(project);
        launcher.setSourceOutputDirectory(config.getOutputDirectory());
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
        for (final File dir : getMainSourceSet(project)) {
            launcher.addInputResource(new FileSystemFolder(dir));
        }
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

    private static class MissingCommonClassException extends IllegalStateException {
        MissingCommonClassException(final CtType<?> type) {
            super("Class " + type.getSimpleName() + " has nothing to inherit");
        }
    }
}

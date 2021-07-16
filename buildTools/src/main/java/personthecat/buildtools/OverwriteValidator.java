package personthecat.buildtools;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import javax.annotation.Nonnull;
import java.io.File;
import java.util.Set;

public class OverwriteValidator implements Plugin<Project> {

    @Override
    public void apply(@Nonnull final Project project) {
        final Set<File> sources = LauncherContext.getMainSourceSet(project);
        if (sources.isEmpty()) {
            System.out.println("No source directories " + project.getName() + ". Skipping.");
            return;
        }
        OverwriteValidatorExtension.create(project);
        getCompileJava(project).doFirst(t -> runLauncher(project, t));
    }


    private static Task getCompileJava(final Project project) {
        for (final Task task : project.getTasks()) {
            if ("compileJava".equals(task.getName())) {
                return task;
            }
        }
        throw new NullPointerException("No compileJava task in project");
    }

    private static void runLauncher(final Project project, final Task compileJava) {
        final OverwriteValidatorExtension config = OverwriteValidatorExtension.get(project);
        if (!project.equals(config.getCommonProject())) {
            LauncherContext.initStatic(project);
            LauncherContext.process(project);
            compileJava.setProperty("source", config.getOutputDirectory());
        }
    }
}

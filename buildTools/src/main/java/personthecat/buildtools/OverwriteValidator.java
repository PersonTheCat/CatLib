package personthecat.buildtools;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import javax.annotation.Nonnull;
import java.io.File;

public class OverwriteValidator implements Plugin<Project> {

    @Override
    public void apply(@Nonnull final Project project) {
        final File javaDir = LauncherContext.getJavaDirectory(project);
        if (!javaDir.exists()) {
            System.out.println("No source directory in " + project.getName() + ". Skipping.");
            return;
        }
        if (project.equals(LauncherContext.getCommonProject(project))) {
            return;
        }
        LauncherContext.initStatic(project);
        LauncherContext.process(project);
        overrideSourceDir(project);
    }

    private static void overrideSourceDir(final Project project) {
        final Task compileJava = getCompileJava(project);
        compileJava.setProperty("source", new File(project.getBuildDir(), "generated"));
    }

    private static Task getCompileJava(final Project project) {
        for (final Task task : project.getTasks()) {
            if ("compileJava".equals(task.getName())) {
                return task;
            }
        }
        throw new NullPointerException("No compileJava task in project");
    }
}

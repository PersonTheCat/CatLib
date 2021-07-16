package personthecat.buildtools;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

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
    }
}

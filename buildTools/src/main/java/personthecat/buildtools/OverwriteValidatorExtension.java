package personthecat.buildtools;

import org.gradle.api.Project;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Objects;

@SuppressWarnings("unused") // Used by Gradle
public class OverwriteValidatorExtension {

    public static final String EXTENSION_NAME = "overwriteValidator";

    private final Project project;
    public File outputDirectory;
    public Project commonProject;

    public OverwriteValidatorExtension(final Project project) {
        this.project = project;
        this.outputDirectory = new File(project.getBuildDir(), "generated");
        this.commonProject = project.findProject(":common");
    }

    public OverwriteValidatorExtension(final OverwriteValidatorExtension source) {
        this.project = source.project;
        this.outputDirectory = source.outputDirectory;
        this.commonProject = source.commonProject;
    }

    public static void create(final Project project) {
        final OverwriteValidatorExtension extension = new OverwriteValidatorExtension(project);
        project.getExtensions().create(EXTENSION_NAME, extension.getClass(), extension);
    }

    public static OverwriteValidatorExtension get(final Project project) {
        return project.getExtensions().getByType(OverwriteValidatorExtension.class);
    }

    @Nonnull
    public File getOutputDirectory() {
        return this.outputDirectory;
    }

    public void outputDirectory(final Object outputDirectory) {
        Objects.requireNonNull(outputDirectory, "Output directory may not be null");
        this.outputDirectory = this.project.file(outputDirectory);
    }

    @Nonnull
    public Project getCommonProject() {
        Objects.requireNonNull(this.commonProject, "No common project defined for " + project.getName());
        return this.commonProject;
    }

    public void commonProject(final Object commonProject) {
        Objects.requireNonNull(commonProject, "Common project may not be null");
        if (commonProject instanceof String) {
            this.commonProject = this.project.project((String) commonProject);
        } else if (commonProject instanceof Project) {
            this.commonProject = (Project) commonProject;
        }
    }
}

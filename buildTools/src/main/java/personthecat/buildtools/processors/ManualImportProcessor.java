package personthecat.buildtools.processors;

import org.gradle.api.Project;
import personthecat.buildtools.CtUtils;
import personthecat.buildtools.LauncherContext;
import spoon.Launcher;
import spoon.reflect.declaration.CtType;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManualImportProcessor {

    /** Representing all possible imports in a Java source file.. */
    private static final Pattern ALL_IMPORT_PATTERN = Pattern.compile("^\\s*import.*$");

    /** Representing a collapsed import in a Java source file. */
    private static final Pattern COLLAPSED_IMPORT_PATTERN = Pattern.compile("^\\s*import.*\\*.*$");

    /** Representing a static import in a Java source file. */
    private static final Pattern STATIC_IMPORT_PATTERN = Pattern.compile("^\\s*import.*\\s+static\\s+.*$");

    /** Representing the nested type imports which are broken by Spoon. */
    private static final Pattern BROKEN_IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+([A-Z].*);.*$");

    /** Representing the package declaration at the top of the file. */
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package.*$");

    /**
     * This method is responsible for correcting a series of import-related errors
     * that will occur when serializing files through Spoon.
     * <p>
     *   Namely, collapsed imports, static imports, and nested type imports, which
     *   are all misprinted by the library.
     * </p>
     *
     * @param project The current project which the plugin has been applied to.
     * @param launcher The context storing the parsed AST of this project.
     */
    public static void fixImports(final Project project, final Launcher launcher) {
        final Set<File> javaSources = LauncherContext.getMainSourceSet(project);
        final File generatedSources = launcher.getEnvironment().getSourceOutputDirectory();
        for (final CtType<?> type : CtUtils.getAllClasses(launcher.getModel())) {
            final CtType<?> overwritten = LauncherContext.getOverwrittenClass(type);
            if (overwritten != null) {
                final String path = getRelativePath(javaSources, type.getPosition().getFile());
                fixClassFile(new File(generatedSources, path), overwritten.getPosition().getFile());
            }
        }
    }

    private static String getRelativePath(final Set<File> sources, final File f) {
        final String filePath = f.getPath();
        for (final File source : sources) {
            final String sourcePath = source.getPath();
            if (filePath.startsWith(sourcePath)) {
                return filePath.substring(sourcePath.length());
            }
        }
        throw new IllegalStateException("No matching source: " + filePath);
    }

    private static void fixClassFile(final File generated, final File common) {
        final List<String> lines = readLines(generated);
        final List<String> imports = getAllImports(lines);
        final List<String> broken = getBrokenReferences(imports);
        final List<String> missing = getMissingImports(readLines(common));

        if (broken.isEmpty() && missing.isEmpty()) {
            return;
        }

        final int index = getPackageIndex(lines) + 1;
        lines.addAll(index, missing);
        lines.removeIf(broken::contains);
        writeLines(generated, lines);
    }

    private static List<String> readLines(final File f) {
        try {
            return Files.readAllLines(f.toPath());
        } catch (final IOException e) {
            throw new UncheckedIOException("Fixing imports", e);
        }
    }

    private static List<String> getAllImports(final List<String> lines) {
        final List<String> imports = new ArrayList<>();
        for (final String line : lines) {
            if (ALL_IMPORT_PATTERN.matcher(line).matches()) {
                imports.add(line);
            }
        }
        return imports;
    }

    // Spoon breaks nested class references by importing them as a package.
    private static List<String> getBrokenReferences(final List<String> imports) {
        final List<String> broken = new ArrayList<>();
        for (final String i : imports) {
            final Matcher matcher = BROKEN_IMPORT_PATTERN.matcher(i);
            if (matcher.matches() && isBroken(imports, i, matcher.group(1))) {
                broken.add(i);
            }
        }
        return broken;
    }

    private static boolean isBroken(final List<String> imports, final String i, final String ref) {
        for (final String i2 : imports) {
            // Not the same line, but does contain the reference
            if (!i.equals(i2) && i2.contains(ref)) {
                return true;
            }
        }
        return false;
    }

    // These types of imports are never copied by Spoon.
    private static List<String> getMissingImports(final List<String> lines) {
        final List<String> imports = new ArrayList<>();
        for (final String line : lines) {
            if (COLLAPSED_IMPORT_PATTERN.matcher(line).matches() || STATIC_IMPORT_PATTERN.matcher(line).matches()) {
                imports.add(line);
            }
        }
        return imports;
    }

    private static int getPackageIndex(final List<String> lines) {
        int num = 0;
        for (final String line : lines) {
            if (PACKAGE_PATTERN.matcher(line).matches()) {
                return num;
            }
            num++;
        }
        throw new IllegalStateException("No package declaration in Java file.");
    }

    private static void writeLines(final File f, final List<String> lines) {
        try {
            Files.write(f.toPath(), lines, Charset.defaultCharset());
        } catch (final IOException e) {
            throw new UncheckedIOException("Fixing imports", e);
        }
    }
}

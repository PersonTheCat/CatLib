package personthecat.buildtools.processors;

import org.gradle.api.Project;
import personthecat.buildtools.CtUtils;
import personthecat.buildtools.LauncherContext;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MissingOverwriteProcessor {

    /** The header message for missing overrides. */
    private static final String MISSING_MEMBERS = "Missing members";

    /** The header message for unreachable overrides. */
    private static final String UNREACHABLE_MEMBERS = "Unreachable members";

    public static void processModel(final Project project, final CtModel model) {
        final ValidationContext ctx = validateAll(project, CtUtils.getAllClasses(model));
        if (ctx.anyErrors()) {
            ctx.printErrors();
            throw new InvalidOverwritesException(project);
        }
    }

    static ValidationContext validateAll(final Project project, final List<CtType<?>> classes) {
        final ValidationContext ctx = new ValidationContext(project);
        for (final CtType<?> ctClass : classes) {
            final CtType<?> overwritten = LauncherContext.getOverwrittenClass(ctClass);
            if (overwritten != null) {
                putMissingOverrides(ctx.missing, ctClass, overwritten);
                putUnreachableConstants(ctx.unreachable, ctClass, overwritten);
            }
        }
        return ctx;
    }

    static void putMissingOverrides(final MemberMap missing, final CtType<?> type, final CtType<?> overwritten) {
        final String relativeName = getRelativeName(type);
        for (final CtTypeMember member : CtUtils.getPublicMembers(overwritten)) {
            if (!CtUtils.classOverridesMember(type, member)) {
                missing.add(type.getQualifiedName(), CtUtils.formatMember(relativeName, member));
            }
        }
    }

    static void putUnreachableConstants(final MemberMap unreachable, final CtType<?> type, final CtType<?> overwritten) {
        final String relativeName = getRelativeName(type);
        for (final CtField<?> field : CtUtils.getPublicConstants(overwritten)) {
            if (CtUtils.classOverridesField(type, field)) {
                unreachable.add(type.getQualifiedName(), CtUtils.formatField(relativeName, field));
            }
        }
    }

    static String getRelativeName(final CtType<?> ctClass) {
        return ctClass.getQualifiedName().substring(ctClass.getPackage().getQualifiedName().length() + 1);
    }

    private static class MemberMap extends HashMap<String, List<String>> {
        void add(final String qualifiedClass, final String formattedMember) {
            final List<String> members = this.computeIfAbsent(qualifiedClass, k -> new ArrayList<>());
            members.add(formattedMember);
        }
    }

    private static class ValidationContext {
        final MemberMap missing = new MemberMap();
        final MemberMap unreachable = new MemberMap();
        final Project project;

        ValidationContext(final Project project) {
            this.project = project;
        }

        boolean anyErrors() {
            return !(missing.isEmpty() && unreachable.isEmpty());
        }

        void printErrors() {
            if (!this.missing.isEmpty()) {
                System.err.println(createRedText(this.formatInvalidMembers(MISSING_MEMBERS, this.missing)));
            }
            if (!this.unreachable.isEmpty()) {
                System.err.println(createRedText(this.formatInvalidMembers(UNREACHABLE_MEMBERS, this.unreachable)));
            }
        }

        String formatInvalidMembers(final String head, final Map<String, List<String>> missing) {
            final StringBuilder sb = new StringBuilder();
            for (final Map.Entry<String, List<String>> entry : missing.entrySet()) {
                sb.append(buildInvalidMemberMessage(head, entry.getKey(), entry.getValue()));
            }
            return sb.substring(0, sb.length() - 1);
        }

        String buildInvalidMemberMessage(final String head, final String className, final List<String> invalid) {
            final StringBuilder msg = new StringBuilder("\n")
                .append(head)
                .append(" in '")
                .append(this.project.getName())
                .append("' @ ")
                .append(className)
                .append('\n');
            for (final String member : invalid) {
                msg.append(" * ").append(member).append('\n');
            }
            return msg.toString();
        }

        static String createRedText(final String text) {
            return "\u001B[31m" + text + "\u001B[0m";
        }
    }

    private static class InvalidOverwritesException extends IllegalStateException {
        InvalidOverwritesException(final Project project) {
            super("Project '" + project.getName() + "' contains invalid overwrites");
        }
    }
}

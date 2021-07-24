package personthecat.catlib.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.StringArgumentType;
import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import org.hjson.JsonValue;
import personthecat.catlib.command.arguments.HjsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.io.FileIO;
import personthecat.catlib.util.HjsonTools;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static personthecat.catlib.command.CommandSuggestions.ANY_VALUE;
import static personthecat.catlib.command.CommandUtils.fileArg;
import static personthecat.catlib.command.CommandUtils.greedyArg;
import static personthecat.catlib.command.CommandUtils.jsonFileArg;
import static personthecat.catlib.command.CommandUtils.jsonPathArg;

@UtilityClass
@SuppressWarnings("unused")
public class DefaultLibCommands {

    public static final String FILE_ARGUMENT = "file";
    public static final String PATH_ARGUMENT = "path";
    public static final String VALUE_ARGUMENT = "value";
    public static final String DIRECTORY_ARGUMENT = "dir";
    public static final String NAME_ARGUMENT = "name";

    /** The header displayed whenever the /display command runs. */
    private static final String DISPLAY_HEADER = "--- {} ---\n";

    /** The text formatting to be used for the display message header. */
    private static final Style DISPLAY_HEADER_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GREEN)
        .withBold(true);

    /** The text formatting used to indicate values being deleted. */
    private static final Style DELETED_VALUE_STYLE = Style.EMPTY
        .withColor(ChatFormatting.RED);

    /** The text formatting use to indicate values being replaced. */
    private static final Style REPLACED_VALUE_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GREEN);

    /** The text formatting used for the undo button. */
    private static final Style UNDO_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GRAY)
        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to undo.")))
        .withUnderlined(true)
        .withBold(true);

    /** The number of backups before a warning is displayed. */
    private static final int BACKUP_COUNT_WARNING = 10;

    private static final String DISPLAY_ANY = "#display";
    private static final String UPDATE_ANY = "#update";
    private static final String BACKUP_ANY = "#backup";
    private static final String COPY_ANY = "#copy";
    private static final String MOVE_ANY = "#move";
    private static final String DELETE_ANY = "#delete";
    private static final String CLEAN_ANY = "#clean";
    private static final String RENAME_ANY = "#rename";
    private static final String OPEN_ANY = "#open";
    private static final String COMBINE_ANY = "#combine";

    public static List<LibCommandBuilder> createAll(final ModDescriptor mod, final boolean global) {
        return Lists.newArrayList(
            createDisplay(mod, global),
            createUpdate(mod, global),
            createBackup(mod, global),
            createCopy(mod, global),
            createMove(mod, global),
            createDelete(mod, global),
            createClean(mod, global),
            createRename(mod, global),
            createOpen(mod, global)
        );
    }

    public static LibCommandBuilder createDisplay(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("display")
            .arguments("<file> [<path>]")
            .append("Outputs the contents of any JSON file to the chat.")
            .wrap(DISPLAY_ANY, DefaultLibCommands::executeDisplay)
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder
                .then(jsonFileArg(FILE_ARGUMENT, mod)
                    .executes(wrappers.get(DISPLAY_ANY))
                .then(jsonPathArg(PATH_ARGUMENT)
                    .executes(wrappers.get(DISPLAY_ANY))))
            );
    }

    public static LibCommandBuilder createUpdate(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("update")
            .arguments("<file> [<path>] [<value>]")
            .append("Manually update a JSON value. Omit the value or")
            .append("path to display the current contents.")
            .wrap(DISPLAY_ANY, DefaultLibCommands::executeDisplay)
            .wrap(UPDATE_ANY, DefaultLibCommands::executeUpdate)
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder
                .then(jsonFileArg(FILE_ARGUMENT, mod)
                    .executes(wrappers.get(DISPLAY_ANY))
                .then(jsonPathArg(PATH_ARGUMENT)
                    .executes(wrappers.get(DISPLAY_ANY))
                .then(greedyArg(VALUE_ARGUMENT, ANY_VALUE)
                    .executes(wrappers.get(UPDATE_ANY)))))
            );
    }

    public static LibCommandBuilder createBackup(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("backup")
            .arguments("<file>")
            .append("Copies a file to the current mod's backup folder.")
            .wrap(BACKUP_ANY, DefaultLibCommands::executeBackup)
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder
                .then(fileArg(FILE_ARGUMENT)
                    .executes(wrappers.get(BACKUP_ANY)))
            );
    }

    public static LibCommandBuilder createCopy(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("copy")
            .arguments("<file> <to>")
            .append("Copies a file from one location to another.")
            .append("Accepts either a directory or a file as <to>.")
            .wrap(COPY_ANY, DefaultLibCommands::executeCopy)
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder
                .then(fileArg(FILE_ARGUMENT)
                .then(fileArg(DIRECTORY_ARGUMENT)
                    .executes(wrappers.get(COPY_ANY))))
            );
    }

    public static LibCommandBuilder createMove(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("move")
            .arguments("<file> <to>")
            .append("Moves a file from one location to another.")
            .append("Accepts either a directory or a file as <to>.")
            .wrap(MOVE_ANY, DefaultLibCommands::executeMove)
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder
                .then(fileArg(FILE_ARGUMENT)
                .then(fileArg(DIRECTORY_ARGUMENT)
                    .executes(wrappers.get(MOVE_ANY))))
            );
    }

    public static LibCommandBuilder createDelete(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("delete")
            .arguments("<file>")
            .append("Moves a file to the current mod's backup folder.")
            .wrap(DELETE_ANY, DefaultLibCommands::executeDelete)
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder
                .then(fileArg(FILE_ARGUMENT)
                    .executes(wrappers.get(DELETE_ANY)))
            );
    }

    public static LibCommandBuilder createClean(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("clean")
            .arguments("[<directory>]")
            .append("Moves all files in the given directory to the mod's")
            .append("backup folder. Else, deletes the contents of the")
            .append("mod's backup folder.")
            .wrap(CLEAN_ANY, DefaultLibCommands::executeClean)
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder.executes(wrappers.get(CLEAN_ANY))
                .then(fileArg(DIRECTORY_ARGUMENT)
                    .executes(wrappers.get(CLEAN_ANY)))
            );
    }

    public static LibCommandBuilder createRename(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("rename")
            .arguments("<file> <to>")
            .append("Renames a file without needing a path or extension.")
            .wrap(RENAME_ANY, DefaultLibCommands::executeRename)
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder
                .then(fileArg(FILE_ARGUMENT)
                .then(Commands.argument(NAME_ARGUMENT, StringArgumentType.word())
                    .executes(wrappers.get(RENAME_ANY))))
            );
    }

    public static LibCommandBuilder createOpen(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("open")
            .arguments("[<name>]")
            .append("Opens a file or directory in your default editor.")
            .wrap(OPEN_ANY, DefaultLibCommands::executeOpen)
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder
                .then(fileArg(FILE_ARGUMENT)
                    .executes(wrappers.get(OPEN_ANY)))
            );
    }

    private static void executeDisplay(final CommandContextWrapper wrapper) {
        final HjsonArgument.Result file = wrapper.getJsonFile(FILE_ARGUMENT);
        final JsonValue json = wrapper.getOptional(PATH_ARGUMENT, PathArgument.Result.class)
            .flatMap(result -> HjsonTools.getValueFromPath(file.json.get(), result))
            .orElseGet(file.json);

        wrapper.generateMessage("")
            .append(wrapper.createText(DISPLAY_HEADER, file.file.getName()).setStyle(DISPLAY_HEADER_STYLE))
            .append(wrapper.lintMessage(json.toString(HjsonTools.FORMATTER)))
            .sendMessage();
    }

    private static void executeUpdate(final CommandContextWrapper wrapper) {
        final HjsonArgument.Result file = wrapper.getJsonFile(FILE_ARGUMENT);
        final PathArgument.Result path = wrapper.getJsonPath(PATH_ARGUMENT);

        // Read the old and new values.
        final String toEscaped = wrapper.getString(VALUE_ARGUMENT);
        final String toLiteral = unEscape(toEscaped);
        final JsonValue toValue = JsonValue.readHjson(toLiteral);
        final JsonValue fromValue = HjsonTools.getValueFromPath(file.json.get(), path)
            .orElseGet(() -> JsonValue.valueOf(null));
        final String fromLiteral = fromValue.toString(HjsonTools.FORMATTER);
        final String fromEscaped = escape(fromLiteral);

        // Write the new value.
        HjsonTools.setValueFromPath(file.json.get(), path, toValue);
        HjsonTools.writeJson(file.json.get(), file.file)
            .expect("Error writing to file: {}", file.file.getName());

        // Send feedback.
        wrapper.generateMessage("Successfully updated {}.\n", file.file.getName())
            .append(fromLiteral.replace("\r", ""), DELETED_VALUE_STYLE)
            .append(" -> ")
            .append(toLiteral, REPLACED_VALUE_STYLE)
            .append(" ")
            .append(generateUndo(wrapper.getInput(), fromEscaped, toEscaped))
            .sendMessage();
    }

    private static TextComponent generateUndo(final String input, final String from, final String to) {
        final int index = input.lastIndexOf(to);
        final String command = (input.substring(0, index) + from).replace("\"\"", "\"");
        final ClickEvent undo = new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
        return (TextComponent) new TextComponent("[UNDO]").setStyle(UNDO_STYLE.withClickEvent(undo));
    }

    private static String escape(final String literal) {
        return literal.replaceAll("\r?\n", "\\\\n") // newline -> \n
            .replace("\"", "\\\""); // " -> \"
    }

    private static String unEscape(final String escaped) {
        return escaped.replace("\\n", "\n")
            .replace("\\\"", "\"");
    }

    private static void executeBackup(final CommandContextWrapper wrapper) {
        if (BACKUP_COUNT_WARNING < FileIO.backup(wrapper.getBackupsFolder(), wrapper.getFile(FILE_ARGUMENT), true)) {
            wrapper.sendError("{} backups detected. Consider cleaning these out.", BACKUP_COUNT_WARNING);
        }
        wrapper.sendMessage("Backup created successfully.");
    }

    private static void executeTest(final CommandContextWrapper wrapper) {
        if (BACKUP_COUNT_WARNING < FileIO.backup(wrapper.getBackupsFolder(), wrapper.getFile(FILE_ARGUMENT), true)) {
            wrapper.sendError("{} backups detected. Consider cleaning these out.", BACKUP_COUNT_WARNING);
        }
        wrapper.sendMessage("Backup created successfully.");
    }

    private static void executeCopy(final CommandContextWrapper wrapper) {
        FileIO.copy(wrapper.getFile(FILE_ARGUMENT), wrapper.getFile(DIRECTORY_ARGUMENT))
            .expect("The file could not be copied.");
        wrapper.sendMessage("File copied successfully.");
    }

    private static void executeMove(final CommandContextWrapper wrapper) {
        FileIO.move(wrapper.getFile(FILE_ARGUMENT), wrapper.getFile(DIRECTORY_ARGUMENT))
            .expect("The file could not be moved.");
        wrapper.sendMessage("File moved successfully.");
    }

    private static void executeDelete(final CommandContextWrapper wrapper) {
        if (BACKUP_COUNT_WARNING < FileIO.backup(wrapper.getBackupsFolder(), wrapper.getFile(FILE_ARGUMENT), false)) {
            wrapper.sendError("{} backups have been created. Consider cleaning these out.", BACKUP_COUNT_WARNING);
        }
        wrapper.sendMessage("File moved to backups.");
    }

    private static void executeClean(final CommandContextWrapper wrapper) {
        final Optional<File> directory = wrapper.getOptional(DIRECTORY_ARGUMENT, File.class)
            .filter(dir -> !dir.equals(wrapper.getBackupsFolder()));
        if (directory.isPresent()) {
            final File dir = directory.get();
            if (dir.isFile()) {
                if (!dir.delete()) {
                    wrapper.sendError("Error deleting {}", dir.getName());
                } else {
                    wrapper.sendMessage("Deleted {}", dir.getName());
                }
            } else {
                cleanFiles(wrapper, dir);
            }
        } else {
            cleanFiles(wrapper, wrapper.getBackupsFolder());
        }
    }

    private static void cleanFiles(final CommandContextWrapper wrapper, final File dir) {
        final int deleted = deleteNotRecursive(dir);
        if (deleted < 0) {
            wrapper.sendError("Some files could not be deleted.");
        } else {
            wrapper.sendMessage("Deleted {} files.", deleted);
        }
    }

    private static int deleteNotRecursive(final File dir) {
        int deleted = 0;
        for (final File f : FileIO.listFiles(dir)) {
            if (f.isFile() && !f.delete()) {
                return -1;
            }
            deleted++;
        }
        return deleted;
    }

    private static void executeRename(final CommandContextWrapper wrapper) {
        FileIO.rename(wrapper.getFile(FILE_ARGUMENT), wrapper.getString(NAME_ARGUMENT))
            .expect("Error renaming file.");
        wrapper.sendMessage("File renamed successfully.");
    }

    public static void executeOpen(final CommandContextWrapper wrapper) {
        Util.getPlatform().openFile(wrapper.getFile(FILE_ARGUMENT));
    }
}

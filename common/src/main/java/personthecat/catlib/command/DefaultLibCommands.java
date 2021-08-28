package personthecat.catlib.command;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.brigadier.arguments.StringArgumentType;
import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import org.hjson.HjsonOptions;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.catlib.command.arguments.HjsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.io.FileIO;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.JsonCombiner;
import personthecat.catlib.util.PathUtils;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static personthecat.catlib.command.CommandSuggestions.ANY_VALUE;
import static personthecat.catlib.command.CommandUtils.arg;
import static personthecat.catlib.command.CommandUtils.fileArg;
import static personthecat.catlib.command.CommandUtils.greedyArg;
import static personthecat.catlib.command.CommandUtils.jsonFileArg;
import static personthecat.catlib.command.CommandUtils.jsonPathArg;
import static personthecat.catlib.util.PathUtils.extension;
import static personthecat.catlib.util.PathUtils.noExtension;

@UtilityClass
@SuppressWarnings("unused")
public class DefaultLibCommands {

    public static final String FILE_ARGUMENT = "file";
    public static final String TO_ARGUMENT = "to";
    public static final String PATH_ARGUMENT = "path";
    public static final String VALUE_ARGUMENT = "value";
    public static final String DIRECTORY_ARGUMENT = "dir";
    public static final String NAME_ARGUMENT = "name";
    public static final String MAX_ARGUMENT = "max";
    public static final String SCALE_ARGUMENT = "scale";

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

    /** Require braces for objects when executing the update command. */
    private static final HjsonOptions NO_EMIT_BRACES = new HjsonOptions().setParseLegacyRoot(false);

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
    private static final String CH_ANY = "#ch";
    private static final String CH_MAX = "#ch_max";
    private static final String CW_ANY = "#cw";
    private static final String CW_MAX = "#cw_max";
    private static final String TO_JSON_ANY = "#to_json";
    private static final String TO_HJSON_ANY = "#to_hjson";

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
            createOpen(mod, global),
            createCombine(mod, global),
            createCh(mod, global),
            createCw(mod, global),
            createToJson(mod, global),
            createToHjson(mod, global)
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
                .then(fileArg(FILE_ARGUMENT, mod)
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
                .then(fileArg(FILE_ARGUMENT, mod)
                .then(fileArg(DIRECTORY_ARGUMENT, mod)
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
                .then(fileArg(FILE_ARGUMENT, mod)
                .then(fileArg(DIRECTORY_ARGUMENT, mod)
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
                .then(fileArg(FILE_ARGUMENT, mod)
                    .executes(wrappers.get(DELETE_ANY)))
            );
    }

    public static LibCommandBuilder createClean(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("clean")
            .arguments("<directory>")
            .append("Moves all files in the given directory to the mod's")
            .append("backup folder. Else, deletes the contents of the")
            .append("mod's backup folder.")
            .wrap(CLEAN_ANY, DefaultLibCommands::executeClean)
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder.executes(wrappers.get(CLEAN_ANY))
                .then(fileArg(DIRECTORY_ARGUMENT, mod)
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
                .then(fileArg(FILE_ARGUMENT, mod)
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
                .then(fileArg(FILE_ARGUMENT, mod)
                    .executes(wrappers.get(OPEN_ANY)))
            );
    }

    public static LibCommandBuilder createCombine(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("combine")
            .arguments("<file> <path> <to>")
            .append("Copies the given path from the first preset into the second preset.")
            .wrap(COMBINE_ANY, DefaultLibCommands::executeCombine)
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder
                .then(jsonFileArg(FILE_ARGUMENT, mod)
                .then(jsonPathArg(PATH_ARGUMENT)
                .then(jsonFileArg(TO_ARGUMENT, mod)
                    .executes(wrappers.get(COMBINE_ANY)))))
            );
    }

    public static LibCommandBuilder createCh(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("ch")
            .arguments("[<scale|max>]")
            .append("Allows you to expand your chat height beyond the")
            .append("default limit of 1.0.")
            .wrap(CH_ANY, wrapper -> executeCh(wrapper, false))
            .wrap(CH_MAX, wrapper -> executeCh(wrapper, true))
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .side(CommandSide.CLIENT)
            .generate((builder, wrappers) -> builder.executes(wrappers.get(CH_ANY))
                .then(Commands.literal(MAX_ARGUMENT).executes(wrappers.get(CH_MAX)))
                .then(arg(SCALE_ARGUMENT, 0.25, 5.0).executes(wrappers.get(CH_ANY)))
            );
    }

    public static LibCommandBuilder createCw(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("cw")
            .arguments("[<scale|max>]")
            .append("Allows you to expand your chat width beyond the")
            .append("default limit of 1.0.")
            .wrap(CW_ANY, wrapper -> executeCw(wrapper, false))
            .wrap(CW_MAX, wrapper -> executeCw(wrapper, true))
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .side(CommandSide.CLIENT)
            .generate((builder, wrappers) -> builder.executes(wrappers.get(CW_ANY))
                .then(Commands.literal(MAX_ARGUMENT).executes(wrappers.get(CW_MAX)))
                .then(arg(SCALE_ARGUMENT, 0.25, 3.0).executes(wrappers.get(CW_ANY)))
            );
    }

    public static LibCommandBuilder createToJson(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("tojson")
            .arguments("<file>")
            .append("Converts an Hjson file to a regular JSON file.")
            .wrap(TO_JSON_ANY, wrapper -> convert(wrapper, true))
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder
                .then(fileArg(FILE_ARGUMENT, mod)
                    .executes(wrappers.get(TO_JSON_ANY))));
    }

    public static LibCommandBuilder createToHjson(final ModDescriptor mod, final boolean global) {
        return LibCommandBuilder.named("tohjson")
            .arguments("<file>")
            .append("Converts an Hjson file to a regular JSON file.")
            .wrap(TO_HJSON_ANY, wrapper -> convert(wrapper, false))
            .type(global ? CommandType.GLOBAL : CommandType.MOD)
            .generate((builder, wrappers) -> builder
                .then(fileArg(FILE_ARGUMENT, mod)
                    .executes(wrappers.get(TO_HJSON_ANY))));
    }

    private static void executeDisplay(final CommandContextWrapper wrapper) {
        final HjsonArgument.Result file = wrapper.getJsonFile(FILE_ARGUMENT);
        final JsonValue json = wrapper.getOptional(PATH_ARGUMENT, PathArgument.Result.class)
            .flatMap(result -> HjsonUtils.getValueFromPath(file.json.get(), result))
            .orElseGet(file.json);

        final String header = file.file.getParentFile().getName() + "/" + file.file.getName();
        wrapper.generateMessage("")
            .append(wrapper.createText(DISPLAY_HEADER, header).setStyle(DISPLAY_HEADER_STYLE))
            .append(wrapper.lintMessage(json.toString(HjsonUtils.NO_CR)))
            .sendMessage();
    }

    private static void executeUpdate(final CommandContextWrapper wrapper) {
        final HjsonArgument.Result file = wrapper.getJsonFile(FILE_ARGUMENT);
        final PathArgument.Result path = wrapper.getJsonPath(PATH_ARGUMENT);

        // Read the old and new values.
        final String toEscaped = wrapper.getString(VALUE_ARGUMENT);
        final String toLiteral = unEscape(toEscaped);
        final JsonValue toValue = JsonValue.readHjson(toLiteral, NO_EMIT_BRACES);
        final JsonValue fromValue = HjsonUtils.getValueFromPath(file.json.get(), path)
            .orElseGet(() -> JsonValue.valueOf(null));
        final String fromLiteral = fromValue.toString(HjsonUtils.NO_CR);
        final String fromEscaped = escape(fromLiteral);

        // Write the new value.
        HjsonUtils.setValueFromPath(file.json.get(), path, toValue.isNull() ? null : toValue);
        HjsonUtils.writeJson(file.json.get(), file.file)
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
        final File file = wrapper.getFile(FILE_ARGUMENT);
        if (PathUtils.isIn(wrapper.getMod().getBackupFolder(), file)) {
            FileIO.delete(file);
            wrapper.sendMessage("File deleted successfully.");
        } else if (BACKUP_COUNT_WARNING < FileIO.backup(wrapper.getBackupsFolder(), wrapper.getFile(FILE_ARGUMENT), false)) {
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

    private static void executeOpen(final CommandContextWrapper wrapper) {
        Util.getPlatform().openFile(wrapper.getFile(FILE_ARGUMENT));
    }

    private static void executeCombine(final CommandContextWrapper wrapper) {
        final HjsonArgument.Result from = wrapper.getJsonFile(FILE_ARGUMENT);
        final PathArgument.Result path = wrapper.getJsonPath(PATH_ARGUMENT);
        final HjsonArgument.Result to = wrapper.getJsonFile(TO_ARGUMENT);

        if (BACKUP_COUNT_WARNING < FileIO.backup(wrapper.getBackupsFolder(), to.file, true)) {
            wrapper.sendError("Created > {} backups. Consider cleaning these out.", BACKUP_COUNT_WARNING);
        }
        JsonCombiner.combine(from, to, path);
        wrapper.sendMessage("Finished combining file. The original was moved to the backups directory.");
    }

    private static void executeCh(final CommandContextWrapper wrapper, final boolean max) {
        final Minecraft mc = Minecraft.getInstance();
        final Options cfg = mc.options;
        final Window window = Objects.requireNonNull(mc.getWindow(), "Not running client side");
        final Optional<Double> scaleArgument = wrapper.getOptional(SCALE_ARGUMENT, Double.class);

        if (!max && !scaleArgument.isPresent()) {
            wrapper.sendMessage("Current chat height: {}", cfg.chatHeightFocused);
            return;
        }

        final double possible = (window.getGuiScaledHeight() - 20.0) / 180.0;
        if (max) {
            cfg.chatHeightFocused = possible;
        } else {
            final double height = (scaleArgument.get() * 180.0 + 20.0) * window.getGuiScale();
            if (height > window.getHeight()) {
                wrapper.sendError("Max size is {} with your current window and scale.", possible);
                return;
            }
            cfg.chatHeightFocused = scaleArgument.get();
        }
        cfg.save();

        wrapper.sendMessage("Updated chat height: {}", cfg.chatHeightFocused);
    }

    private static void executeCw(final CommandContextWrapper wrapper, final boolean max) {
        final Minecraft mc = Minecraft.getInstance();
        final Options cfg = mc.options;
        final Window window = Objects.requireNonNull(mc.getWindow(), "Not running client side");
        final Optional<Double> scaleArgument = wrapper.getOptional(SCALE_ARGUMENT, Double.class);

        if (!max && !scaleArgument.isPresent()) {
            wrapper.sendMessage("Current chat width: {}", cfg.chatWidth);
            return;
        }

        final double possible = (double) window.getGuiScaledWidth() / 320.0;
        if (max) {
            cfg.chatWidth = possible;
        } else {
            final double width = scaleArgument.get() * 320.0 * window.getGuiScale();
            if (width > window.getWidth()) {
                wrapper.sendError("Max size is {} with your current window and scale.", possible);
                return;
            }
            cfg.chatWidth = scaleArgument.get();
        }
        cfg.save();

        wrapper.sendMessage("Updated chat width: {}", cfg.chatWidth);
    }

    private static void convert(final CommandContextWrapper wrapper, final boolean toJson) {
        final File source = wrapper.getFile(FILE_ARGUMENT);
        if (!source.exists()) {
            wrapper.sendError("Found not found.");
            return;
        }
        if (toJson == "json".equals(extension(source))) {
            wrapper.sendError("File is already in the desired format.");
            return;
        }
        final Optional<JsonObject> json = HjsonUtils.readJson(source);
        if (!json.isPresent()) {
            wrapper.sendError("The file could not be read.");
            return;
        }
        final String extension = toJson ? "json" : "hjson";
        final File converted = new File(source.getParentFile(), noExtension(source) + extension);
        HjsonUtils.writeJson(json.get(), converted).expect("Error writing file.");

        if (!PathUtils.isIn(wrapper.getBackupsFolder(), source)) {
            FileIO.backup(wrapper.getBackupsFolder(), source, false);
            wrapper.sendMessage("File converted successfully. The original was moved to the backups directory.");
        } else {
            wrapper.sendMessage("File converted successfully. The original could not be backed up.");
        }
    }
}

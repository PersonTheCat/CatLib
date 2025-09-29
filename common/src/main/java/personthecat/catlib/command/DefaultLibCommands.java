package personthecat.catlib.command;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.apache.commons.io.function.Uncheck;
import personthecat.catlib.client.gui.SimpleTextPage;
import personthecat.catlib.command.arguments.JsonArgument;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.linting.IdListLinter;
import personthecat.catlib.linting.Linters;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.serialization.codec.CodecSupport;
import personthecat.catlib.serialization.json.JsonPath;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.io.FileIO;
import personthecat.catlib.serialization.json.XjsUtils;
import personthecat.catlib.serialization.json.JsonCombiner;
import personthecat.catlib.util.McUtils;
import personthecat.catlib.util.PathUtils;
import xjs.data.Json;
import xjs.data.JsonCopy;
import xjs.data.JsonObject;
import xjs.data.JsonValue;
import xjs.data.serialization.JsonContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.commands.Commands.literal;
import static personthecat.catlib.command.CommandUtils.boolArg;
import static personthecat.catlib.command.CommandUtils.clickToOpen;
import static personthecat.catlib.command.CommandUtils.displayOnHover;
import static personthecat.catlib.command.CommandUtils.enumArg;
import static personthecat.catlib.command.LibSuggestions.CURRENT_JSON;
import static personthecat.catlib.command.CommandUtils.arg;
import static personthecat.catlib.command.CommandUtils.clickToRun;
import static personthecat.catlib.command.CommandUtils.fileArg;
import static personthecat.catlib.command.CommandUtils.greedyArg;
import static personthecat.catlib.command.CommandUtils.idArg;
import static personthecat.catlib.command.CommandUtils.jsonFileArg;
import static personthecat.catlib.command.CommandUtils.jsonPathArg;
import static personthecat.catlib.command.CommandUtils.registryArg;
import static personthecat.catlib.util.LibUtil.f;
import static personthecat.catlib.util.PathUtils.extension;
import static personthecat.catlib.util.PathUtils.noExtension;

public final class DefaultLibCommands {
    public static final String FILE_ARGUMENT = "file";
    public static final String TO_ARGUMENT = "to";
    public static final String PATH_ARGUMENT = "path";
    public static final String VALUE_ARGUMENT = "value";
    public static final String DIRECTORY_ARGUMENT = "dir";
    public static final String NAME_ARGUMENT = "name";
    public static final String MAX_ARGUMENT = "max";
    public static final String SCALE_ARGUMENT = "scale";
    public static final String REGISTRY_ARGUMENT = "registry";
    public static final String ITEM_ARGUMENT = "item";
    public static final String OVERWRITE_ARGUMENT = "overwrite";
    public static final String FORMAT_ARGUMENT = "format";

    /** The header displayed whenever the /display command runs. */
    private static final String DISPLAY_HEADER = "--- {} ---";

    /** The text formatting to be used for the display message header. */
    private static final Style DISPLAY_HEADER_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GREEN)
        .withBold(true);

    /** The text formatting used to indicate values being deleted. */
    private static final Style DELETED_VALUE_STYLE = Style.EMPTY
        .withColor(ChatFormatting.RED);

    /** The text formatting used to indicate values being replaced. */
    private static final Style REPLACED_VALUE_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GREEN);

    /** The text formatting used for the undo button. */
    private static final Style UNDO_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GRAY)
        .withHoverEvent(displayOnHover("Click to undo."))
        .applyFormat(ChatFormatting.UNDERLINE)
        .withBold(true);

    /** The number of backups before a warning is displayed. */
    private static final int BACKUP_COUNT_WARNING = 10;

    private DefaultLibCommands() {}

    public static List<LibCommandBuilder> createAll(final ModDescriptor mod) {
        return Lists.newArrayList(
            createDisplay(mod),
            createUpdate(mod),
            createTest(mod),
            createUnTest(mod),
            createDebug(mod),
            createBackup(mod),
            createCopy(mod),
            createMove(mod),
            createDelete(mod),
            createClean(mod),
            createRename(mod),
            createOpen(mod),
            createCombine(mod),
            createCh(mod),
            createCw(mod),
            createNew(mod),
            createConvert(mod),
            createFormat(mod)
        );
    }

    public static LibCommandBuilder createDisplay(final ModDescriptor mod) {
        return LibCommandBuilder.named("display")
            .arguments("<file> [<path>]")
            .append("Outputs the contents of any JSON file to the chat.")
            .generate((builder, utl) -> builder
                .then(jsonFileArg(FILE_ARGUMENT, mod)
                    .executes(utl.wrap(DefaultLibCommands::display))
                .then(jsonPathArg(PATH_ARGUMENT)
                    .executes(utl.wrap(DefaultLibCommands::display))))
            );
    }

    public static LibCommandBuilder createUpdate(final ModDescriptor mod) {
        return LibCommandBuilder.named("update")
            .arguments("<file> [<path>] [<value>]")
            .append("Manually update a JSON value. Omit the value or")
            .append("path to display the current contents.")
            .generate((builder, utl) -> builder
                .then(jsonFileArg(FILE_ARGUMENT, mod)
                    .executes(utl.wrap(DefaultLibCommands::display))
                .then(jsonPathArg(PATH_ARGUMENT)
                    .executes(utl.wrap(DefaultLibCommands::display))
                .then(greedyArg(VALUE_ARGUMENT, CURRENT_JSON)
                    .executes(utl.wrap(DefaultLibCommands::update)))))
            );
    }

    public static LibCommandBuilder createTest(final ModDescriptor mod) {
        return LibCommandBuilder.named("test")
            .append("Applies night vision and spectator mode for easy cave viewing.")
            .generate((builder, utl) ->
                builder.executes(utl.wrap(DefaultLibCommands::test)));
    }

    public static LibCommandBuilder createUnTest(final ModDescriptor mod) {
        return LibCommandBuilder.named("untest")
            .append("Removes night vision and puts you in the default game mode.")
            .generate((builder, utl) ->
                builder.executes(utl.wrap(DefaultLibCommands::unTest)));
    }

    public static LibCommandBuilder createDebug(final ModDescriptor mod) {
        return LibCommandBuilder.named("debug")
            .arguments("<registry> [<item>]")
            .append("Dumps registry details to the chat")
            .generate((builder, utl) ->
                builder.executes(utl.wrap(DefaultLibCommands::debug))
                    .then(registryArg(REGISTRY_ARGUMENT, DynamicRegistries.rootKey())
                        .executes(utl.wrap(DefaultLibCommands::debug))
                    .then(idArg(ITEM_ARGUMENT)
                        .suggests(LibSuggestions.PREVIOUS_IDS)
                        .executes(utl.wrap(DefaultLibCommands::debug)))));
    }

    public static LibCommandBuilder createBackup(final ModDescriptor mod) {
        return LibCommandBuilder.named("backup")
            .arguments("<file>")
            .append("Copies a file to the current mod's backup folder.")
            .generate((builder, utl) -> builder
                .then(fileArg(FILE_ARGUMENT, mod)
                    .executes(utl.wrap(DefaultLibCommands::backup)))
            );
    }

    public static LibCommandBuilder createCopy(final ModDescriptor mod) {
        return LibCommandBuilder.named("copy")
            .arguments("<file> <to>")
            .append("Copies a file from one location to another.")
            .append("Accepts either a directory or a file as <to>.")
            .generate((builder, utl) -> builder
                .then(fileArg(FILE_ARGUMENT, mod)
                .then(fileArg(DIRECTORY_ARGUMENT, mod)
                    .executes(utl.wrap(DefaultLibCommands::copy))))
            );
    }

    public static LibCommandBuilder createMove(final ModDescriptor mod) {
        return LibCommandBuilder.named("move")
            .arguments("<file> <to>")
            .append("Moves a file from one location to another.")
            .append("Accepts either a directory or a file as <to>.")
            .generate((builder, utl) -> builder
                .then(fileArg(FILE_ARGUMENT, mod)
                .then(fileArg(DIRECTORY_ARGUMENT, mod)
                    .executes(utl.wrap( DefaultLibCommands::move))))
            );
    }

    public static LibCommandBuilder createDelete(final ModDescriptor mod) {
        return LibCommandBuilder.named("delete")
            .arguments("<file>")
            .append("Moves a file to the current mod's backup folder.")
            .generate((builder, utl) -> builder
                .then(fileArg(FILE_ARGUMENT, mod)
                    .executes(utl.wrap(DefaultLibCommands::delete)))
            );
    }

    public static LibCommandBuilder createClean(final ModDescriptor mod) {
        return LibCommandBuilder.named("clean")
            .arguments("<directory>")
            .append("Moves all files in the given directory to the mod's")
            .append("backup folder. Else, deletes the contents of the")
            .append("mod's backup folder.")
            .generate((builder, utl) -> builder.executes(utl.wrap(DefaultLibCommands::clean))
                .then(fileArg(DIRECTORY_ARGUMENT, mod)
                    .executes(utl.wrap(DefaultLibCommands::clean)))
            );
    }

    public static LibCommandBuilder createRename(final ModDescriptor mod) {
        return LibCommandBuilder.named("rename")
            .arguments("<file> <to>")
            .append("Renames a file without needing a path or extension.")
            .generate((builder, utl) -> builder
                .then(fileArg(FILE_ARGUMENT, mod)
                .then(Commands.argument(NAME_ARGUMENT, StringArgumentType.word())
                    .executes(utl.wrap(DefaultLibCommands::rename))))
            );
    }

    public static LibCommandBuilder createOpen(final ModDescriptor mod) {
        return LibCommandBuilder.named("open")
            .arguments("[<name>]")
            .append("Opens a file or directory in your default editor.")
            .generate((builder, utl) -> builder
                .then(fileArg(FILE_ARGUMENT, mod)
                    .executes(utl.wrap(DefaultLibCommands::open)))
            );
    }

    public static LibCommandBuilder createCombine(final ModDescriptor mod) {
        return LibCommandBuilder.named("combine")
            .arguments("<file> <path> <to>")
            .append("Copies the given path from the first preset into the second preset.")
            .generate((builder, utl) -> builder
                .then(jsonFileArg(FILE_ARGUMENT, mod)
                .then(jsonPathArg(PATH_ARGUMENT)
                .then(jsonFileArg(TO_ARGUMENT, mod)
                    .executes(utl.wrap(DefaultLibCommands::combine)))))
            );
    }

    public static LibCommandBuilder createCh(final ModDescriptor mod) {
        return LibCommandBuilder.named("ch")
            .arguments("[<scale|max>]")
            .append("Allows you to expand your chat height beyond the")
            .append("default limit of 1.0.")
            .side(CommandSide.CLIENT)
            .generate((builder, utl) -> builder.executes(utl.wrap(ctx -> ch(ctx, false)))
                .then(literal(MAX_ARGUMENT).executes(utl.wrap(ctx -> ch(ctx, true))))
                .then(arg(SCALE_ARGUMENT, 0.25, 5.0).executes(utl.wrap(ctx -> ch(ctx, false))))
            );
    }

    public static LibCommandBuilder createCw(final ModDescriptor mod) {
        return LibCommandBuilder.named("cw")
            .arguments("[<scale|max>]")
            .append("Allows you to expand your chat width beyond the")
            .append("default limit of 1.0.")
            .side(CommandSide.CLIENT)
            .generate((builder, utl) -> builder.executes(utl.wrap(ctx -> cw(ctx, false)))
                .then(literal(MAX_ARGUMENT).executes(utl.wrap(ctx -> cw(ctx, true))))
                .then(arg(SCALE_ARGUMENT, 0.25, 3.0).executes(utl.wrap(ctx -> cw(ctx, false))))
            );
    }

    public static LibCommandBuilder createNew(final ModDescriptor mod) {
        return LibCommandBuilder.named("new")
            .arguments("<file>")
            .append("Generates an empty data file.")
            .generate((builder, utl) ->
                builder.then(fileArg(FILE_ARGUMENT, mod)
                    .executes(utl.wrap(ctx -> newFile(ctx, false)))
                    .then(boolArg(OVERWRITE_ARGUMENT)
                        .executes(utl.wrap(ctx -> newFile(ctx, ctx.getBoolean(OVERWRITE_ARGUMENT)))))));
    }

    public static LibCommandBuilder createConvert(final ModDescriptor mod) {
        return LibCommandBuilder.named("convert")
            .arguments("<format> <file>")
            .append("Converts JSON-like files between formats.")
            .generate((builder, utl) -> builder
                .then(fileArg(FILE_ARGUMENT, mod)
                    .then(enumArg(FORMAT_ARGUMENT, Format.class)
                        .executes(utl.wrap(DefaultLibCommands::convert)))));
    }

    public static LibCommandBuilder createFormat(final ModDescriptor mod) {
        return LibCommandBuilder.named("format")
            .arguments("<file>")
            .append("Formats a JSON, JSONC, DJS, Hjson, or similar file.")
            .append("Formatting options will match CatLib settings.")
            .generate((builder, utl) -> builder
                .then(fileArg(FILE_ARGUMENT, mod)
                    .executes(utl.wrap(DefaultLibCommands::format))));
    }

    private static void display(final CommandContextWrapper wrapper) {
        final JsonArgument.Result file = wrapper.getJsonFile(FILE_ARGUMENT);

        // get raw text or parse to display value in file
        final String text = wrapper.getOptional(PATH_ARGUMENT, JsonPath.class)
            .filter(result -> !result.isEmpty())
            .flatMap(result ->
                XjsUtils.getValueFromPath(file.json.get(), result)
                    .map(json -> JsonContext.getWriter(extension(file.file)).stringify(json.trim(), XjsUtils.noCr())))
            .orElseGet(() -> Uncheck.get(() -> Files.readString(file.file).replace("\r", "")));

        final String header = file.file.getParent().getFileName() + "/" + file.file.getFileName();
        final Component headerComponent =
            Component.literal(f(DISPLAY_HEADER, header)).setStyle(DISPLAY_HEADER_STYLE);

        final Component detailsComponent = Linters.get(file.file, Linters.NONE).lint(text);

        final long numLines = text.chars().filter(c -> c == '\n').count();
        if (McUtils.isClientSide() && numLines >= LibConfig.displayLength()) {
            loadTextPage(wrapper, headerComponent, detailsComponent);
            return;
        }
        wrapper.sendMessage(
            Component.empty()
                .append(headerComponent)
                .append("\n")
                .append(detailsComponent));
    }

    @Environment(EnvType.CLIENT)
    private static void loadTextPage(
            final CommandContextWrapper wrapper, final Component header, final Component details) {
        wrapper.setScreen(new SimpleTextPage(null, header, details));
    }

    private static void update(final CommandContextWrapper wrapper) {
        final JsonArgument.Result file = wrapper.getJsonFile(FILE_ARGUMENT);
        final JsonPath path = wrapper.getJsonPath(PATH_ARGUMENT);

        // Read the old and new values.
        final String toEscaped = wrapper.getString(VALUE_ARGUMENT);
        final String toLiteral = unEscape(toEscaped);
        final JsonValue toValue = Json.parse(toLiteral).trim();
        final JsonValue fromValue = XjsUtils.getValueFromPath(file.json.get(), path)
            .orElseGet(() -> Json.value(null));
        final String fromLiteral = fromValue.unformatted().toString(XjsUtils.noCr());
        final String fromEscaped = escape(fromLiteral);

        // Write the new value.
        XjsUtils.setValueFromPath(file.json.get(), path, toValue.isNull() ? null : toValue);
        XjsUtils.writeJson(file.json.get(), file.file);

        wrapper.sendMessage(
            Component.literal(f("Successfully updated {}.\n", file.file.getFileName()))
                .append(Component.literal(fromLiteral.replace("\r", "")).withStyle(DELETED_VALUE_STYLE))
                .append(" -> ")
                .append(Component.literal(toLiteral).withStyle(REPLACED_VALUE_STYLE))
                .append(" ")
                .append(generateUndo(wrapper.getInput(), fromEscaped, toEscaped)));
    }

    private static Component generateUndo(final String input, final String from, final String to) {
        final int index = input.lastIndexOf(to);
        final String command = (input.substring(0, index) + from).replace("\"\"", "\"");
        final ClickEvent undo = clickToRun("/" + command);
        return Component.literal("[UNDO]").setStyle(UNDO_STYLE.withClickEvent(undo));
    }

    private static String escape(final String literal) {
        return literal.replaceAll("\r?\n", "\\\\n") // newline -> \n
            .replace("\"", "\\\""); // " -> \"
    }

    private static String unEscape(final String escaped) {
        return escaped.replace("\\n", "\n")
            .replace("\\\"", "\"");
    }

    private static void backup(final CommandContextWrapper wrapper) {
        if (BACKUP_COUNT_WARNING < FileIO.backup(wrapper.getMod().backupFolder(), wrapper.getFile(FILE_ARGUMENT), true)) {
            wrapper.sendError("{} backups detected. Consider cleaning these out.", BACKUP_COUNT_WARNING);
        }
        wrapper.sendMessage("Backup created successfully.");
    }

    private static void copy(final CommandContextWrapper wrapper) {
        FileIO.copy(wrapper.getFile(FILE_ARGUMENT), wrapper.getFile(DIRECTORY_ARGUMENT));
        wrapper.sendMessage("File copied successfully.");
    }

    private static void move(final CommandContextWrapper wrapper) {
        FileIO.move(wrapper.getFile(FILE_ARGUMENT), wrapper.getFile(DIRECTORY_ARGUMENT));
        wrapper.sendMessage("File moved successfully.");
    }

    private static void delete(final CommandContextWrapper wrapper) {
        final Path file = wrapper.getFile(FILE_ARGUMENT);
        if (PathUtils.isIn(wrapper.getMod().backupFolder(), file)) {
            FileIO.delete(file);
            wrapper.sendMessage("File deleted successfully.");
        } else if (BACKUP_COUNT_WARNING < FileIO.backup(wrapper.getMod().backupFolder(), wrapper.getFile(FILE_ARGUMENT), false)) {
            wrapper.sendError("{} backups have been created. Consider cleaning these out.", BACKUP_COUNT_WARNING);
        }
        wrapper.sendMessage("File moved to backups.");
    }

    private static void clean(final CommandContextWrapper wrapper) {
        final Optional<Path> directory = wrapper.getOptional(DIRECTORY_ARGUMENT, Path.class)
            .filter(dir -> !FileIO.isSameFile(dir, wrapper.getMod().backupFolder()));
        if (directory.isPresent()) {
            final Path dir = directory.get();
            if (Files.isRegularFile(dir)) {
                FileIO.delete(dir);
                wrapper.sendMessage("Deleted {}", dir.getFileName());
            } else {
                cleanFiles(wrapper, dir);
            }
        } else {
            cleanFiles(wrapper, wrapper.getMod().backupFolder());
        }
    }

    private static void cleanFiles(final CommandContextWrapper wrapper, final Path dir) {
        final int deleted = deleteNotRecursive(dir);
        if (deleted < 0) {
            wrapper.sendError("Some files could not be deleted.");
        } else {
            wrapper.sendMessage("Deleted {} files.", deleted);
        }
    }

    private static int deleteNotRecursive(final Path dir) {
        final AtomicInteger deleted = new AtomicInteger();
        try {
            FileIO.forEach(dir, f -> {
                if (Files.isRegularFile(f)) {
                    FileIO.delete(f);
                    deleted.incrementAndGet();
                }
            });
        } catch (final UncheckedIOException e) {
            return -1;
        }
        return deleted.get();
    }

    private static void rename(final CommandContextWrapper wrapper) {
        FileIO.rename(wrapper.getFile(FILE_ARGUMENT), wrapper.getString(NAME_ARGUMENT));
        wrapper.sendMessage("File renamed successfully.");
    }

    private static void open(final CommandContextWrapper wrapper) {
        final var file = wrapper.getFile(FILE_ARGUMENT);
        if (!Files.exists(file)) {
            XjsUtils.writeJson(Json.object(), file);
        }
        Util.getPlatform().openUri(file.toUri());
    }

    private static void combine(final CommandContextWrapper wrapper) {
        final JsonArgument.Result from = wrapper.getJsonFile(FILE_ARGUMENT);
        final JsonPath path = wrapper.getJsonPath(PATH_ARGUMENT);
        final JsonArgument.Result to = wrapper.getJsonFile(TO_ARGUMENT);

        if (BACKUP_COUNT_WARNING < FileIO.backup(wrapper.getMod().backupFolder(), to.file, true)) {
            wrapper.sendError("Created > {} backups. Consider cleaning these out.", BACKUP_COUNT_WARNING);
        }
        JsonCombiner.combine(from, to, path);
        wrapper.sendMessage("Finished combining file. The original was moved to the backups directory.");
    }

    private static void test(final CommandContextWrapper wrapper) {
        final Player player = wrapper.getPlayer();
        if (player != null) {
            wrapper.setGameMode(GameType.SPECTATOR);
            final Holder<MobEffect> nightVision = MobEffects.NIGHT_VISION;
            player.addEffect(new MobEffectInstance(nightVision, 999999999, 1, true, false));
        }
    }

    private static void unTest(final CommandContextWrapper wrapper) {
        final Player player = wrapper.getPlayer();
        if (player != null) {
            final GameType mode = Optional.of(wrapper.getServer())
                .map(MinecraftServer::getDefaultGameType)
                .orElse(GameType.CREATIVE);

            wrapper.setGameMode(mode);
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static void debug(final CommandContextWrapper wrapper) {
        RegistryHandle<?> handle = wrapper.getOptional(REGISTRY_ARGUMENT, RegistryHandle.class).orElse(null);
        if (handle == null) {
            handle = DynamicRegistries.get(DynamicRegistries.rootKey());
        }
        final ResourceLocation id = wrapper.getOptional(ITEM_ARGUMENT, ResourceLocation.class).orElse(null);
        if (id == null) {
            final var text = Arrays.toString(handle.keySet().toArray());
            wrapper.sendMessage(IdListLinter.INSTANCE.lint(text));
            return;
        }
        final Object item = handle.lookup(id);
        if (item == null) {
            wrapper.sendError("No such item in registry");
            return;
        }
        final String s = CodecSupport.anyToString(item);
        if (s != null) {
            wrapper.sendMessage(Linters.DJS.lint(s.replaceAll("\r\n", "\n")));
        } else {
            wrapper.sendMessage(item.toString());
        }
    }

    private static void ch(final CommandContextWrapper wrapper, final boolean max) {
        final Minecraft mc = Minecraft.getInstance();
        final Options cfg = mc.options;
        final Window window = Objects.requireNonNull(mc.getWindow(), "Not running client side");
        final Optional<Double> scaleArgument = wrapper.getOptional(SCALE_ARGUMENT, Double.class);

        if (!max && scaleArgument.isEmpty()) {
            wrapper.sendMessage("Current chat height: {}", cfg.chatHeightFocused().get());
            return;
        }

        final double possible = (window.getGuiScaledHeight() - 20.0) / 180.0;
        if (max) {
            cfg.chatHeightFocused().set(possible);
        } else {
            final double height = (scaleArgument.get() * 180.0 + 20.0) * window.getGuiScale();
            if (height > window.getHeight()) {
                wrapper.sendError("Max size is {} with your current window and scale.", possible);
                return;
            }
            cfg.chatHeightFocused().set(scaleArgument.get());
        }
        cfg.save();

        wrapper.sendMessage("Updated chat height: {}", cfg.chatHeightFocused().get());
    }

    private static void cw(final CommandContextWrapper wrapper, final boolean max) {
        final Minecraft mc = Minecraft.getInstance();
        final Options cfg = mc.options;
        final Window window = Objects.requireNonNull(mc.getWindow(), "Not running client side");
        final Optional<Double> scaleArgument = wrapper.getOptional(SCALE_ARGUMENT, Double.class);

        if (!max && scaleArgument.isEmpty()) {
            wrapper.sendMessage("Current chat width: {}", cfg.chatWidth().get());
            return;
        }

        final double possible = (double) window.getGuiScaledWidth() / 320.0;
        if (max) {
            cfg.chatWidth().set(possible);
        } else {
            final double width = scaleArgument.get() * 320.0 * window.getGuiScale();
            if (width > window.getWidth()) {
                wrapper.sendError("Max size is {} with your current window and scale.", possible);
                return;
            }
            cfg.chatWidth().set(scaleArgument.get());
        }
        cfg.save();

        wrapper.sendMessage("Updated chat width: {}", cfg.chatWidth().get());
    }

    private static void newFile(final CommandContextWrapper wrapper, final boolean overwrite) throws IOException {
        final Path file = wrapper.getFile(FILE_ARGUMENT);
        if (Files.exists(file)) {
            if (overwrite) {
                Files.delete(file);
            } else {
                wrapper.sendError("File already exists. Pass overwrite argument to replace file.");
                return;
            }
        }
        Files.createFile(file);
        final var filenameStyle = 
            Style.EMPTY.withUnderlined(true).withClickEvent(clickToOpen(file));
        wrapper.sendMessage(
            Component.empty()
                .append("Created new file: ")
                .append(Component.literal(file.getFileName().toString()).withStyle(filenameStyle)));
    }

    private static void convert(final CommandContextWrapper wrapper) {
        final Path source = wrapper.getFile(FILE_ARGUMENT);
        final Format format = wrapper.get(FORMAT_ARGUMENT, Format.class);
        if (!Files.exists(source)) {
            wrapper.sendError("Found not found.");
            return;
        }
        if (format.name().equalsIgnoreCase(extension(source))) {
            wrapper.sendError("File is already in the desired format.");
            return;
        }
        final Optional<JsonObject> json = XjsUtils.readJson(source);
        if (json.isEmpty()) {
            wrapper.sendError("The file could not be read.");
            return;
        }
        final String extension = format.name().toLowerCase();
        final Path converted = source.resolveSibling(noExtension(source) + "." + extension);
        XjsUtils.writeJson(json.get(), converted);

        if (!PathUtils.isIn(wrapper.getMod().backupFolder(), source)) {
            FileIO.backup(wrapper.getMod().backupFolder(), source, false);
            wrapper.sendMessage("File converted successfully. The original was moved to the backups directory.");
        } else {
            wrapper.sendMessage("File converted successfully. The original could not be backed up.");
        }
    }

    private static void format(final CommandContextWrapper wrapper) {
        final Path file = wrapper.getFile(FILE_ARGUMENT);
        if (!Files.exists(file)) {
            wrapper.sendError("No such file. Nothing to format.");
            return;
        }

        // recursive copy, nothing but values and comments
        XjsUtils.updateJson(file, o -> o.copy(JsonCopy.RECURSIVE | JsonCopy.COMMENTS));

        final var mod = wrapper.getMod();
        final var relativePath = mod.configFolder().relativize(file);
        final var viewCommand = String.format("/%s display %s", mod.commandPrefix(), relativePath);
        final var commandComponent =
            Component.literal(viewCommand)
                .withStyle(Style.EMPTY.withUnderlined(true).withClickEvent(clickToRun(viewCommand)));

        wrapper.sendMessage(
            Component.empty()
                .append("File updated successfully. View with ")
                .append(commandComponent));
    }

    private enum Format {
        JSON,
        JSONC,
        DJS,
        HJSON,
        UBJSON
    }
}

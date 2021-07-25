package personthecat.catlib.command;

import com.mojang.brigadier.context.CommandContext;
import lombok.AllArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.command.arguments.HjsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.exception.CommandExecutionException;
import personthecat.catlib.util.SyntaxLinter;
import personthecat.fresult.Result;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static personthecat.catlib.exception.Exceptions.cmdEx;
import static personthecat.catlib.util.Shorthand.f;

@SuppressWarnings("unused")
@AllArgsConstructor
public class CommandContextWrapper {

    /** The style used when printing error messages. */
    private static final Style ERROR_STYLE = Style.EMPTY.withColor(ChatFormatting.RED);

    private final CommandContext<CommandSourceStack> ctx;
    private final SyntaxLinter linter;
    private final ModDescriptor mod;

    public String getString(final String key) {
        return this.get(key, String.class);
    }

    public int getInt(final String key) {
        return this.get(key, Integer.class);
    }

    public double getDouble(final String key) {
        return this.get(key, Double.class);
    }

    public BlockState getBlock(final String key) {
        return this.get(key, BlockInput.class).getState();
    }

    public File getFile(final String key) {
        return this.get(key, File.class);
    }

    public HjsonArgument.Result getJsonFile(final String key) {
        return this.get(key, HjsonArgument.Result.class);
    }

    public PathArgument.Result getJsonPath(final String key) {
        return this.get(key, PathArgument.Result.class);
    }

    public <T> Optional<T> getOptional(final String key, final Class<T> type) {
        return Result.suppress(() -> this.get(key, type)).get(Result::IGNORE);
    }

    public <T> List<T> getList(final String key, final Class<T> type) {
        final List<T> list = new ArrayList<>();
        for (int i = 0; true; i++) {
            final Optional<T> arg = this.getOptional(key + i, type);
            if (arg.isPresent()) {
                list.add(arg.get());
            } else {
                return list;
            }
        }
    }

    public <T> T get(final String key, final Class<T> type) {
        return this.ctx.getArgument(key, type);
    }

    public void sendMessage(final String msg) {
        this.sendMessage(new TextComponent(msg));
    }

    public void sendMessage(final String msg, final Object... args) {
        this.sendMessage(f(msg, args));
    }

    public void sendMessage(final String msg, final Style style) {
        this.sendMessage(new TextComponent(msg).setStyle(style));
    }

    public void sendMessage(final Component msg) {
        this.ctx.getSource().sendSuccess(msg, true);
    }

    public void sendLintedMessage(final String message) {
        this.sendMessage(this.linter.lint(message));
    }

    public void sendError(final String msg) {
        this.ctx.getSource().sendFailure(new TextComponent(msg));
    }

    public void sendError(final String msg, final Object... args) {
        this.sendError(f(msg, args));
    }

    public void sendError(final String msg, final Style style) {
        this.ctx.getSource().sendFailure(new TextComponent(msg).setStyle(style));
    }

    public void sendError(final Component msg) {
        this.ctx.getSource().sendFailure(msg);
    }

    public PendingMessageWrapper generateMessage(final String msg) {
        return new PendingMessageWrapper(this, new TextComponent(msg));
    }

    public PendingMessageWrapper generateMessage(final String msg, final Object... args) {
        return this.generateMessage(f(msg, args));
    }

    public PendingMessageWrapper generateMessage(final String msg, final Style style) {
        return new PendingMessageWrapper(this, (TextComponent) new TextComponent(msg).setStyle(style));
    }

    public PendingMessageWrapper generateMessage(final TextComponent component) {
        return new PendingMessageWrapper(this, component);
    }

    public TextComponent createText(final String msg) {
        return new TextComponent(msg);
    }

    public TextComponent createText(final String msg, final Object... args) {
        return new TextComponent(f(msg, args));
    }

    public Component lintMessage(final String msg) {
        return this.linter.lint(msg);
    }

    public Component lintMessage(final String msg, final Object... args) {
        return this.linter.lint(f(msg, args));
    }

    public Level getLevel() {
        return this.ctx.getSource().getLevel();
    }

    public MinecraftServer getServer() {
        return this.ctx.getSource().getServer();
    }

    public boolean isClientSide() {
        return this.getLevel().isClientSide();
    }

    public void execute(final String command) {
        this.tryExecute(command).throwIfErr();
    }

    public Result<Integer, CommandExecutionException> tryExecute(final String command) {
        final Commands manager = this.getServer().getCommands();
        return Result.suppress(() -> manager.performCommand(this.ctx.getSource(), command))
            .mapErr(CommandExecutionException::new)
            .filter(i -> i >= 0, () -> cmdEx("Running nested command"));
    }

    public String getInput() {
        return this.ctx.getInput();
    }

    public ModDescriptor getMod() {
        return this.mod;
    }

    public File getBackupsFolder() {
        return this.mod.getBackupFolder();
    }

    public File getModConfigFolder() {
        return this.mod.getConfigFolder();
    }

    public CommandContext<CommandSourceStack> getCtx() {
        return this.ctx;
    }

    public CommandSourceStack getSource() {
        return this.ctx.getSource();
    }
}
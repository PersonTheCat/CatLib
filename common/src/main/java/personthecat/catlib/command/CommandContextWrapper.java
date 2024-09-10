package personthecat.catlib.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.command.arguments.JsonArgument;
import personthecat.catlib.event.lifecycle.ClientTickEvent;
import personthecat.catlib.serialization.json.JsonPath;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.exception.CommandExecutionException;
import personthecat.catlib.linting.SyntaxLinter;
import personthecat.fresult.Result;
import personthecat.fresult.Void;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static personthecat.catlib.util.LibUtil.f;

@Log4j2
public record CommandContextWrapper(
        CommandContext<CommandSourceStack> ctx, SyntaxLinter linter, ModDescriptor mod) {

    public String getString(final String key) {
        return this.get(key, String.class);
    }

    public int getInt(final String key) {
        return this.get(key, Integer.class);
    }

    public double getDouble(final String key) {
        return this.get(key, Double.class);
    }

    public boolean getBoolean(final String key) {
        return this.get(key, Boolean.class);
    }

    public BlockState getBlock(final String key) {
        return this.get(key, BlockInput.class).getState();
    }

    public Item getItem(final String key) {
        return this.get(key, ItemInput.class).getItem();
    }

    public File getFile(final String key) {
        return this.get(key, File.class);
    }

    public JsonArgument.Result getJsonFile(final String key) {
        return this.get(key, JsonArgument.Result.class);
    }

    public JsonPath getJsonPath(final String key) {
        return this.get(key, JsonPath.class);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptional(final String key, final Class<? super T> type) {
        return Result.suppress(() -> this.get(key, (Class<T>) type)).get(Result::IGNORE);//.map(t -> (A) t);
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

    public String getActual(final String key) {
        return this.tryGetActual(key).orElseThrow(() ->
            new CommandExecutionException(f("No such argument: {}", key)));
    }

    public Optional<String> tryGetActual(final String key) {
        for (final ParsedCommandNode<CommandSourceStack> node : this.ctx.getNodes()) {
            if (key.equals(node.getNode().getName())) {
                return Optional.of(node.getRange().get(this.getInput()));
            }
        }
        return Optional.empty();
    }

    public void sendMessage(final String msg) {
        this.sendMessage(Component.literal(msg));
    }

    public void sendMessage(final String msg, final Object... args) {
        this.sendMessage(f(msg, args));
    }

    public void sendMessage(final String msg, final ChatFormatting color) {
        this.sendMessage(msg, Style.EMPTY.withColor(color));
    }

    public void sendMessage(final String msg, final Style style) {
        this.sendMessage(Component.literal(msg).setStyle(style));
    }

    public void sendMessage(final Component msg) {
        this.sendMessage(() -> msg);
    }

    public void sendMessage(final Supplier<Component> supplier) {
        this.ctx.getSource().sendSuccess(supplier, true);
    }

    public void sendLintedMessage(final String message) {
        this.sendMessage(this.linter.lint(message));
    }

    public void sendError(final String msg) {
        this.ctx.getSource().sendFailure(Component.literal(msg));
    }

    public void sendError(final String msg, final Object... args) {
        this.sendError(f(msg, args));
    }

    public void sendError(final String msg, final Style style) {
        this.ctx.getSource().sendFailure(Component.literal(msg).setStyle(style));
    }

    public void sendError(final Component msg) {
        this.ctx.getSource().sendFailure(msg);
    }

    public PendingMessageWrapper generateMessage(final String msg) {
        return new PendingMessageWrapper(this, Component.literal(msg));
    }

    public PendingMessageWrapper generateMessage() {
        return new PendingMessageWrapper(this, Component.empty());
    }

    public PendingMessageWrapper generateMessage(final String msg, final Object... args) {
        return this.generateMessage(f(msg, args));
    }

    public PendingMessageWrapper generateMessage(final String msg, final Style style) {
        return new PendingMessageWrapper(this, Component.literal(msg).setStyle(style));
    }

    public PendingMessageWrapper generateMessage(final MutableComponent component) {
        return new PendingMessageWrapper(this, component);
    }

    public Component lintMessage(final String msg) {
        return this.linter.lint(msg);
    }

    public Component lintMessage(final String msg, final Object... args) {
        return this.linter.lint(f(msg, args));
    }

    @Environment(EnvType.CLIENT)
    public void setScreen(final Screen screen) {
        ClientTickEvent.registerSingle(minecraft -> minecraft.setScreen(screen));
    }

    @Nullable
    public Entity getEntity() {
        return this.ctx.getSource().getEntity();
    }

    public Entity assertEntity() {
        final Entity entity = this.getEntity();
        if (entity == null) throw new CommandExecutionException("Expected an entity");
        return entity;
    }

    @Nullable
    public Player getPlayer() {
        final Entity entity = this.getEntity();
        return entity instanceof Player ? (Player) entity : null;
    }

    public Player assertPlayer() {
        final Player player = this.getPlayer();
        if (player == null) throw new CommandExecutionException("Expected a player");
        return player;
    }

    public boolean isPlayer() {
        return this.getEntity() instanceof Player;
    }

    public Vec3 getPos() {
        return this.ctx.getSource().getPosition();
    }

    @SuppressWarnings("ConstantConditions")
    public void setGameMode(final GameType type) {
        if (this.isClientSide()) {
            if (this.getPlayer() instanceof LocalPlayer) {
                Minecraft.getInstance().gameMode.setLocalMode(type);
            }
        } else if (this.getPlayer() instanceof ServerPlayer player) {
            player.setGameMode(type);
        }
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

    public void execute(final String command, final Object... args) {
        this.execute(f(command, args));
    }

    public Result<Void, CommandExecutionException> tryExecute(final String command) {
        final Commands manager = this.getServer().getCommands();
        return Result.suppress(() -> manager.performPrefixedCommand(this.ctx.getSource(), command))
            .mapErr(CommandExecutionException::new);
    }

    public String getInput() {
        return this.ctx.getInput();
    }

    public ModDescriptor getMod() {
        return this.mod;
    }

    public CommandContext<CommandSourceStack> getCtx() {
        return this.ctx;
    }

    public CommandSourceStack getSource() {
        return this.ctx.getSource();
    }
}

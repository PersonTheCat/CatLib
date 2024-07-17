package personthecat.catlib.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.command.annotations.CommandBuilder;
import personthecat.catlib.command.function.CommandFunction;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.data.Lazy;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.LibStringUtils;
import personthecat.catlib.util.McUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.literal;
import static personthecat.catlib.command.CommandUtils.arg;
import static personthecat.catlib.command.CommandUtils.clickToRun;
import static personthecat.catlib.command.CommandUtils.displayOnHover;
import static personthecat.catlib.util.LibUtil.f;

/**
 * A helper used for generating and registering commands for the current mod.
 * <p>
 *   To use this context, generate a series of {@link LibCommandBuilder}s and
 *   pass them into the context via {@link #addCommand} <b>after</b> the context
 *   has been constructed.
 * </p><pre>
 *     CommandRegistrationContext.forMod(MOD_DESCRIPTOR)
 *       .addCommand(createCommand1())
 *       .addCommand(createCommand2())
 *       ...
 * </pre><p>
 *   Alternatively, the builders can be generated reflectively by parsing methods
 *   annotated with {@link ModCommand} or {@link CommandBuilder}. See the
 *   documentation inside of these classes for more information on using them.
 * <p>
 *   Classes containing the annotated methods can be passed into the context and
 *   processed automatically by calling {@link #addAllCommands(Class[])}.
 * </p><pre>
 *     CommandRegistrationContext.forMod(MOD_DESCRIPTOR)
 *       .addAllCommands(ModCommands.class, ClientModCommands.class)
 *       ...
 * </pre><p>
 *   Once each of the builders for the current mod have been passed into the
 *   context, calling {@link #registerAll()} will take care of generating the
 *   Brigadier command trees and registering them via {@link LibCommandRegistrar}.
 * </p><pre>
 *     CommandRegistrationContext.forMod(MOD_DESCRIPTOR)
 *       ...
 *       .registerAll();
 * </pre><p>
 *   In addition, a help command will generated for the current mod which can be
 *   accessed by running {@code /&lt;modid&gt; [<help>]} in-game.
 * <p>
 *   The generated help command will be automatically formatted so that each
 *   command will be displayed nicely on its given page. To customize this, call
 *   {@link #setUsageLineCount(int)}, {@link #setUsageLineLength(int)}, or even
 *   {@link #setUsageCommandCount(int)} to configure the generated output.
 * </p><pre>
 *     CommandRegistrationContext.forMod(MOD_DESCRIPTOR)
 *       ...
 *       .setUsageCmdCount(3)
 *       .setUsageLineLength(40)
 *       .registerAll();
 * </pre><p>
 *   While this context is active, the calling mod's {@link ModDescriptor} can be
 *   accessed by calling {@link #getActiveMod()} or {@link #getActiveModOrThrow()}.
 *   This method has been and can be leveraged behind the scenes to provide
 *   implicit access to mod-specific details in the current thread.
 * <p>
 *   For example, a few of the argument types provided by {@link CommandUtils}
 *   accept an optional second parameter for specifying the {@link ModDescriptor}.
 *   If this parameter is absent, it will be acquired implicitly from the active
 *   registration context.
 * </p>
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class CommandRegistrationContext {

    /** The text formatting to be used for the command usage header. */
    private static final Style HEADER_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GREEN)
        .withBold(true);

    /** The text formatting to be used for displaying command usage. */
    private static final Style USAGE_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GRAY);

    /** The text formatting to be used for displaying command arguments. */
    private static final Style ARGUMENT_STYLE = Style.EMPTY
        .withColor(TextColor.fromRgb(Color.LIGHT_GRAY.getRGB())).withItalic(true);

    /** The text formatting to be used for clickable next/previous page buttons. */
    private static final Style CLICKABLE_PAGE_STYLE = Style.EMPTY
        .withColor(ChatFormatting.WHITE);

    /** The text formatting to be used for un-clickable next/previous page buttons. */
    private static final Style UNCLICKABLE_PAGE_STYLE = Style.EMPTY
        .withColor(ChatFormatting.DARK_GRAY);

    /** A hover event indicating that a page button can be clicked. */
    private static final HoverEvent DISPLAY_CLICK_TO_OPEN =
        displayOnHover(Component.translatable("catlib.commands.help.page.hover"));

    /** The name of the help argument */
    private static final String HELP_ARGUMENT = "help";

    /** The name of the optional help page argument. */
    private static final String PAGE_ARGUMENT = "page";

    /** Whichever mod is currently registering commands in this thread. */
    private static final ThreadLocal<ModDescriptor> ACTIVE_MOD = new ThreadLocal<>();

    private final List<LibCommandBuilder> commands;
    private final ModDescriptor mod;
    private int usageLineLength;
    private int usageLineCount;
    private int usageCmdCount;

    private CommandRegistrationContext(final ModDescriptor mod) {
        this.commands = new ArrayList<>();
        this.mod = mod;
        this.usageLineLength = LibConfig.defaultUsageLineLength();
        this.usageLineCount = LibConfig.defaultUsageLineCount();
        this.usageCmdCount = LibConfig.defaultUsageCommandCount();
    }

    /**
     * Construct a new registration context when given the single required value
     * containing information about the current mod's ID, name, and config folders.
     * <p>
     *   Once the registration context has been constructed, this object cab be
     *   accessed again by calling {@link #getActiveMod()}. It will be available
     *   until this context has been disposed, which occurs when calling either
     *   {@link #registerAll()} or {@link #dispose()}.
     * <p>
     *   Consider storing this object in a static field for any method which
     *   requires it after the context has been disposed.
     * <p>
     *   Alternatively, the descriptor can be accessed by every command registered
     *   through this context wherever a {@link CommandContextWrapper} is provided.
     *  </p>
     *
     * @param mod Data about the mod registering commands.
     * @return A new registration context accepting mod command builders.
     */
    @CheckReturnValue
    public static CommandRegistrationContext forMod(final ModDescriptor mod) {
        return new CommandRegistrationContext(mod).setupActiveMod(mod);
    }

    /**
     * Takes care of establishing the active {@link ModDescriptor} in the current
     * thread. This method is called automatically wherever a new command registration
     * context is created.
     * <p>
     *   Note that if a {@link ModDescriptor} already exists in the current thread
     *   which does not match the provided descriptor, this is an error and the
     *   program cannot recover.
     * <p>
     *   You can avoid this problem by making sure to call either {@link #registerAll()}
     *   or {@link #dispose()};
     * </p>
     *
     * @throws AmbiguousContextException If a different mod has forgotten to dispose of
     *                                   its registration context.
     * @param mod The descriptor of the mod registering commands.
     * @return <code>this</code>, for method chaining.
     */
    private CommandRegistrationContext setupActiveMod(final ModDescriptor mod) {
        final ModDescriptor activeMod = ACTIVE_MOD.get();
        if (activeMod == null) {
            ACTIVE_MOD.set(mod);
        } else if (!mod.getModId().equals(activeMod.getModId())) {
            throw new AmbiguousContextException(activeMod.getModId(), mod.getModId());
        }
        return this;
    }

    /**
     * Provides the active mod's {@link ModDescriptor} which is available only
     * to the current thread. Note that once this context has been disposed,
     * the descriptor will no longer be available and this method will return
     * <code>null</code>.
     *
     * @return The active mod's {@link ModDescriptor}.
     */
    @Nullable
    public static ModDescriptor getActiveMod() {
        return ACTIVE_MOD.get();
    }

    /**
     * Variant of {@link #getActiveMod()} which asserts that an active registration
     * context must be available. If a context is not available, this is an error
     * and the program must exit.
     *
     * @return The active mod's {@link ModDescriptor}.
     */
    @NotNull
    public static ModDescriptor getActiveModOrThrow() {
        return Objects.requireNonNull(ACTIVE_MOD.get(), "Active mod queried out of context.");
    }

    /**
     * Adds a {@link LibCommandBuilder} to be processed by the wrapper. This builder
     * will be used to generate a help command. Each of the provided commands will be
     * registered behind the scenes along with the generated help command after calling
     * {@link #registerAll()}.
     *
     * @param command The builder for the command being added.
     * @return <code>this</code>, for method chaining.
     */
    public CommandRegistrationContext addCommand(final LibCommandBuilder command) {
        this.commands.add(command);
        return this;
    }

    /**
     * Variant of {@link #addCommand(LibCommandBuilder)} which can accept multiple
     * builders simultaneously. Note that each of these builders <b>must be generated
     * after the context has been constructed</b>.
     *
     * @param commands A series of command builders being added.
     * @return <code>this</code>, for method chaining.
     */
    public CommandRegistrationContext addAllCommands(final Collection<LibCommandBuilder> commands) {
        this.commands.addAll(commands);
        return this;
    }

    /**
     * Variant of {@link #addAllCommands(Collection)} which accepts a series of classes
     * containing methods annotated with {@link ModCommand} and {@link CommandBuilder}.
     * Note that each of these methods is allowed to be private, but <b>must be static</b>.
     * <p>
     *   See the respective classes for more information on using these annotations.
     * </p>
     *
     * @param classes A series of class files containing the annotated methods.
     * @return <code>this</code>, for method chaining.
     */
    public CommandRegistrationContext addAllCommands(final Class<?>... classes) {
        this.commands.addAll(CommandClassEvaluator.getBuilders(this.mod, classes));
        return this;
    }

    /**
     * Adds every command declared in the class of the given object. This allows command
     * classes to use instance methods while also supporting multi-arg constructors.
     *
     * @param instances The instance of a class containing annotated command definitions.
     * @return <code>this</code>, for method chaining.
     */
    public CommandRegistrationContext addAllCommands(final Object... instances) {
        this.commands.addAll(CommandClassEvaluator.getBuilders(this.mod, instances));
        return this;
    }

    /**
     * Adds the default set of commands provided by the library as sub commands of the
     * current mod's main command. Note that every command accepting a file argument
     * will be adjusted for the current mod according to its {@link ModDescriptor}.
     *
     * @return <code>this</code>, for method chaining.
     */
    public CommandRegistrationContext addLibCommands() {
        this.commands.addAll(DefaultLibCommands.createAll(this.mod));
        return this;
    }

    /**
     * Sets the maximum number of characters per line to be displayed in the output
     * of this mod's generated help command.
     *
     * @param length The maximum number of characters per line.
     * @return <code>this</code>, for method chaining.
     */
    public CommandRegistrationContext setUsageLineLength(final int length) {
        this.usageLineLength = Math.min(100, Math.max(10, length));
        return this;
    }

    /**
     * Sets the maximum number of lines to be displayed per page of this mod's
     * generated help command. If number of lines exceeds this limit, the <b>next</b>
     * command will be moved onto the following page.
     *
     * @param count The maximum number of lines per page.
     * @return <code>this</code>, for method chaining.
     */
    public CommandRegistrationContext setUsageLineCount(final int count) {
        this.usageLineCount = Math.min(90, Math.max(1, count));
        return this;
    }

    /**
     * Sets the maximum number of commands per page of this mod's generated help
     * command. Regardless of the line count, once the allotted number of commands
     * have been displayed on the current page, the following commands will be moved
     * onto the next page.
     *
     * @param count The maximum number of commands per page.
     * @return <code>this</code>, for method chaining.
     */
    public CommandRegistrationContext setUsageCommandCount(final int count) {
        this.usageCmdCount = Math.min(30, Math.max(1, count));
        return this;
    }

    /**
     * Generates this mod's help command and registers <b>all</b> of the commands
     * in this context with {@link LibCommandRegistrar}.
     * <p>
     *   After the commands have been registered, the active mod descriptor will be
     *   disposed of and can no longer be accessed.
     * </p>
     */
    public void registerAll() {
        final List<LibCommandBuilder> commands = this.getCommandsForCurrentSide();
        final LiteralArgumentBuilder<CommandSourceStack> modCommand = this.generateBasicModCommand(commands);

        for (final LibCommandBuilder command : commands) {
            if (command.getHelpInfo().isGlobal()) {
                LibCommandRegistrar.registerCommand(command.getCommand(), command.getSide());
            } else {
                modCommand.then(command.getCommand());
            }
        }
        LibCommandRegistrar.registerCommand(modCommand);
        this.commands.clear();
        this.dispose();
    }

    /**
     * Manually clears the active mod descriptor from the current thread. After this
     * point, the mod's descriptor can no longer be accessed through the library except
     * when provided a {@link CommandContextWrapper}.
     */
    public void dispose() {
        ACTIVE_MOD.remove();
    }

    /**
     * Generates a list of every {@link LibCommandBuilder} which is valid for the current
     * server side.
     *
     * @return Every {@link LibCommandBuilder} applicable for the current context.
     */
    public List<LibCommandBuilder> getCommandsForCurrentSide() {
        final boolean dedicated = McUtils.isDedicatedServer();
        return this.commands.stream()
            .filter(builder -> builder.getSide().canRegister(dedicated))
            .collect(Collectors.toList());
    }

    /**
     * Generates a basic {@link LiteralArgumentBuilder} named according to this mod's
     * command prefix, which is provided by its {@link ModDescriptor}.
     *
     * @param commands Every command applicable to the current server side.
     * @return A generated {@link LiteralArgumentBuilder} for the current mod.
     */
    private LiteralArgumentBuilder<CommandSourceStack> generateBasicModCommand(final List<LibCommandBuilder> commands) {
        final CommandFunction helpCommand = this.createHelp(
            new Lazy<>(() -> this.createHelpMessage(commands)));
        return LibCommandBuilder.named(this.mod.getCommandPrefix())
            .generate((builder, utl) -> {
                final Command<CommandSourceStack> cmd = utl.wrap(helpCommand);
                return builder.executes(cmd)
                    .then(literal(HELP_ARGUMENT).executes(cmd)
                        .then(arg(PAGE_ARGUMENT, 1, Integer.MAX_VALUE).executes(cmd)));
            }).getCommand();
    }

    /**
     * Generates the basic help command to used by the current mod.
     *
     * @param pageSupplier Generates text components corresponding to each help page.
     * @return The command to be executed when provided an optional page number.
     */
    private CommandFunction createHelp(final Supplier<List<Component>> pageSupplier) {
        return ctx -> {
            final int page = ctx.getOptional(PAGE_ARGUMENT, Integer.class).orElse(1) - 1;
            final List<Component> pages = pageSupplier.get();
            if (page < 0 || page >= pages.size()) {
                ctx.sendError("Invalid page number. Must be {} to {}", 1, pages.size());
                return;
            }
            ctx.sendMessage(() -> pages.get(page));
        };
    }

    /**
     * Generates a list of help pages when provided every command that is valid
     * for the current server side.
     *
     * @param commands Every command builder applicable for the current context.
     * @return The generated list of formatted help pages.
     */
    private List<Component> createHelpMessage(final List<LibCommandBuilder> commands) {
        final List<List<HelpCommandInfo>> pages = this.createHelpPages(commands);
        final List<Component> messages = new ArrayList<>();
        final boolean anyGlobal = this.isAnyGlobal(commands);

        for (int i = 0; i < pages.size(); i++) {
            final List<HelpCommandInfo> page = pages.get(i);
            final MutableComponent message = Component.empty(); // No formatting.
            final Component header = this.createHeader(i + 1, pages.size());
            message.append(header);

            for (final HelpCommandInfo info : page) {
                message.append("\n").append(this.createUsageText(anyGlobal, info));
            }
            message.append("\n").append(this.createFooter(header, i + 1, pages.size()));
            messages.add(message);
        }
        return messages;
    }

    /**
     * Sorts every {@link HelpCommandInfo} container from a list of command builders.
     *
     * @param commands Every command builder applicable for the current context.
     * @return The sorted pages of help info.
     */
    private List<List<HelpCommandInfo>> createHelpPages(final List<LibCommandBuilder> commands) {
        final List<List<HelpCommandInfo>> pages = new ArrayList<>();
        List<HelpCommandInfo> currentPage = new ArrayList<>();
        int numLines = 0;
        int numCommands = 0;

        final Iterator<LibCommandBuilder> iterator = commands.iterator();
        while (iterator.hasNext()) {
            final HelpCommandInfo info = iterator.next().getHelpInfo();
            if (info.getDescription().isEmpty()) {
                continue;
            }
            final String description = Language.getInstance().getOrDefault(info.getDescription());
            currentPage.add(info);
            numLines += 2 + description.length() / this.usageLineLength;
            numCommands++;
            if (iterator.hasNext() && numLines >= this.usageLineCount || numCommands >= this.usageCmdCount) {
                pages.add(currentPage);
                currentPage = new ArrayList<>();
                numLines = 0;
                numCommands = 0;
            }
        }
        pages.add(currentPage);
        return pages;
    }

    /**
     * Determines whether any command in the given list should be applied to
     * the global root command node.
     *
     * @param commands Every command applicable for the current context.
     * @return whether any command in the list is global.
     */
    private boolean isAnyGlobal(final List<LibCommandBuilder> commands) {
        return commands.stream().anyMatch(c -> c.getHelpInfo().isGlobal());
    }

    /**
     * Generates the header text to be rendered at the top of the current help page.
     *
     * @param page The current page number.
     * @param numPages The last page number.
     * @return The formatted header as a {@link Component}.
     */
    private Component createHeader(final int page, final int numPages) {
        return Component.literal(" --- ")
            .append(Component.translatable("catlib.commands.help.usage", this.mod.getName()))
            .append(f(" ({} / {}) ---", page, numPages))
            .withStyle(HEADER_STYLE);
    }

    /**
     * Generates the footer text to be rendered at the bottom of the current help page.
     *
     * @param header The previously-generated header component
     * @param page The current page number
     * @param numPages The last page number
     * @return The formatted footer as a {@link Component}.
     */
    private Component createFooter(final Component header, final int page, final int numPages) {
        // --- [previous] ${repeat('', max(3, len(header - rest)))} [next] ---
        final String headerText = header.getString();
        final String previousText = Language.getInstance().getOrDefault("catlib.commands.help.previous");
        final String nextText = Language.getInstance().getOrDefault("catlib.commands.help.next");
        final int footerLen = "--- []  [] ---".length() + previousText.length() + nextText.length();

        return Component.literal(" --- ")
            .append(this.createPreviousButton(previousText, page))
            .append(" ")
            .append("-".repeat(Math.max(3, headerText.length() - footerLen)))
            .append(" ")
            .append(this.createNextButton(nextText, page, numPages))
            .append(" ---")
            .withStyle(HEADER_STYLE);
    }

    /**
     * Generates the previous page button at the bottom of each help page.
     *
     * @param text The text to display on the button
     * @param page The current page number
     * @return The formatted, clickable previous page button
     */
    private Component createPreviousButton(final String text, final int page) {
        final MutableComponent component = Component.literal("[").append(text).append("]");
        if (page > 1) {
            final String cmd = f("/{} help {}", this.mod.getCommandPrefix(), page - 1);
            component.setStyle(
                CLICKABLE_PAGE_STYLE.withClickEvent(clickToRun(cmd)).withHoverEvent(DISPLAY_CLICK_TO_OPEN));
        } else {
            component.setStyle(UNCLICKABLE_PAGE_STYLE);
        }
        return component;
    }

    /**
     * Generates the next page button at the bottom of each help page.
     *
     * @param text The text to display on the button
     * @param page The current page number
     * @param numPages The last page number
     * @return The formatted, clickable next page button
     */
    private Component createNextButton(final String text, final int page, final int numPages) {
        final MutableComponent component = Component.literal("[").append(text).append("]");
        if (page < numPages) {
            final String cmd = f("/{} help {}", this.mod.getCommandPrefix(), page + 1);
            component.setStyle(
                CLICKABLE_PAGE_STYLE.withClickEvent(clickToRun(cmd)).withHoverEvent(DISPLAY_CLICK_TO_OPEN));
        } else {
            component.setStyle(UNCLICKABLE_PAGE_STYLE);
        }
        return component;
    }

    /**
     * Generates the formatted usage text for a single command's {@link HelpCommandInfo}.
     * If any command in the context is global, every non-global command will be rendered
     * with <code>/&lt;modid&gt;</code> in the title.
     *
     * @param anyGlobal Whether any command in the context is a global command.
     * @param info The raw help info corresponding to the current command.
     * @return The formatted help info as a {@link Component}.
     */
    private Component createUsageText(final boolean anyGlobal, final HelpCommandInfo info) {
        final MutableComponent text = Component.empty();
        if (!info.isGlobal() && anyGlobal) {
            text.append(this.mod.getCommandPrefix() + " ");
        }
        text.append(info.getName());
        if (!info.getArguments().isEmpty()) {
            text.append(Component.literal(" " + info.getArguments()).withStyle(ARGUMENT_STYLE));
        }
        final String description = Language.getInstance().getOrDefault(info.getDescription());
        final List<String> lines = LibStringUtils.wrapLines(description, this.usageLineLength);
        text.append(Component.literal(" :\n " + lines.getFirst()).setStyle(USAGE_STYLE));
        for (int i = 1; i < lines.size(); i++) {
            text.append(Component.literal("\n  " + lines.get(i)).setStyle(USAGE_STYLE));
        }
        return text;
    }

    private static class AmbiguousContextException extends RuntimeException {
        AmbiguousContextException(final String... ids) {
            super("Multiple contexts in the current thread. Call #registerAll: " + Arrays.toString(ids));
        }
    }
}

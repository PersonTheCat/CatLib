package personthecat.catlib.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.LibStringUtils;
import personthecat.catlib.util.McTools;
import personthecat.fresult.Result;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.literal;
import static personthecat.catlib.util.Shorthand.f;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@ParametersAreNonnullByDefault
public class CommandRegistrationContext {

    /** The text formatting to be used for the command usage header. */
    private static final Style HEADER_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GREEN)
        .withBold(true);

    /** The text formatting to be used for displaying command usage. */
    private static final Style USAGE_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GRAY);

    /** The header to be used by the help message /  usage text. */
    private static final String USAGE_HEADER = " --- {} Command Usage ({} / {}) ---";

    /** The default number of characters per line on the help page. */
    private static final int DEFAULT_USAGE_LINE_LENGTH = 60;

    /** The default number of lines before any help page is forcibly wrapped. */
    private static final int DEFAULT_USAGE_LINE_COUNT = 15;

    /** The default preferred number of commands to display per help page. */
    private static final int DEFAULT_USAGE_CMD_COUNT = 5;

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
        this.usageLineLength = DEFAULT_USAGE_LINE_LENGTH;
        this.usageLineCount = DEFAULT_USAGE_LINE_COUNT;
        this.usageCmdCount = DEFAULT_USAGE_CMD_COUNT;
    }

    @CheckReturnValue
    public static CommandRegistrationContext forMod(final ModDescriptor mod) {
        return new CommandRegistrationContext(mod).setupActiveMod(mod);
    }

    private CommandRegistrationContext setupActiveMod(final ModDescriptor mod) {
        final ModDescriptor activeMod = ACTIVE_MOD.get();
        if (activeMod == null) {
            ACTIVE_MOD.set(mod);
        } else if (!mod.getModId().equals(activeMod.getModId())) {
            throw new AmbiguousContextException(activeMod.getModId(), mod.getModId());
        }
        return this;
    }

    @Nullable
    public static ModDescriptor getActiveMod() {
        return ACTIVE_MOD.get();
    }

    public static ModDescriptor getActiveModOrThrow() {
        return Objects.requireNonNull(ACTIVE_MOD.get(), "Active mod queried out of context.");
    }

    public CommandRegistrationContext addCommand(final LibCommandBuilder cmd) {
        this.commands.add(cmd);
        return this;
    }

    public CommandRegistrationContext addCommands(final Class<?>... classes) {
        this.commands.addAll(CommandClassEvaluator.getBuilders(classes));
        return this;
    }

    public CommandRegistrationContext addLibCommands() {
        DefaultLibCommands.createAll(this.mod, false).forEach(this::addCommand);
        return this;
    }

    public CommandRegistrationContext setUsageLineLength(final int length) {
        this.usageLineLength = Math.min(100, Math.max(10, length));
        return this;
    }

    public CommandRegistrationContext setUsageLineCount(final int count) {
        this.usageLineCount = Math.min(90, Math.max(1, count));
        return this;
    }

    public CommandRegistrationContext setUsageCmdCount(final int count) {
        this.usageCmdCount = Math.min(30, Math.max(1, count));
        return this;
    }

    public CommandRegistrationContext registerAll() {
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
        return this.dispose();
    }

    public CommandRegistrationContext dispose() {
        ACTIVE_MOD.remove();
        return this;
    }

    public List<LibCommandBuilder> getCommandsForCurrentSide() {
        final boolean dedicated = McTools.isDedicatedServer();
        return this.commands.stream()
            .filter(builder -> builder.getSide().canRegister(dedicated))
            .collect(Collectors.toList());
    }

    private LiteralArgumentBuilder<CommandSourceStack> generateBasicModCommand(final List<LibCommandBuilder> commands) {
        final List<TextComponent> message = this.createHelpMessage(commands);
        final Command<CommandSourceStack> helpCommand = this.executeHelp(message);

        return literal(this.mod.getCommandPrefix()).executes(helpCommand)
            .then(literal(HELP_ARGUMENT).executes(helpCommand)
                .then(CommandUtils.arg(PAGE_ARGUMENT, 1, message.size()).executes(helpCommand)));
    }

    private Command<CommandSourceStack> executeHelp(final List<TextComponent> message) {
        return ctx -> {
            final CommandSourceStack source = ctx.getSource();
            source.sendSuccess(message.get(this.getHelpPage(ctx) - 1), true);
            return 0;
        };
    }

    private int getHelpPage(final CommandContext<CommandSourceStack> ctx) {
        return Result.suppress(() -> ctx.getArgument(PAGE_ARGUMENT, Integer.class))
            .resolve(e -> 1)
            .expose();
    }

    private List<TextComponent> createHelpMessage(final List<LibCommandBuilder> commands) {
        final List<List<HelpCommandInfo>> pages = this.createHelpPages(commands);
        final List<TextComponent> messages = new ArrayList<>();
        final boolean anyGlobal = this.isAnyGlobal(commands);

        for (int i = 0; i < pages.size(); i++) {
            final List<HelpCommandInfo> page = pages.get(i);
            final TextComponent message = new TextComponent(""); // No formatting.
            message.append(this.createHeader(i + 1, pages.size()));

            for (final HelpCommandInfo info : page) {
                message.append("\n").append(this.createUsageText(anyGlobal, info));
            }
            messages.add(message);
        }
        return messages;
    }

    private List<List<HelpCommandInfo>> createHelpPages(final List<LibCommandBuilder> commands) {
        final List<List<HelpCommandInfo>> pages = new ArrayList<>();
        List<HelpCommandInfo> currentPage = new ArrayList<>();
        int numLines = 0;
        int numCommands = 0;

        for (final LibCommandBuilder command : commands) {
            final HelpCommandInfo info = command.getHelpInfo();
            if (info.getArguments().isEmpty() && info.getDescription().isEmpty()) {
                continue;
            }
            currentPage.add(info);
            numLines += 2 + StringUtils.countMatches(info.getDescription(), '\n');
            numCommands++;
            if (numLines >= this.usageLineCount || numCommands >= this.usageCmdCount) {
                pages.add(currentPage);
                currentPage = new ArrayList<>();
                numLines = 0;
                numCommands = 0;
            }
        }
        pages.add(currentPage);
        return pages;
    }

    private boolean isAnyGlobal(final List<LibCommandBuilder> commands) {
        return commands.stream().anyMatch(c -> c.getHelpInfo().isGlobal());
    }

    private TextComponent createHeader(final int page, final int numPages) {
        return (TextComponent) new TextComponent(f(USAGE_HEADER, this.mod.getName(), page, numPages))
            .setStyle(HEADER_STYLE);
    }

    private TextComponent createUsageText(final boolean anyGlobal, final HelpCommandInfo info) {
        final String prefix = info.isGlobal() || !anyGlobal ? "" : this.mod.getCommandPrefix() + " ";
        final String command = prefix + info.getName() + " " + info.getArguments();

        final TextComponent text = new TextComponent(command);
        final List<String> lines = LibStringUtils.wrapLines(info.getDescription(), this.usageLineLength);
        text.append(new TextComponent(" :\n " + lines.get(0)).setStyle(USAGE_STYLE));
        for (int i = 1; i < lines.size(); i++) {
            text.append(new TextComponent("\n  " + lines.get(i)).setStyle(USAGE_STYLE));
        }
        return text;
    }

    private static class AmbiguousContextException extends RuntimeException {
        AmbiguousContextException(final String... ids) {
            super("Multiple contexts in the current thread. Call #registerAll: " + Arrays.toString(ids));
        }
    }
}

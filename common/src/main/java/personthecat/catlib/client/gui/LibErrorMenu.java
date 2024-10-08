package personthecat.catlib.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.data.collections.MultiValueHashMap;
import personthecat.catlib.data.collections.MultiValueMap;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.FormattedException;

import java.util.*;

public class LibErrorMenu extends LibMenu {

    private static final int PADDING = 6;
    private static final int MAX_SIZE = 756;
    private static final int MID_SIZE = 596;

    private final Map<ModDescriptor, CategorizedList> options;
    private final MultiValueMap<ModDescriptor, FormattedException> errors;
    private final Set<FormattedException> fatal;
    private final List<ModDescriptor> keys;
    private final int[] screens;
    private @Nullable CategorizedList current;
    private @Nullable CategorizedList mods;
    private int page;

    public LibErrorMenu(@Nullable final Screen parent) {
        super(parent, Component.translatable("catlib.errorMenu.numErrors", LibErrorContext.numMods()));
        this.options = new HashMap<>();
        this.errors = new MultiValueHashMap<>();
        this.fatal = new HashSet<>();
        this.screens = new int[LibErrorContext.numMods()];
        this.keys = LibErrorContext.getMods();
        this.current = null;
        this.mods = null;
        this.page = 0;
        this.loadErrors();
    }

    private void loadErrors() {
        this.errors.putAll(LibErrorContext.getCommon());
        LibErrorContext.getFatal().forEach((m, l) -> l.forEach(e -> {
            this.errors.add(m, e);
            this.fatal.add(e);
        }));
        Arrays.fill(this.screens, 0);
    }

    @Override
    protected void init() {
        super.init();

        this.updateButtons();
        this.rebuildOptions();
        this.current = this.options.get(this.keys.get(this.page));
        this.mods = new CategorizedList(this, this.getModMenuLeft(), this.getModMenuRight(), this.createModButtons());
        this.mods.deselectAll();
        this.mods.selectButton(this.page);
        if (this.current != null) {
            this.addRenderableWidget(this.current);
        }
        this.addRenderableWidget(this.mods);
        this.setFocused(this.current);
    }

    private void updateButtons() {
        if (this.keys.size() == 1) {
            this.previous.active = false;
            this.next.active = false;
        }
    }

    private void rebuildOptions() {
        this.options.clear();
        int page = 0;
        for (final Map.Entry<ModDescriptor, List<FormattedException>> entry : this.errors.entrySet()) {
            final MultiValueMap<String, FormattedException> sorted = sortExceptions(entry.getValue());
            final CategorizedList list = new CategorizedList(this, this.getErrorMenuLeft(), this.getErrorMenuRight(), this.createErrorButtons(page, sorted));
            this.options.put(entry.getKey(), list);
            page++;
        }
    }

    private int getModMenuLeft() {
        return this.width < MAX_SIZE ? PADDING : this.width / 2 - (MID_SIZE / 2);
    }

    private int getModMenuRight() {
        return this.width / 2;
    }

    private int getErrorMenuLeft() {
        return this.width / 2 - PADDING;
    }

    private int getErrorMenuRight() {
        return this.width < MAX_SIZE ? this.width : this.width / 2 + (MID_SIZE / 2);
    }

    private MultiValueMap<String, AbstractWidget> createModButtons() {
        final MultiValueMap<String, AbstractWidget> buttons = new MultiValueHashMap<>();
        final List<AbstractWidget> widgets = new ArrayList<>();
        for (final ModDescriptor mod : this.keys) {
            widgets.add(CategorizedList.createButton(Component.literal(mod.name()), b -> this.setMod(mod)));
        }
        buttons.put("catlib.errorMenu.mods", widgets);
        return buttons;
    }

    private MultiValueMap<String, FormattedException> sortExceptions(List<FormattedException> exceptions) {
        final MultiValueMap<String, FormattedException> sorted = new MultiValueHashMap<>();
        for (final FormattedException exception : exceptions) {
            sorted.add(exception.getCategory(), exception);
        }
        return sorted;
    }

    private MultiValueMap<String, AbstractWidget> createErrorButtons(int page, MultiValueMap<String, FormattedException> sorted) {
        final MultiValueMap<String, AbstractWidget> buttons = new MultiValueHashMap<>();

        for (final Map.Entry<String, List<FormattedException>> entry : sorted.entrySet()) {
            final List<AbstractWidget> widgets = new ArrayList<>();

            for (int j = 0; j < entry.getValue().size(); j++) {
                final FormattedException e = entry.getValue().get(j);
                final int screen = j;

                Component display = e.getDisplayMessage();
                if (display instanceof MutableComponent && this.fatal.contains(e)) {
                    display = ((MutableComponent) display).withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                }
                final Component tooltipMessage = e.getTooltip();
                final Tooltip tooltip = tooltipMessage != null ? Tooltip.create(tooltipMessage) : null;

                final Button.OnPress onPress = b -> {
                    this.screens[page] = screen;
                    Minecraft.getInstance().setScreen(e.getDetailsScreen(this));
                };

                widgets.add(CategorizedList.createButton(display, onPress, tooltip));
            }
            buttons.put(entry.getKey(), widgets);
        }
        return buttons;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void renderMenu(GuiGraphics graphics, int x, int y, float partial) {
        this.current.render(graphics, x, y, partial);
        this.mods.render(graphics, x, y, partial);
    }

    public boolean hasPreviousError() {
        final List<FormattedException> list = this.getCurrentScreens();
        int screen = this.screens[this.page];
        return screen != decrementWrapping(screen, list.size());
    }

    public boolean hasNextError() {
        final List<FormattedException> list = this.getCurrentScreens();
        int screen = this.screens[this.page];
        return screen != incrementWrapping(screen, list.size());
    }

    @Nullable
    public Screen previousError() {
        final List<FormattedException> list = this.getCurrentScreens();
        int screen = this.screens[this.page];
        int previous = decrementWrapping(screen, list.size());
        if (screen != previous) {
            this.screens[this.page] = previous;

            final FormattedException e = list.get(previous);
            return e.getDetailsScreen(this);
        }
        return null;
    }

    @Nullable
    public Screen nextError() {
        final List<FormattedException> list = this.getCurrentScreens();
        int screen = this.screens[this.page];
        int next = incrementWrapping(screen, list.size());
        if (screen != next) {
            this.screens[this.page] = next;

            final FormattedException e = list.get(next);
            return e.getDetailsScreen(this);
        }
        return null;
    }

    private List<FormattedException> getCurrentScreens() {
        return this.errors.get(this.keys.get(this.page));
    }

    @Override
    protected void onPrevious() {
        this.page = decrementWrapping(this.page, this.keys.size());
        this.updateMod(this.keys.get(this.page));
    }

    @Override
    protected void onNext() {
        this.page = incrementWrapping(this.page, this.keys.size());
        this.updateMod(this.keys.get(this.page));
    }

    @Override
    public void onClose() {
        if (LibErrorContext.isFatal()) {
            Minecraft.getInstance().close();
        }
        LibErrorContext.dispose();
        super.onClose();
    }

    private static int decrementWrapping(int num, int max) {
        return num <= 0 ? max - 1 : num - 1;
    }

    private static int incrementWrapping(int num, int max) {
        return num >= max - 1 ? 0 : num + 1;
    }

    private void setMod(ModDescriptor mod) {
        this.page = this.keys.indexOf(mod);
        this.updateMod(mod);
    }

    @SuppressWarnings("unchecked")
    private void updateMod(ModDescriptor mod) {
        this.children().remove(this.current);
        this.current = this.options.get(mod);
        ((List<GuiEventListener>) this.children()).add(this.current);
        this.setFocused(this.current);

        if (this.mods != null) {
            this.mods.deselectAll();
            this.mods.selectButton(this.page);
        }
    }
}

package personthecat.catlib.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.data.MultiValueHashMap;
import personthecat.catlib.data.MultiValueMap;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.FormattedException;

import java.util.*;

public class LibErrorMenu extends LibMenu {

    private static final int PADDING = 6;
    private static final int MAX_SIZE = 756;
    private static final int MID_SIZE = 596;

    private final Map<ModDescriptor, CategorizedList> options;
    private final MultiValueMap<ModDescriptor, FormattedException> errors;
    private final List<ModDescriptor> keys;
    private final int[] screens;
    private @Nullable CategorizedList current;
    private @Nullable CategorizedList mods;
    private int page;

    public LibErrorMenu(@Nullable final Screen parent) {
        super(parent, new TranslatableComponent("catlib.errorMenu.numErrors", LibErrorContext.numMods()));
        this.options = new HashMap<>();
        this.errors = new MultiValueHashMap<>();
        this.screens = new int[LibErrorContext.numMods()];
        this.keys = LibErrorContext.getMods();
        this.current = null;
        this.mods = null;
        this.page = 0;
        this.loadErrors();
    }

    private void loadErrors() {
        this.errors.putAll(LibErrorContext.getCommon());
        LibErrorContext.getFatal().forEach((m, l) -> l.forEach(e -> this.errors.add(m, e)));
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
        this.children.add(this.current);
        this.children.add(this.mods);
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
        for (final Map.Entry<ModDescriptor, List<FormattedException>> entry : this.errors.entrySet()) {
            final MultiValueMap<String, FormattedException> sorted = sortExceptions(entry.getValue());
            final CategorizedList list = new CategorizedList(this, this.getErrorMenuLeft(), this.getErrorMenuRight(), createErrorButtons(sorted));
            this.options.put(entry.getKey(), list);
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
            widgets.add(CategorizedList.createButton(new TextComponent(mod.getName()), b -> this.setMod(mod)));
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

    private MultiValueMap<String, AbstractWidget> createErrorButtons(MultiValueMap<String, FormattedException> sorted) {
        final MultiValueMap<String, AbstractWidget> buttons = new MultiValueHashMap<>();
        int i = 0;

        for (final Map.Entry<String, List<FormattedException>> entry : sorted.entrySet()) {
            final List<AbstractWidget> widgets = new ArrayList<>();

            for (int j = 0; j < entry.getValue().size(); j++) {
                final FormattedException e = entry.getValue().get(j);
                final int page = i;
                final int screen = j;

                final Component display = e.getDisplayMessage();
                final Component tooltip = e.getTooltip();

                final Button.OnPress onPress = b -> {
                    this.screens[page] = screen;
                    Minecraft.getInstance().setScreen(e.getDetailsScreen(this));
                };

                final Button.OnTooltip onTooltip = tooltip == null ? Button.NO_TOOLTIP :
                    (b, stack, x, y) -> this.renderTooltip(stack, tooltip, x, y);

                widgets.add(CategorizedList.createButton(display, onPress, onTooltip));
            }
            buttons.put(entry.getKey(), widgets);
            i++;
        }
        return buttons;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void renderMenu(PoseStack stack, int x, int y, float partial) {
        this.current.render(stack, x, y, partial);
        this.mods.render(stack, x, y, partial);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void renderDetails(PoseStack stack, int x, int y, float partial) {
        super.renderDetails(stack, x, y, partial);
        this.mods.renderTooltips(stack, x, y);
        this.current.renderTooltips(stack, x, y);
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

    private void updateMod(ModDescriptor mod) {
        this.children.remove(this.current);
        this.current = this.options.get(mod);
        this.children.add(this.current);
        this.setFocused(this.current);

        if (this.mods != null) {
            this.mods.deselectAll();
            this.mods.selectButton(this.page);
        }
    }
}

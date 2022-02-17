package personthecat.catlib.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class ErrorDetailPage extends SimpleTextPage {

    public ErrorDetailPage(@Nullable Screen parent, Component title, Component details) {
        super(parent, title, details);
    }

    @Override
    protected void init() {
        super.init();

        if (this.parent instanceof LibErrorMenu) {
            final LibErrorMenu menu = (LibErrorMenu) this.parent;
            this.previous.active = menu.hasPreviousError();
            this.next.active = menu.hasNextError();
        }
    }

    @Override
    protected void onPrevious() {
        if (this.parent instanceof LibErrorMenu) {
            final LibErrorMenu menu = (LibErrorMenu) this.parent;
            Minecraft.getInstance().setScreen(menu.previousError());
        }
    }

    @Override
    protected void onNext() {
        if (this.parent instanceof LibErrorMenu) {
            final LibErrorMenu menu = (LibErrorMenu) this.parent;
            Minecraft.getInstance().setScreen(menu.nextError());
        }
    }
}

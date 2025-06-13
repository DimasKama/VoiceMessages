package ru.dimaskama.voicemessages.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.dimaskama.voicemessages.mixin.client.ScreenAccessor;

public abstract class OverlayScreen extends Screen {

    public final Screen parent;

    protected OverlayScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        ((ScreenAccessor) parent).voicemessages_init();
        super.init();
    }

    @Override
    protected void refreshWidgetPositions() {
        ((ScreenAccessor) parent).voicemessages_refreshWidgetPositions();
        super.refreshWidgetPositions();
    }

    @Override
    public void tick() {
        parent.tick();
        super.tick();
    }

    @Override
    public void onDisplayed() {
        parent.onDisplayed();
        super.onDisplayed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        parent.render(context, mouseX, mouseY, delta);
        actualRender(context, mouseX, mouseY, delta);
    }

    protected void actualRender(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        super.removed();
        parent.removed();
    }

}

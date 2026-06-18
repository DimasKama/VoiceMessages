package ru.dimaskama.voicemessages.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dimaskama.voicemessages.mixin.client.ScreenAccessor;

public abstract class OverlayScreen extends Screen {

    public final Screen parent;
    private boolean firstInit = true;

    protected OverlayScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (firstInit) {
            parent.init(width, height);
            firstInit = false;
        } else {
            ((ScreenAccessor) parent).voicemessages_init();
        }
        super.init();
    }

    @Override
    protected void rebuildWidgets() {
        ((ScreenAccessor) parent).voicemessages_rebuildWidgets();
        super.rebuildWidgets();
    }

    @Override
    public void tick() {
        parent.tick();
        super.tick();
    }

    @Override
    public void added() {
        parent.added();
        super.added();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        parent.extractRenderState(guiGraphics, mouseX, mouseY, delta);
        actualRender(guiGraphics, mouseX, mouseY, delta);
    }

    protected void actualRender(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        super.removed();
        parent.removed();
    }

}

package ru.dimaskama.voicemessages.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.voicemessages.*;
import ru.dimaskama.voicemessages.client.Playback;
import ru.dimaskama.voicemessages.client.PlaybackManager;
import ru.dimaskama.voicemessages.client.PlaybackPlayer;
import ru.dimaskama.voicemessages.networking.VoiceMessageChunkC2S;
import ru.dimaskama.voicemessages.networking.VoiceMessageEndC2S;

import java.util.List;

public class VoiceMessageConfirmScreen extends OverlayScreen {

    private static final WidgetSprites SEND_SPRITES = new WidgetSprites(
            VoiceMessagesMod.id("send"),
            VoiceMessagesMod.id("send_hovered")
    );
    private final int leftX;
    private final int fromBottomY;
    private final Playback playback;
    private final String target;
    @Nullable
    private final Component targetText;
    private int targetTextX;
    private int targetTextY;
    private PlaybackPlayer playbackPlayer;

    public VoiceMessageConfirmScreen(Screen parent, int leftX, int fromBottomY, List<short[]> audio, String target) {
        super(Component.translatable("voicemessages.confirm"), parent);
        this.leftX = leftX;
        this.fromBottomY = fromBottomY;
        playback = new Playback(audio);
        this.target = target;
        targetText = getScreenTargetText(target);
    }

    @Override
    protected void init() {
        super.init();
        int bottomY = height - fromBottomY;
        if (playbackPlayer == null) {
            playbackPlayer = new PlaybackPlayer(PlaybackManager.MAIN, playback, 0xFFAAAAAA);
            playbackPlayer.setRectangle(leftX + 1, bottomY - 15, 260, 15);
        }
        ImageButton sendButton = addRenderableWidget(new ImageButton(
                leftX + 265,
                bottomY - 15,
                14,
                14,
                SEND_SPRITES,
                button -> {
                    send();
                    onClose();
                }
        ));
        sendButton.setTooltip(Tooltip.create(Component.translatable("voicemessages.send")));
        ImageButton cancelButton = addRenderableWidget(new ImageButton(
                leftX + 281,
                bottomY - 15,
                14,
                14,
                RecordVoiceMessageScreen.CANCEL_SPRITES,
                button -> onClose()
        ));
        cancelButton.setTooltip(Tooltip.create(CommonComponents.GUI_CANCEL));
        targetTextX = leftX + 300;
        targetTextY = bottomY - 12;
    }

    private void send() {
        List<short[]> audio = playback.getAudio();
        OpusEncoder encoder = VoiceMessagesPlugin.getClientOpusEncoder();
        encoder.resetState();
        VoiceMessagesModService service = VoiceMessagesMod.getService();
        for (VoiceMessageChunkC2S chunk : VoiceMessageChunkC2S.split(VoiceMessagesPlugin.encodeList(encoder, audio))) {
            service.sendToServer(chunk);
        }
        service.sendToServer(new VoiceMessageEndC2S(target));
        VoiceMessages.getLogger().info("Sent voice message (" + (1000 * audio.size() / VoiceMessages.FRAMES_PER_SEC) + "ms)");
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void actualRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 150.0F);
        super.actualRender(guiGraphics, mouseX, mouseY, delta);
        playbackPlayer.render(guiGraphics);
        if (targetText != null) {
            guiGraphics.drawString(font, targetText, targetTextX, targetTextY, 0xFFFFFFFF);
        }
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            send();
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (playbackPlayer.mouseClicked((int) mouseX, (int) mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        super.removed();
        PlaybackManager.MAIN.stopPlaying();
        PlaybackManager.MAIN.remove(playback);
    }

    public static Component getTargetText(String target) {
        String key = "voicemessages.target." + target;
        return Component.literal(I18n.exists(key) ? I18n.get(key) : target);
    }

    @Nullable
    public static Component getScreenTargetText(String target) {
        return VoiceMessages.TARGET_ALL.equals(target)
                ? null
                : Component.translatable("voicemessages.to", VoiceMessageConfirmScreen.getTargetText(target));
    }

}

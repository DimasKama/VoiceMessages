package ru.dimaskama.voicemessages.client.screen;

import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.VoiceMessagesPlugin;
import ru.dimaskama.voicemessages.client.Playback;
import ru.dimaskama.voicemessages.client.PlaybackManager;
import ru.dimaskama.voicemessages.client.screen.widget.PlaybackPlayerWidget;
import ru.dimaskama.voicemessages.networking.VoiceMessageChunkC2S;

import java.util.List;

public class VoiceMessageConfirmScreen extends OverlayScreen {

    private static final WidgetSprites SEND_SPRITES = new WidgetSprites(
            VoiceMessagesMod.id("send"),
            VoiceMessagesMod.id("send_hovered")
    );
    private final int leftX;
    private final int fromBottomY;
    private final Playback playback;
    private PlaybackPlayerWidget playbackPlayerWidget;

    public VoiceMessageConfirmScreen(Screen parent, int leftX, int fromBottomY, List<short[]> audio) {
        super(Component.translatable("voicemessages.confirm"), parent);
        this.leftX = leftX;
        this.fromBottomY = fromBottomY;
        playback = new Playback(audio);
    }

    @Override
    protected void init() {
        super.init();
        if (playbackPlayerWidget == null) {
            playbackPlayerWidget = new PlaybackPlayerWidget(PlaybackManager.MAIN, playback, 0xFFAAAAAA);
        }
        int bottomY = height - fromBottomY;
        playbackPlayerWidget.setRectangle(260, 15, leftX + 1, bottomY - 15);
        addRenderableWidget(playbackPlayerWidget);
        ImageButton sendButton = addRenderableWidget(new ImageButton(
                leftX + 265,
                bottomY - 15,
                14,
                14,
                SEND_SPRITES,
                button -> {
                    List<short[]> audio = playback.getAudio();
                    OpusEncoder encoder = VoiceMessagesPlugin.getClientOpusEncoder();
                    encoder.resetState();
                    for (VoiceMessageChunkC2S chunk : VoiceMessageChunkC2S.split(VoiceMessagesPlugin.encodeList(encoder, audio))) {
                        VoiceMessagesMod.getService().sendToServer(chunk);
                    }
                    VoiceMessages.getLogger().info("Sent voice message (" + (1000 * audio.size() / VoiceMessages.FRAMES_PER_SEC) + "ms)");
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
        guiGraphics.pose().popPose();
    }

    @Override
    public void removed() {
        super.removed();
        PlaybackManager.MAIN.stopPlaying();
    }

}

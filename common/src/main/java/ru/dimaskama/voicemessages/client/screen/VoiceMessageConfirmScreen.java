package ru.dimaskama.voicemessages.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesPlugin;
import ru.dimaskama.voicemessages.client.Playback;
import ru.dimaskama.voicemessages.client.PlaybackManager;
import ru.dimaskama.voicemessages.client.screen.widget.PlaybackPlayerWidget;
import ru.dimaskama.voicemessages.networking.VoiceMessageC2S;

import java.util.List;

public class VoiceMessageConfirmScreen extends OverlayScreen {

    private static final ButtonTextures SEND_TEXTURES = new ButtonTextures(
            VoiceMessages.id("send"),
            VoiceMessages.id("send_hovered")
    );
    private final int leftX;
    private final int fromBottomY;
    private final Playback playback;
    private PlaybackPlayerWidget playbackPlayerWidget;

    public VoiceMessageConfirmScreen(Screen parent, int leftX, int fromBottomY, List<short[]> audio) {
        super(Text.translatable("voicemessages.confirm"), parent);
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
        playbackPlayerWidget.setDimensionsAndPosition(260, 15, leftX + 1, bottomY - 15);
        addDrawableChild(playbackPlayerWidget);
        TexturedButtonWidget sendButton = addDrawableChild(new TexturedButtonWidget(
                leftX + 265,
                bottomY - 15,
                14,
                14,
                SEND_TEXTURES,
                button -> {
                    List<short[]> audio = playback.getAudio();
                    VoiceMessages.getService().sendToServer(new VoiceMessageC2S(VoiceMessagesPlugin.getClientOpus().encode(audio)));
                    VoiceMessages.LOGGER.info("Sent voice message ({}ms)", 1000 * audio.size() / VoiceMessages.FRAMES_PER_SEC);
                    close();
                }
        ));
        sendButton.setTooltip(Tooltip.of(Text.translatable("voicemessages.send")));
        TexturedButtonWidget cancelButton = addDrawableChild(new TexturedButtonWidget(
                leftX + 281,
                bottomY - 15,
                14,
                14,
                RecordVoiceMessageScreen.CANCEL_TEXTURES,
                button -> close()
        ));
        cancelButton.setTooltip(Tooltip.of(ScreenTexts.CANCEL));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void actualRender(DrawContext context, int mouseX, int mouseY, float delta) {
        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 0.0F, 150.0F);
        super.actualRender(context, mouseX, mouseY, delta);
        context.getMatrices().pop();
    }

    @Override
    public void removed() {
        super.removed();
        PlaybackManager.MAIN.stopPlaying();
    }

}

package ru.dimaskama.voicemessages.client.screen.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.client.Playback;
import ru.dimaskama.voicemessages.client.PlaybackManager;
import ru.dimaskama.voicemessages.client.render.PlaybackRenderer;

public class PlaybackPlayerWidget extends ClickableWidget {

    private final PlayButton playButton = new PlayButton();
    private final PlaybackManager playbackManager;
    private final Playback playback;
    private final int backgroundColor;
    private final String totalDurationString;
    private float scale = 1.0F;
    private int playbackX, playbackY, playbackWidth, playbackHeight;

    public PlaybackPlayerWidget(PlaybackManager playbackManager, Playback playback, int backgroundColor) {
        super(0, 0, 0, 0, ScreenTexts.EMPTY);
        this.playbackManager = playbackManager;
        this.playback = playback;
        this.backgroundColor = backgroundColor;
        totalDurationString = '/' + formatTime(playback.getDurationMs());
    }

    public static String formatTime(int ms) {
        int s = ms / 1000;
        int m = s / 60;
        int sMod = s % 60;
        return String.format("%d:%02d", m, sMod);
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        context.getMatrices().push();
        context.getMatrices().translate(x - scale * x, y - scale * y, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        int width = getWidth();
        int height = getHeight();
        int addX = 0;
        playButton.setPosition(x, y + ((getHeight() - playButton.getHeight()) >> 1));
        playButton.renderWidget(context, mouseX, mouseY, delta);
        addX += playButton.getWidth() + 3;
        String timeStr = formatTime(1000 * playback.getFramePosition() / VoiceMessages.FRAMES_PER_SEC) + totalDurationString;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int timeStrWidth = textRenderer.getWidth(timeStr);
        playbackWidth = width - addX - timeStrWidth - 5;
        boolean renderTimeStr = playbackWidth >= 5;
        if (!renderTimeStr) {
            playbackWidth = width - addX;
        }
        if (playbackWidth >= 5) {
            playbackX = x + addX;
            playbackY = y;
            playbackHeight = height;
            if (backgroundColor != 0) {
                context.fill(playbackX - 1, playbackY - 1, playbackX + playbackWidth + 1, playbackY + playbackHeight + 1, 0xFFFFFFFF);
                context.fill(playbackX, playbackY, playbackX + playbackWidth, playbackY + playbackHeight, backgroundColor);
            }
            PlaybackRenderer.renderPlayback(context, playbackX, playbackY, playbackWidth, playbackHeight, playback, 1.0F);
        }
        if (renderTimeStr) {
            context.drawTextWithShadow(
                    textRenderer,
                    timeStr,
                    x + width - timeStrWidth,
                    y + ((height - 8) >> 1),
                    0xFFFFFFFF
            );
        }
        context.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= playbackY && mouseY < playbackY + playbackHeight) {
            double xProgress = MathHelper.getLerpProgress(mouseX, playbackX, playbackX + playbackWidth);
            if (xProgress >= 0.0 && xProgress <= 1.0) {
                playback.setProgress((float) xProgress);
                playbackManager.play(playback);
                return true;
            }
        }
        if (playButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    private class PlayButton extends ButtonWidget {

        private static final ButtonTextures PLAY_BUTTON_TEXTURES = new ButtonTextures(
                VoiceMessages.id("play"),
                VoiceMessages.id("play_hovered")
        );
        private static final ButtonTextures PAUSE_BUTTON_TEXTURES = new ButtonTextures(
                VoiceMessages.id("pause"),
                VoiceMessages.id("pause_hovered")
        );

        protected PlayButton() {
            super(0, 0, 10, 10, ScreenTexts.EMPTY, button -> {
                if (playback.isPlaying()) {
                    playbackManager.stopPlaying();
                } else {
                    playbackManager.play(playback);
                }
            }, DEFAULT_NARRATION_SUPPLIER);
        }

        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            Identifier identifier = (playback.isPlaying() ? PAUSE_BUTTON_TEXTURES : PLAY_BUTTON_TEXTURES).get(isNarratable(), isSelected());
            context.drawGuiTexture(RenderLayer::getGuiTextured, identifier, getX(), getY(), width, height);
        }

        @Override
        public void playDownSound(SoundManager soundManager) {
        }

    }

}

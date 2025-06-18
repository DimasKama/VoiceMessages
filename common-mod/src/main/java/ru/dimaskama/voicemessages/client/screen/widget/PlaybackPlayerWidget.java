package ru.dimaskama.voicemessages.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.Playback;
import ru.dimaskama.voicemessages.client.PlaybackManager;
import ru.dimaskama.voicemessages.client.render.PlaybackRenderer;

public class PlaybackPlayerWidget extends AbstractWidget {

    private final PlayButton playButton = new PlayButton();
    private final PlaybackManager playbackManager;
    private final Playback playback;
    private final int backgroundColor;
    private final String totalDurationString;
    private float scale = 1.0F;
    private int playbackX, playbackY, playbackWidth, playbackHeight;

    public PlaybackPlayerWidget(PlaybackManager playbackManager, Playback playback, int backgroundColor) {
        super(0, 0, 0, 0, CommonComponents.EMPTY);
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
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x - scale * x, y - scale * y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        int width = getWidth();
        int height = getHeight();
        int addX = 0;
        playButton.setPosition(x, y + ((getHeight() - playButton.getHeight()) >> 1));
        playButton.renderWidget(guiGraphics, mouseX, mouseY, delta);
        addX += playButton.getWidth() + 3;
        String timeStr = formatTime(1000 * playback.getFramePosition() / VoiceMessages.FRAMES_PER_SEC) + totalDurationString;
        Font font = Minecraft.getInstance().font;
        int timeStrWidth = font.width(timeStr);
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
                guiGraphics.fill(playbackX - 1, playbackY - 1, playbackX + playbackWidth + 1, playbackY + playbackHeight + 1, 0xFFFFFFFF);
                guiGraphics.fill(playbackX, playbackY, playbackX + playbackWidth, playbackY + playbackHeight, backgroundColor);
            }
            PlaybackRenderer.renderPlayback(guiGraphics, playbackX, playbackY, playbackWidth, playbackHeight, playback);
        }
        if (renderTimeStr) {
            guiGraphics.drawString(
                    font,
                    timeStr,
                    x + width - timeStrWidth,
                    y + ((height - 8) >> 1),
                    0xFFFFFFFF
            );
        }
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= playbackY && mouseY < playbackY + playbackHeight) {
            double xProgress = Mth.inverseLerp(mouseX, playbackX, playbackX + playbackWidth);
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
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    private class PlayButton extends Button {

        private static final WidgetSprites PLAY_BUTTON_SPRITES = new WidgetSprites(
                VoiceMessagesMod.id("play"),
                VoiceMessagesMod.id("play_hovered")
        );
        private static final WidgetSprites PAUSE_BUTTON_SPRITES = new WidgetSprites(
                VoiceMessagesMod.id("pause"),
                VoiceMessagesMod.id("pause_hovered")
        );

        protected PlayButton() {
            super(0, 0, 10, 10, CommonComponents.EMPTY, button -> {
                if (playback.isPlaying()) {
                    playbackManager.stopPlaying();
                } else {
                    playbackManager.play(playback);
                }
            }, DEFAULT_NARRATION);
        }

        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            ResourceLocation id = (playback.isPlaying() ? PAUSE_BUTTON_SPRITES : PLAY_BUTTON_SPRITES).get(isActive(), isFocused());
            guiGraphics.blitSprite(RenderType::guiTextured, id, getX(), getY(), width, height);
        }

        @Override
        public void playDownSound(SoundManager soundManager) {

        }

    }

}

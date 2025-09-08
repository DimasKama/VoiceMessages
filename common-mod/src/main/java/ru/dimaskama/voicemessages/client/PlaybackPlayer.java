package ru.dimaskama.voicemessages.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.render.PlaybackRenderer;

public class PlaybackPlayer {

    private static final int PLAY_BUTTON_SIDE = 10;
    private static final ResourceLocation PLAY_TEXTURE = VoiceMessagesMod.id("play");
    private static final ResourceLocation PAUSE_TEXTURE = VoiceMessagesMod.id("pause");
    private final PlaybackManager playbackManager;
    private final Playback playback;
    private final int backgroundColor;
    private final String totalDurationString;
    private ScreenRectangle playButtonRectangle = new ScreenRectangle(0, 0, 0, 0);
    private ScreenRectangle playbackTimeRectangle = new ScreenRectangle(0, 0, 0, 0);
    private ScreenRectangle playbackRectangle = new ScreenRectangle(0, 0, 0, 0);
    private int alpha = 0xFF;
    private int overlayColor = 0xFFFFFFFF;

    public PlaybackPlayer(PlaybackManager playbackManager, Playback playback, int backgroundColor) {
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

    public void setAlpha(int alpha) {
        this.alpha = alpha;
        overlayColor = (alpha << 24) | 0xFFFFFF;
    }

    public PlaybackPlayer setRectangle(int x, int y, int width, int height) {
        playButtonRectangle = new ScreenRectangle(x, y + ((height - PLAY_BUTTON_SIDE) >> 1), PLAY_BUTTON_SIDE, PLAY_BUTTON_SIDE);
        int playbackX = x + Math.min(width, PLAY_BUTTON_SIDE + 3);
        playbackTimeRectangle = new ScreenRectangle(playbackX, y, x + width - playbackX, height);
        playbackRectangle = new ScreenRectangle(0, 0, 0, 0);
        return this;
    }

    public void transform(Matrix4f matrix) {
        playButtonRectangle = transformRectangle(playButtonRectangle, matrix);
        playbackTimeRectangle = transformRectangle(playbackTimeRectangle, matrix);
        playbackRectangle = transformRectangle(playbackRectangle, matrix);
    }

    public void render(GuiGraphics guiGraphics) {
        renderPlayButton(guiGraphics);
        renderPlayback(guiGraphics);
    }

    private void renderPlayButton(GuiGraphics guiGraphics) {
        ResourceLocation id = playback.isPlaying() ? PAUSE_TEXTURE : PLAY_TEXTURE;
        RenderSystem.enableBlend();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);
        guiGraphics.blitSprite(
                id,
                playButtonRectangle.left(),
                playButtonRectangle.top(),
                playButtonRectangle.width(),
                playButtonRectangle.height()
        );
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void renderPlayback(GuiGraphics guiGraphics) {
        String timeStr = getTimeString();
        Font font = Minecraft.getInstance().font;
        int timeStrWidth = font.width(timeStr);
        int playbackWidth = playbackTimeRectangle.width() - timeStrWidth - 5;
        boolean renderTimeStr = playbackWidth >= 5;
        if (!renderTimeStr) {
            playbackWidth = playbackTimeRectangle.width();
        }
        if (playbackWidth >= 5) {
            int playbackX = playbackTimeRectangle.left();
            int playbackY = playbackTimeRectangle.top();
            int playbackHeight = playbackTimeRectangle.height();
            playbackRectangle = new ScreenRectangle(playbackX, playbackY, playbackWidth, playbackHeight);
            if ((backgroundColor & 0xFF000000) != 0) {
                guiGraphics.fill(playbackX - 1, playbackY - 1, playbackX + playbackWidth + 1, playbackY + playbackHeight + 1, 0xFFFFFFFF);
                guiGraphics.fill(playbackX, playbackY, playbackX + playbackWidth, playbackY + playbackHeight, backgroundColor);
            }
            PlaybackRenderer.renderPlayback(guiGraphics, playbackX, playbackY, playbackWidth, playbackHeight, alpha, playback);
        } else {
            playbackRectangle = new ScreenRectangle(0, 0, 0, 0);
        }
        if (renderTimeStr) {
            guiGraphics.drawString(
                    font,
                    timeStr,
                    playbackTimeRectangle.right() - timeStrWidth,
                    playbackTimeRectangle.top() + ((playbackTimeRectangle.height() - 8) >> 1),
                    overlayColor
            );
        }
    }

    private String getTimeString() {
        return formatTime(1000 * playback.getFramePosition() / VoiceMessages.FRAMES_PER_SEC) + totalDurationString;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            if (playbackRectangle.containsPoint(mouseX, mouseY)) {
                float delta = Mth.inverseLerp(mouseX, playbackRectangle.left(), playbackRectangle.right());
                if (delta >= 0.0F && delta <= 1.0F) {
                    playback.setProgress(delta);
                    playbackManager.play(playback);
                    return true;
                }
            }
            if (playButtonRectangle.containsPoint(mouseX, mouseY)) {
                if (playback.isPlaying()) {
                    playbackManager.stopPlaying();
                } else {
                    playbackManager.play(playback);
                }
                return true;
            }
        }
        return false;
    }

    private static ScreenRectangle transformRectangle(ScreenRectangle rectangle, Matrix4f matrix) {
        Vector3f pos = matrix.transformPosition(rectangle.left(), rectangle.top(), 0.0F, new Vector3f());
        Vector3f dims = matrix.transformPosition(rectangle.right(), rectangle.bottom(), 0.0F, new Vector3f())
                .sub(pos);
        return new ScreenRectangle(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(dims.x), Mth.floor(dims.y));
    }

}

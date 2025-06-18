package ru.dimaskama.voicemessages.client.render;

import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import ru.dimaskama.voicemessages.client.Playback;

public final class PlaybackRenderer {

    public static void renderPlayback(GuiGraphics guiGraphics, int x, int y, int width, int height, Playback playback) {
        FloatList audioLevels = playback.getAudioLevels();
        renderPlayback(guiGraphics, x, y, width, height, playback.getProgress(), audioLevels.size(), audioLevels);
    }

    public static void renderPlayback(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, int frameCount, FloatList audioLevels) {
        int audioLevelsSize = audioLevels.size();
        for (int i = 0; i < width; i++) {
            boolean isOdd = (i & 1) != 0;
            float p = (float) i / width;
            boolean passed = progress > 0.0F && p <= progress;
            float pAdj = p * frameCount;
            boolean recorded = pAdj < audioLevelsSize;
            int h;
            if (recorded) {
                int floor = Mth.floor(pAdj);
                int ceil = Mth.ceil(pAdj);
                h = Mth.lerpInt(Mth.lerp(pAdj - floor, audioLevels.getFloat(floor), ceil < audioLevelsSize ? audioLevels.getFloat(ceil) : 0.0F), 1, height);
            } else {
                h = 1;
            }
            int xx = x + i;
            int yy = y + ((height - h) >> 1);
            if (recorded || !isOdd) {
                guiGraphics.fill(xx, yy, xx + 1, yy + h, isOdd
                        ? passed ? 0xFFCCCCCC : 0xFF888888
                        : passed ? 0xFFFFFFFF : 0xFFAAAAAA);
            }
        }
    }

}

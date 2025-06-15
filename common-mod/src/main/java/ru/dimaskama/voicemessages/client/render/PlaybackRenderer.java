package ru.dimaskama.voicemessages.client.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import ru.dimaskama.voicemessages.client.Playback;

import java.util.Arrays;
import java.util.List;

public final class PlaybackRenderer {

    public static void renderPlayback(GuiGraphics guiGraphics, int x, int y, int width, int height, Playback playback, float recordProgress) {
        renderPlayback(guiGraphics, x, y, width, height, getSeed(playback.getAudio()), playback.getProgress(), recordProgress);
    }

    public static void renderPlayback(GuiGraphics guiGraphics, int x, int y, int width, int height, long randomSeed, float progress, float recordProgress) {
        RandomSource random = new SingleThreadedRandomSource(randomSeed);
        int minH = Math.max(1, height / 3);
        for (int i = 0; i < width; i++) {
            boolean isOdd = (i & 1) != 0;
            float p = (float) i / width;
            boolean passed = progress > 0.0F && p <= progress;
            boolean passedRecord = p <= recordProgress;
            int h = passedRecord ? Mth.lerpInt(random.nextFloat(), minH, height) : 1;
            int xx = x + i;
            int yy = y + ((height - h) >> 1);
            if (passedRecord || !isOdd) {
                guiGraphics.fill(xx, yy, xx + 1, yy + h, isOdd
                        ? passed ? 0xFFCCCCCC : 0xFF888888
                        : passed ? 0xFFFFFFFF : 0xFFAAAAAA);
            }
        }
    }

    public static long getSeed(List<short[]> someAudio) {
        if (someAudio == null) {
            return 0L;
        }
        short[] firstFrame;
        try {
            firstFrame = someAudio.getFirst();
        } catch (Exception e) {
            return 0L;
        }
        //todo test performance
        return Arrays.hashCode(firstFrame);
    }

}

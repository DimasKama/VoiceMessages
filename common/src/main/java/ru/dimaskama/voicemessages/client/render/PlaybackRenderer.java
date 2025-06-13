package ru.dimaskama.voicemessages.client.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import ru.dimaskama.voicemessages.client.Playback;

import java.util.List;

public final class PlaybackRenderer {

    public static void renderPlayback(DrawContext context, int x, int y, int width, int height, Playback playback, float recordProgress) {
        renderPlayback(context, x, y, width, height, getSeed(playback.getAudio()), playback.getProgress(), recordProgress);
    }

    public static void renderPlayback(DrawContext context, int x, int y, int width, int height, long randomSeed, float progress, float recordProgress) {
        Random random = new LocalRandom(randomSeed);
        int minH = Math.max(1, height / 3);
        for (int i = 0; i < width; i++) {
            boolean isOdd = (i & 1) != 0;
            float p = (float) i / width;
            boolean passed = progress > 0.0F && p <= progress;
            boolean passedRecord = p <= recordProgress;
            int h = passedRecord ? MathHelper.lerp(random.nextFloat(), minH, height) : 1;
            int xx = x + i;
            int yy = y + ((height - h) >> 1);
            if (passedRecord || !isOdd) {
                context.fill(xx, yy, xx + 1, yy + h, isOdd
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
        int l = Math.min(4, firstFrame.length);
        long result = 0L;
        for (int i = 0; i < l; i++) {
            result = (result << 16) | firstFrame[i];
        }
        return result;
    }

}

package ru.dimaskama.voicemessages;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class VoiceMessagesUtil {

    public static final int C2S_VOICE_MESSAGE_CHUNK_MAX_SIZE = 31743;
    public static final int S2C_VOICE_MESSAGE_CHUNK_MAX_SIZE = 1024000;

    public static <T> List<T> splitToChunks(List<byte[]> audio, int maxSize, Function<List<byte[]>, T> factory) {
        int start = 0;
        int totalFrames = audio.size();
        int chunkSize = 0;
        List<T> chunks = new ArrayList<>();
        for (int i = 0; i < totalFrames; i++) {
            int frameSize = 2 + audio.get(i).length;
            if (chunkSize + frameSize >= maxSize) {
                chunks.add(factory.apply(audio.subList(start, i)));
                start = i;
                chunkSize = 0;
            } else {
                chunkSize += frameSize;
            }
        }
        if (start < totalFrames) {
            chunks.add(factory.apply(audio.subList(start, totalFrames)));
        }
        return chunks;
    }

    public static boolean isFlush(List<byte[]> chunk) {
        return chunk.isEmpty();
    }

}

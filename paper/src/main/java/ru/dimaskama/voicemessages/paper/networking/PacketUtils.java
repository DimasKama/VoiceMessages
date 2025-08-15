package ru.dimaskama.voicemessages.paper.networking;

import com.mojang.datafixers.util.Pair;
import ru.dimaskama.voicemessages.VoiceMessages;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketUtils {

    public static int getVarIntSize(int i) {
        int size = 0;
        while ((i & -128) != 0) {
            i >>>= 7;
            ++size;
        }
        return size + 1;
    }

    // Returns integer packed with its size ((size << 32) | result)
    public static long readVarInt(byte[] bytes, int position) {
        int result = 0;
        int size = 0;

        byte b;
        do {
            b = bytes[position + size];
            result |= (b & 127) << size++ * 7;
            if (size > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b & 128) == 128);

        return ((long) size << 32) | result;
    }

    // Returns var int size
    public static int writeVarInt(byte[] bytes, int position, int i) {
        int size = 0;
        while ((i & -128) != 0) {
            bytes[position + size] = (byte) (i & 127 | 128);
            i >>>= 7;
            ++size;
        }
        bytes[position + size] = (byte) i;
        return size + 1;
    }

    public static int unpackSize(long readVarInt) {
        return (int) (readVarInt >>> 32);
    }

    public static int unpackInt(long readVarInt) {
        return (int) (readVarInt & 0xFFFFFFFFL);
    }

    public static Pair<List<byte[]>, Integer> readVoiceMessage(byte[] bytes, int position) {
        int offset = 0;
        long packedFrameCount = readVarInt(bytes, position + offset);
        offset += unpackSize(packedFrameCount);
        int frameCount = unpackInt(packedFrameCount);
        if (frameCount > VoiceMessages.MAX_VOICE_MESSAGE_FRAMES) {
            throw new IllegalArgumentException("Frame count is greater than " + VoiceMessages.MAX_VOICE_MESSAGE_FRAMES);
        }
        List<byte[]> frames = new ArrayList<>(frameCount);
        for (int i = 0; i < frameCount; i++) {
            long packedFrameSize = readVarInt(bytes, position + offset);
            int frameSize = unpackInt(packedFrameSize);
            if (frameSize > VoiceMessages.MAX_VOICE_MESSAGE_FRAMES) {
                throw new IllegalArgumentException("Frame #" + i + " is larger than " + VoiceMessages.MAX_VOICE_MESSAGE_FRAMES + " bytes");
            }
            offset += unpackSize(packedFrameSize);
            byte[] frame = new byte[frameSize];
            System.arraycopy(bytes, position + offset, frame, 0, frameSize);
            frames.add(frame);
            offset += frameSize;
        }
        return Pair.of(frames, offset);
    }

    public static int getVoiceMessageSize(List<byte[]> audio) {
        int size = getVarIntSize(audio.size());
        for (byte[] frame : audio) {
            size += getVarIntSize(frame.length) + frame.length;
        }
        return size;
    }

    public static int writeVoiceMessage(byte[] bytes, int position, List<byte[]> audio) {
        int offset = 0;
        offset += writeVarInt(bytes, position + offset, audio.size());
        for (byte[] frame : audio) {
            offset += writeVarInt(bytes, position + offset, frame.length);
            System.arraycopy(frame, 0, bytes, position + offset, frame.length);
            offset += frame.length;
        }
        return offset;
    }

    public static void writeUuid(byte[] bytes, int position, UUID uuid) {
        writeLong(bytes, position, uuid.getMostSignificantBits());
        writeLong(bytes, position + 8, uuid.getLeastSignificantBits());
    }

    public static void writeLong(byte[] bytes, int position, long l) {
        for (int i = 0; i < 8; i++) {
            bytes[position + i] = (byte) ((l >>> (56 - (i << 3))) & 0xFF);
        }
    }

    public static Pair<String, Integer> readUtf8(byte[] bytes, int position) {
        long packedSize = readVarInt(bytes, position);
        int offset = unpackSize(packedSize);
        int size = unpackInt(packedSize);
        String string = new String(bytes, offset, size, StandardCharsets.UTF_8);
        offset += size;
        return Pair.of(string, offset);
    }

}

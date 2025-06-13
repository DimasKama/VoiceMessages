package ru.dimaskama.voicemessages;

import com.google.common.collect.ImmutableList;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import java.util.List;

public record OpusPair(
        OpusEncoder encoder,
        OpusDecoder decoder
) {

    public List<byte[]> encode(List<short[]> audio) {
        encoder.resetState();
        int size = audio.size();
        ImmutableList.Builder<byte[]> builder = ImmutableList.builderWithExpectedSize(size);
        for (short[] frame : audio) {
            builder.add(encoder.encode(frame));
        }
        return builder.build();
    }

    public List<short[]> decode(List<byte[]> encoded) {
        decoder.resetState();
        int size = encoded.size();
        ImmutableList.Builder<short[]> builder = ImmutableList.builderWithExpectedSize(size);
        for (byte[] frame : encoded) {
            builder.add(decoder.decode(frame));
        }
        return builder.build();
    }

}

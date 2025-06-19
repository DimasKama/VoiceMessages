package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.VoiceMessagesUtil;

import java.util.List;

public record VoiceMessageChunkC2S(List<byte[]> encodedAudio) implements CustomPacketPayload {

    public static final Type<VoiceMessageChunkC2S> TYPE = new Type<>(VoiceMessagesMod.id("voice_message_chunk_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceMessageChunkC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.byteArray(VoiceMessages.MAX_ENCODED_FRAME_SIZE).apply(ByteBufCodecs.list(VoiceMessages.MAX_VOICE_MESSAGE_FRAMES)),
            VoiceMessageChunkC2S::encodedAudio,
            VoiceMessageChunkC2S::new
    );

    @Override
    public Type<VoiceMessageChunkC2S> type() {
        return TYPE;
    }

    public static List<VoiceMessageChunkC2S> split(List<byte[]> audio) {
        return VoiceMessagesUtil.splitToChunks(audio, VoiceMessagesUtil.C2S_VOICE_MESSAGE_CHUNK_MAX_SIZE, VoiceMessageChunkC2S::new);
    }

}

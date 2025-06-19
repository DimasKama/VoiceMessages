package ru.dimaskama.voicemessages.networking;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.VoiceMessagesUtil;

import java.util.List;
import java.util.UUID;

public record VoiceMessageChunkS2C(UUID sender, List<byte[]> encodedAudio) implements CustomPacketPayload {

    public static final Type<VoiceMessageChunkS2C> TYPE = new Type<>(VoiceMessagesMod.id("voice_message_chunk_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceMessageChunkS2C> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            VoiceMessageChunkS2C::sender,
            ByteBufCodecs.byteArray(VoiceMessages.MAX_ENCODED_FRAME_SIZE).apply(ByteBufCodecs.list(VoiceMessages.MAX_VOICE_MESSAGE_FRAMES)),
            VoiceMessageChunkS2C::encodedAudio,
            VoiceMessageChunkS2C::new
    );

    @Override
    public Type<VoiceMessageChunkS2C> type() {
        return TYPE;
    }

    public static List<VoiceMessageChunkS2C> split(UUID sender, List<byte[]> audio) {
        return VoiceMessagesUtil.splitToChunks(audio, VoiceMessagesUtil.S2C_VOICE_MESSAGE_CHUNK_MAX_SIZE, ch -> new VoiceMessageChunkS2C(sender, ch));
    }

}

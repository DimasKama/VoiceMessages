package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;

import java.util.List;

public record VoiceMessageC2S(List<byte[]> encodedAudio) implements CustomPacketPayload {

    public static final Type<VoiceMessageC2S> TYPE = new Type<>(VoiceMessagesMod.id("voice_message_c2s_v0"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceMessageC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.byteArray(256).apply(ByteBufCodecs.list(VoiceMessages.MAX_VOICE_MESSAGE_FRAMES)),
            VoiceMessageC2S::encodedAudio,
            VoiceMessageC2S::new
    );

    @Override
    public Type<VoiceMessageC2S> type() {
        return TYPE;
    }

}

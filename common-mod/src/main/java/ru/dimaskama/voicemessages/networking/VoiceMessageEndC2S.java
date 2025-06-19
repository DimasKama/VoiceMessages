package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.dimaskama.voicemessages.VoiceMessagesMod;

public record VoiceMessageEndC2S(String target) implements CustomPacketPayload {

    public static final Type<VoiceMessageEndC2S> TYPE = new Type<>(VoiceMessagesMod.id("voice_message_end_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceMessageEndC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64),
            VoiceMessageEndC2S::target,
            VoiceMessageEndC2S::new
    );

    @Override
    public Type<VoiceMessageEndC2S> type() {
        return TYPE;
    }

}

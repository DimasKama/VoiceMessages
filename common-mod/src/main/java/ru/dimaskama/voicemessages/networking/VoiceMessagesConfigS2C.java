package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.dimaskama.voicemessages.VoiceMessagesMod;

public record VoiceMessagesConfigS2C(int maxVoiceMessageDurationMs) implements CustomPacketPayload {

    public static final Type<VoiceMessagesConfigS2C> TYPE = new Type<>(VoiceMessagesMod.id("config_v0"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceMessagesConfigS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            VoiceMessagesConfigS2C::maxVoiceMessageDurationMs,
            VoiceMessagesConfigS2C::new
    );

    @Override
    public Type<VoiceMessagesConfigS2C> type() {
        return TYPE;
    }

}

package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.dimaskama.voicemessages.VoiceMessagesMod;

public record VoiceMessagesVersionC2S(String modVersion) implements CustomPacketPayload {

    public static final Type<VoiceMessagesVersionC2S> TYPE = new Type<>(VoiceMessagesMod.id("version"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceMessagesVersionC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128),
            VoiceMessagesVersionC2S::modVersion,
            VoiceMessagesVersionC2S::new
    );

    @Override
    public Type<VoiceMessagesVersionC2S> type() {
        return TYPE;
    }

}

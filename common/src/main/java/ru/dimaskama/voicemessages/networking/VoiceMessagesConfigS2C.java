package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.dimaskama.voicemessages.VoiceMessages;

public record VoiceMessagesConfigS2C(int maxVoiceMessageDurationMs) implements CustomPayload {

    public static final Id<VoiceMessagesConfigS2C> ID = new Id<>(VoiceMessages.id("config_v0"));
    public static final PacketCodec<PacketByteBuf, VoiceMessagesConfigS2C> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT,
            VoiceMessagesConfigS2C::maxVoiceMessageDurationMs,
            VoiceMessagesConfigS2C::new
    );
    @Override
    public Id<VoiceMessagesConfigS2C> getId() {
        return ID;
    }

}

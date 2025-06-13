package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;
import ru.dimaskama.voicemessages.VoiceMessages;

import java.util.List;
import java.util.UUID;

public record VoiceMessageS2C(UUID sender, List<byte[]> encodedAudio) implements CustomPayload {

    public static final Id<VoiceMessageS2C> ID = new Id<>(VoiceMessages.id("voice_message_s2c_v0"));
    public static final PacketCodec<RegistryByteBuf, VoiceMessageS2C> PACKET_CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            VoiceMessageS2C::sender,
            PacketCodecs.byteArray(256).collect(PacketCodecs.toList(VoiceMessages.MAX_VOICE_MESSAGE_FRAMES)),
            VoiceMessageS2C::encodedAudio,
            VoiceMessageS2C::new
    );

    @Override
    public Id<VoiceMessageS2C> getId() {
        return ID;
    }

}

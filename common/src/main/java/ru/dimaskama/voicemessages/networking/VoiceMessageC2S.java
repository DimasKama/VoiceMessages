package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.dimaskama.voicemessages.VoiceMessages;

import java.util.List;

public record VoiceMessageC2S(List<byte[]> encodedAudio) implements CustomPayload {

    public static final Id<VoiceMessageC2S> ID = new Id<>(VoiceMessages.id("voice_message_c2s_v0"));
    public static final PacketCodec<RegistryByteBuf, VoiceMessageC2S> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.byteArray(256).collect(PacketCodecs.toList(VoiceMessages.MAX_VOICE_MESSAGE_FRAMES)),
            VoiceMessageC2S::encodedAudio,
            VoiceMessageC2S::new
    );

    @Override
    public Id<VoiceMessageC2S> getId() {
        return ID;
    }

}

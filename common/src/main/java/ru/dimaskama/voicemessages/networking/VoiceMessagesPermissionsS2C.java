package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.dimaskama.voicemessages.VoiceMessages;

public record VoiceMessagesPermissionsS2C(boolean send) implements CustomPayload {

    public static final Id<VoiceMessagesPermissionsS2C> ID = new Id<>(VoiceMessages.id("voice_messages_permissions_v0"));
    public static final PacketCodec<RegistryByteBuf, VoiceMessagesPermissionsS2C> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN,
            VoiceMessagesPermissionsS2C::send,
            VoiceMessagesPermissionsS2C::new
    );

    @Override
    public Id<VoiceMessagesPermissionsS2C> getId() {
        return ID;
    }

}

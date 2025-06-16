package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.dimaskama.voicemessages.VoiceMessagesMod;

public record VoiceMessagesPermissionsS2C(boolean send) implements CustomPacketPayload {

    public static final Type<VoiceMessagesPermissionsS2C> TYPE = new Type<>(VoiceMessagesMod.id("voice_messages_permissions_v0"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceMessagesPermissionsS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            VoiceMessagesPermissionsS2C::send,
            VoiceMessagesPermissionsS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}

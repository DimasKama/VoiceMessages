package ru.dimaskama.voicemessages.networking;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;

import java.util.List;
import java.util.UUID;

public record VoiceMessageS2C(UUID sender, List<byte[]> encodedAudio) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VoiceMessageS2C> TYPE = new CustomPacketPayload.Type<>(VoiceMessagesMod.id("voice_message_s2c_v0"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceMessageS2C> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            VoiceMessageS2C::sender,
            ByteBufCodecs.byteArray(256).apply(ByteBufCodecs.list(VoiceMessages.MAX_VOICE_MESSAGE_FRAMES)),
            VoiceMessageS2C::encodedAudio,
            VoiceMessageS2C::new
    );

    @Override
    public CustomPacketPayload.Type<VoiceMessageS2C> type() {
        return TYPE;
    }

}

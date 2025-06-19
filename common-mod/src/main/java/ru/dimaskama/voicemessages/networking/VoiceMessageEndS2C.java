package ru.dimaskama.voicemessages.networking;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.dimaskama.voicemessages.VoiceMessagesMod;

import java.util.UUID;

public record VoiceMessageEndS2C(UUID sender, String target) implements CustomPacketPayload {

    public static final Type<VoiceMessageEndS2C> TYPE = new Type<>(VoiceMessagesMod.id("voice_message_end_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceMessageEndS2C> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            VoiceMessageEndS2C::sender,
            ByteBufCodecs.stringUtf8(64),
            VoiceMessageEndS2C::target,
            VoiceMessageEndS2C::new
    );

    @Override
    public Type<VoiceMessageEndS2C> type() {
        return TYPE;
    }

}

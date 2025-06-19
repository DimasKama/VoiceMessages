package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.dimaskama.voicemessages.VoiceMessagesMod;

import java.util.List;

public record VoiceMessageTargetsS2C(List<String> targets) implements CustomPacketPayload {

    public static final Type<VoiceMessageTargetsS2C> TYPE = new Type<>(VoiceMessagesMod.id("targets"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceMessageTargetsS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list()),
            VoiceMessageTargetsS2C::targets,
            VoiceMessageTargetsS2C::new
    );

    @Override
    public Type<VoiceMessageTargetsS2C> type() {
        return TYPE;
    }

}

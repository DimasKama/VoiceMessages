package ru.dimaskama.voicemessages.spigot.networking;

import ru.dimaskama.voicemessages.spigot.VoiceMessagesSpigot;

public record VoiceMessagesConfigS2C(int maxVoiceMessageDurationMs) {

    public static final String CHANNEL = VoiceMessagesSpigot.id("config_v0");

    public byte[] encode() {
        byte[] bytes = new byte[PacketUtils.getVarIntSize(maxVoiceMessageDurationMs)];
        PacketUtils.writeVarInt(bytes, 0, maxVoiceMessageDurationMs);
        return bytes;
    }

}

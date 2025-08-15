package ru.dimaskama.voicemessages.paper.networking;

import ru.dimaskama.voicemessages.paper.VoiceMessagesPaper;

public record VoiceMessagesConfigS2C(int maxVoiceMessageDurationMs) {

    public static final String CHANNEL = VoiceMessagesPaper.id("config_v0");

    public byte[] encode() {
        byte[] bytes = new byte[PacketUtils.getVarIntSize(maxVoiceMessageDurationMs)];
        PacketUtils.writeVarInt(bytes, 0, maxVoiceMessageDurationMs);
        return bytes;
    }

}

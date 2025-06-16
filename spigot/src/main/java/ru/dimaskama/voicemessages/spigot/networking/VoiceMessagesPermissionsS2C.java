package ru.dimaskama.voicemessages.spigot.networking;

import ru.dimaskama.voicemessages.spigot.VoiceMessagesSpigot;

public record VoiceMessagesPermissionsS2C(boolean send) {

    public static final String CHANNEL = VoiceMessagesSpigot.id("voice_messages_permissions_v0");

    public byte[] encode() {
        return new byte[] {(byte) (send ? 1 : 0)};
    }

}

package ru.dimaskama.voicemessages.paper.networking;

import ru.dimaskama.voicemessages.paper.VoiceMessagesPaper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static ru.dimaskama.voicemessages.paper.networking.PacketUtils.*;

public record VoiceMessageTargetsS2C(List<String> targets) {

    public static final String CHANNEL = VoiceMessagesPaper.id("targets");

    public byte[] encode() {
        int packetSize = getVarIntSize(targets.size());
        List<byte[]> encodedTargets = new ArrayList<>();
        for (String target : targets) {
            byte[] encodedTarget = target.getBytes(StandardCharsets.UTF_8);
            packetSize += getVarIntSize(encodedTarget.length) + encodedTarget.length;
            encodedTargets.add(encodedTarget);
        }
        byte[] bytes = new byte[packetSize];
        int pos = 0;
        pos += writeVarInt(bytes, pos, encodedTargets.size());
        for (byte[] encodedTarget : encodedTargets) {
            pos += writeVarInt(bytes, pos, encodedTarget.length);
            System.arraycopy(encodedTarget, 0, bytes, pos, encodedTarget.length);
            pos += encodedTarget.length;
        }
        return bytes;
    }

}

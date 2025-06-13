package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerConfigurationTask;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.config.ServerConfig;

import java.util.function.Consumer;

public class SendConfigTask implements ServerPlayerConfigurationTask {

    public static final Key KEY = new Key(VoiceMessages.id("config").toString());
    private final Runnable onComplete;

    public SendConfigTask(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    @Override
    public void sendPacket(Consumer<Packet<?>> sender) {
        ServerConfig config = VoiceMessages.SERVER_CONFIG.getData();
        sender.accept(new CustomPayloadS2CPacket(new VoiceMessagesConfigS2C(config.maxVoiceMessageDurationMs())));
        onComplete.run();
    }

    @Override
    public Key getKey() {
        return KEY;
    }

}

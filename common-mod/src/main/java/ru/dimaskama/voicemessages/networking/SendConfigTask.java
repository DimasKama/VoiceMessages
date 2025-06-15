package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.network.ConfigurationTask;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.config.ServerConfig;

import java.util.function.Consumer;

public class SendConfigTask implements ConfigurationTask {

    public static final ConfigurationTask.Type TYPE = new Type(VoiceMessagesMod.id("config").toString());
    private final Runnable onComplete;

    public SendConfigTask(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    @Override
    public void start(Consumer<Packet<?>> consumer) {
        ServerConfig config = VoiceMessages.SERVER_CONFIG.getData();
        consumer.accept(new ClientboundCustomPayloadPacket(new VoiceMessagesConfigS2C(config.maxVoiceMessageDurationMs())));
        onComplete.run();
    }

    @Override
    public Type type() {
        return TYPE;
    }

}

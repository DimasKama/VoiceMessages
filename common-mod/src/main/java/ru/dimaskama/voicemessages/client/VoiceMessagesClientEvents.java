package ru.dimaskama.voicemessages.client;

import net.minecraft.client.Minecraft;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.VoiceMessagesModService;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.networking.VoiceMessagesVersionC2S;

public final class VoiceMessagesClientEvents {

    public static void onJoinedServer() {
        VoiceMessagesModService service = VoiceMessagesMod.getService();
        if (service.canSendToServer(VoiceMessagesVersionC2S.TYPE.id())) {
            service.sendToServer(new VoiceMessagesVersionC2S(VoiceMessagesMod.getModVersion()));
        }
    }

    public static void onClientTick(Minecraft minecraft) {
        if (VoiceMessagesMod.isActive()) {
            VoiceMessagesClientNetworking.tickBuildingVoiceMessages();
        }
    }

}

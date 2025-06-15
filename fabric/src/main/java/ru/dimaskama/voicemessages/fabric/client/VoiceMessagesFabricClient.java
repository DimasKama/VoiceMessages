package ru.dimaskama.voicemessages.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.networking.VoiceMessageS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessagesConfigS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessagesPermissionsS2C;

public final class VoiceMessagesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (VoiceMessagesMod.isActive()) {
            ClientConfigurationNetworking.registerGlobalReceiver(VoiceMessagesConfigS2C.TYPE, (payload, context) ->
                    VoiceMessagesClientNetworking.onVoiceMessagesConfigReceived(payload));
            ClientPlayNetworking.registerGlobalReceiver(VoiceMessagesPermissionsS2C.TYPE, (payload, context) ->
                    VoiceMessagesClientNetworking.onVoiceMessagesPermissionsReceived(payload));
            ClientPlayNetworking.registerGlobalReceiver(VoiceMessageS2C.TYPE, (payload, context) ->
                    VoiceMessagesClientNetworking.onVoiceMessageReceived(payload));
        }
    }

}

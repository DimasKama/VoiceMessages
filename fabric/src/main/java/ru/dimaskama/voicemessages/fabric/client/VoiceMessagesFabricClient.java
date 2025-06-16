package ru.dimaskama.voicemessages.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.VoiceMessagesClientEvents;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.networking.VoiceMessageChunkS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessagesConfigS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessagesPermissionsS2C;

public final class VoiceMessagesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (VoiceMessagesMod.isActive()) {
            ClientPlayNetworking.registerGlobalReceiver(VoiceMessagesConfigS2C.TYPE, (payload, context) ->
                    VoiceMessagesClientNetworking.onVoiceMessagesConfigReceived(payload));
            ClientPlayNetworking.registerGlobalReceiver(VoiceMessagesPermissionsS2C.TYPE, (payload, context) ->
                    VoiceMessagesClientNetworking.onVoiceMessagesPermissionsReceived(payload));
            ClientPlayNetworking.registerGlobalReceiver(VoiceMessageChunkS2C.TYPE, (payload, context) ->
                    VoiceMessagesClientNetworking.onVoiceMessageChunkReceived(payload));

            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                    VoiceMessagesClientEvents.onJoinedServer());

            ClientTickEvents.END_CLIENT_TICK.register(VoiceMessagesClientEvents::onClientTick);
        }
    }

}

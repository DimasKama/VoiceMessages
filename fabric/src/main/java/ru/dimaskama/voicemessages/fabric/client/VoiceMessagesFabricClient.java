package ru.dimaskama.voicemessages.fabric.client;

import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.VoiceMessagesClientEvents;
import ru.dimaskama.voicemessages.client.VoicemsgCommand;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.networking.VoiceMessageChunkS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessageEndS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessageTargetsS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessagesConfigS2C;

public final class VoiceMessagesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (VoiceMessagesMod.isActive()) {
            ClientPlayNetworking.registerGlobalReceiver(VoiceMessagesConfigS2C.TYPE, (payload, context) ->
                    VoiceMessagesClientNetworking.onConfigReceived(payload));
            ClientPlayNetworking.registerGlobalReceiver(VoiceMessageTargetsS2C.TYPE, (payload, context) ->
                    VoiceMessagesClientNetworking.onTargetsReceived(payload));
            ClientPlayNetworking.registerGlobalReceiver(VoiceMessageChunkS2C.TYPE, (payload, context) ->
                    VoiceMessagesClientNetworking.onVoiceMessageChunkReceived(payload));
            ClientPlayNetworking.registerGlobalReceiver(VoiceMessageEndS2C.TYPE, (payload, context) ->
                    VoiceMessagesClientNetworking.onVoiceMessageEndReceived(payload));

            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                    VoiceMessagesClientEvents.onJoinedServer());

            ClientTickEvents.END_CLIENT_TICK.register(VoiceMessagesClientEvents::onClientTick);

            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                LiteralCommandNode<FabricClientCommandSource> command = dispatcher.register(new VoicemsgCommand<>(
                        ClientCommandManager::literal,
                        ClientCommandManager::argument
                ).createCommand());
                dispatcher.register(ClientCommandManager.literal(VoicemsgCommand.ALIAS)
                        .executes(command.getCommand())
                        .redirect(command));
            });
        }
    }

}

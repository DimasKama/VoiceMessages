package ru.dimaskama.voicemessages.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesEvents;
import ru.dimaskama.voicemessages.VoiceMessagesService;
import ru.dimaskama.voicemessages.fabric.client.FabricVoiceRecordThread;
import ru.dimaskama.voicemessages.networking.*;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class VoiceMessagesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        VoiceMessages.init(new VoiceMessagesService() {
            @Override
            public boolean isModLoaded(String modId) {
                return FabricLoader.getInstance().isModLoaded(modId);
            }

            @Override
            public void sendToServer(CustomPayload payload) {
                ClientPlayNetworking.send(payload);
            }

            @Override
            public boolean canSendToServer(Identifier payloadId) {
                return ClientPlayNetworking.canSend(payloadId);
            }

            @Override
            public void sendToPlayer(ServerPlayerEntity player, CustomPayload payload) {
                ServerPlayNetworking.send(player, payload);
            }

            @Override
            public boolean canSendToPlayer(ServerPlayerEntity player, Identifier payloadId) {
                return ServerPlayNetworking.canSend(player, payloadId);
            }

            @Override
            public boolean canSendConfigurationToPlayer(ServerConfigurationNetworkHandler handler, Identifier payloadId) {
                return ServerConfigurationNetworking.canSend(handler, payloadId);
            }

            @Override
            public VoiceRecordThread createVoiceRecordThread(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError) {
                return new FabricVoiceRecordThread(frameConsumer, onMicError);
            }

            @Override
            public boolean hasVoiceMessageSendPermission(ServerPlayerEntity player) {
                return Permissions.check(player, VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION, 0);
            }
        });

        if (VoiceMessages.isActive()) {
            PayloadTypeRegistry.configurationS2C().register(VoiceMessagesConfigS2C.ID, VoiceMessagesConfigS2C.PACKET_CODEC);
            ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register((handler, server) ->
                    VoiceMessagesEvents.onRegisterConfigurationTasks(handler, handler::addTask, handler::completeTask));

            PayloadTypeRegistry.playS2C().register(VoiceMessagesPermissionsS2C.ID, VoiceMessagesPermissionsS2C.PACKET_CODEC);
            PayloadTypeRegistry.playS2C().register(VoiceMessageS2C.ID, VoiceMessageS2C.PACKET_CODEC);
            PayloadTypeRegistry.playC2S().register(VoiceMessageC2S.ID, VoiceMessageC2S.PACKET_CODEC);
            ServerPlayNetworking.registerGlobalReceiver(VoiceMessageC2S.ID, (payload, context) ->
                    VoiceMessagesServerNetworking.onVoiceMessageReceived(context.player(), payload));

            ServerLifecycleEvents.SERVER_STARTED.register(VoiceMessagesEvents::onServerStarted);
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                    VoiceMessagesEvents.onPlayerJoined(handler.getPlayer()));
        }
    }

}

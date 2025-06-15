package ru.dimaskama.voicemessages.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesEvents;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.VoiceMessagesModService;
import ru.dimaskama.voicemessages.fabric.client.FabricVoiceRecordThread;
import ru.dimaskama.voicemessages.networking.*;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class VoiceMessagesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        VoiceMessagesMod.init(new VoiceMessagesModService() {
            @Override
            public boolean isModLoaded(String modId) {
                return FabricLoader.getInstance().isModLoaded(modId);
            }

            @Override
            public void sendToServer(CustomPacketPayload payload) {
                ClientPlayNetworking.send(payload);
            }

            @Override
            public boolean canSendToServer(ResourceLocation payloadId) {
                return ClientPlayNetworking.canSend(payloadId);
            }

            @Override
            public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
                ServerPlayNetworking.send(player, payload);
            }

            @Override
            public boolean canSendToPlayer(ServerPlayer player, ResourceLocation payloadId) {
                return ServerPlayNetworking.canSend(player, payloadId);
            }

            @Override
            public boolean canSendConfigurationToPlayer(ServerConfigurationPacketListenerImpl handler, ResourceLocation payloadId) {
                return ServerConfigurationNetworking.canSend(handler, payloadId);
            }

            @Override
            public VoiceRecordThread createVoiceRecordThread(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError) {
                return new FabricVoiceRecordThread(frameConsumer, onMicError);
            }

            @Override
            public boolean hasVoiceMessageSendPermission(ServerPlayer player) {
                return Permissions.check(player, VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION, 0);
            }
        });

        if (VoiceMessagesMod.isActive()) {
            PayloadTypeRegistry.configurationS2C().register(VoiceMessagesConfigS2C.TYPE, VoiceMessagesConfigS2C.PACKET_CODEC);
            ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register((handler, server) ->
                    VoiceMessagesEvents.onRegisterConfigurationTasks(handler, handler::addTask, handler::completeTask));

            PayloadTypeRegistry.playS2C().register(VoiceMessagesPermissionsS2C.TYPE, VoiceMessagesPermissionsS2C.PACKET_CODEC);
            PayloadTypeRegistry.playS2C().register(VoiceMessageS2C.TYPE, VoiceMessageS2C.STREAM_CODEC);
            PayloadTypeRegistry.playC2S().register(VoiceMessageC2S.TYPE, VoiceMessageC2S.STREAM_CODEC);
            ServerPlayNetworking.registerGlobalReceiver(VoiceMessageC2S.TYPE, (payload, context) ->
                    VoiceMessagesServerNetworking.onVoiceMessageReceived(context.player(), payload));

            ServerLifecycleEvents.SERVER_STARTED.register(VoiceMessagesEvents::onServerStarted);
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                    VoiceMessagesEvents.onPlayerJoined(handler.getPlayer()));
        }
    }

}

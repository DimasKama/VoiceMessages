package ru.dimaskama.voicemessages.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(VoiceMessages.ID).orElseThrow();
        VoiceMessagesMod.init(modContainer.getMetadata().getVersion().toString(), new VoiceMessagesModService() {
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
            public VoiceRecordThread createVoiceRecordThread(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError) {
                return new FabricVoiceRecordThread(frameConsumer, onMicError);
            }

            @Override
            public boolean hasVoiceMessageSendPermission(ServerPlayer player) {
                return Permissions.check(player, VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION, 0);
            }

            @Override
            public boolean hasVoiceMessageSendAllPermission(ServerPlayer player) {
                return Permissions.check(player, VoiceMessages.VOICE_MESSAGE_SEND_ALL_PERMISSION, 0);
            }

            @Override
            public boolean hasVoiceMessageSendTeamPermission(ServerPlayer player) {
                return Permissions.check(player, VoiceMessages.VOICE_MESSAGE_SEND_TEAM_PERMISSION, 0);
            }

            @Override
            public boolean hasVoiceMessageSendPlayersPermission(ServerPlayer player) {
                return Permissions.check(player, VoiceMessages.VOICE_MESSAGE_SEND_PLAYERS_PERMISSION, 0);
            }
        });

        if (VoiceMessagesMod.isActive()) {
            PayloadTypeRegistry.playS2C().register(VoiceMessagesConfigS2C.TYPE, VoiceMessagesConfigS2C.STREAM_CODEC);
            PayloadTypeRegistry.playS2C().register(VoiceMessageTargetsS2C.TYPE, VoiceMessageTargetsS2C.STREAM_CODEC);
            PayloadTypeRegistry.playS2C().register(VoiceMessageChunkS2C.TYPE, VoiceMessageChunkS2C.STREAM_CODEC);
            PayloadTypeRegistry.playS2C().register(VoiceMessageEndS2C.TYPE, VoiceMessageEndS2C.STREAM_CODEC);

            PayloadTypeRegistry.playC2S().register(VoiceMessagesVersionC2S.TYPE, VoiceMessagesVersionC2S.STREAM_CODEC);
            ServerPlayNetworking.registerGlobalReceiver(VoiceMessagesVersionC2S.TYPE, (payload, context) ->
                    VoiceMessagesServerNetworking.onVoiceMessagesVersionReceived(context.player(), payload));

            PayloadTypeRegistry.playC2S().register(VoiceMessageChunkC2S.TYPE, VoiceMessageChunkC2S.STREAM_CODEC);
            ServerPlayNetworking.registerGlobalReceiver(VoiceMessageChunkC2S.TYPE, (payload, context) ->
                    VoiceMessagesServerNetworking.onVoiceMessageChunkReceived(context.player(), payload));

            PayloadTypeRegistry.playC2S().register(VoiceMessageEndC2S.TYPE, VoiceMessageEndC2S.STREAM_CODEC);
            ServerPlayNetworking.registerGlobalReceiver(VoiceMessageEndC2S.TYPE, (payload, context) ->
                    VoiceMessagesServerNetworking.onVoiceMessageEndReceived(context.player(), payload));

            ServerLifecycleEvents.SERVER_STARTED.register(VoiceMessagesEvents::onServerStarted);

            ServerTickEvents.END_SERVER_TICK.register(VoiceMessagesEvents::onServerTick);

            ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                    VoiceMessagesServerNetworking.onPlayerDisconnected(server, handler.getOwner().getId()));
        }
    }

}

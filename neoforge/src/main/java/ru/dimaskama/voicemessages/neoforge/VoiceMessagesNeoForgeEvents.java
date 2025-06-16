package ru.dimaskama.voicemessages.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesEvents;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.networking.*;

@EventBusSubscriber(modid = VoiceMessages.ID, bus = EventBusSubscriber.Bus.GAME)
public final class VoiceMessagesNeoForgeEvents {

    @SubscribeEvent
    private static void onServerStarted(ServerStartedEvent event) {
        if (VoiceMessagesMod.isActive()) {
            VoiceMessagesEvents.onServerStarted(event.getServer());
        }
    }

    @SubscribeEvent
    private static void onServerTick(ServerTickEvent.Post event) {
        if (VoiceMessagesMod.isActive()) {
            VoiceMessagesEvents.onServerTick(event.getServer());
        }
    }

    @SubscribeEvent
    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (VoiceMessagesMod.isActive()) {
            if (event.getEntity() instanceof ServerPlayer player) {
                VoiceMessagesServerNetworking.onPlayerDisconnected(player.getUUID());
            }
        }
    }

    @SubscribeEvent
    private static void onPermissionGatherNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(VoiceMessagesNeoForge.VOICE_MESSAGE_SEND_PERMISSION);
    }

    @EventBusSubscriber(modid = VoiceMessages.ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {

        @SubscribeEvent
        private static void onPayloadRegister(RegisterPayloadHandlersEvent event) {
            if (VoiceMessagesMod.isActive()) {
                PayloadRegistrar registrar = event.registrar("1")
                        .executesOn(HandlerThread.NETWORK)
                        .optional();
                registrar.playToServer(
                        VoiceMessagesVersionC2S.TYPE,
                        VoiceMessagesVersionC2S.STREAM_CODEC,
                        (payload, context) ->
                                VoiceMessagesServerNetworking.onVoiceMessagesVersionReceived((ServerPlayer) context.player(), payload)
                );
                registrar.playToClient(
                        VoiceMessagesConfigS2C.TYPE,
                        VoiceMessagesConfigS2C.STREAM_CODEC,
                        (payload, context) ->
                                VoiceMessagesClientNetworking.onVoiceMessagesConfigReceived(payload)
                );
                registrar.playToClient(
                        VoiceMessagesPermissionsS2C.TYPE,
                        VoiceMessagesPermissionsS2C.STREAM_CODEC,
                        (payload, context) ->
                                VoiceMessagesClientNetworking.onVoiceMessagesPermissionsReceived(payload)
                );
                registrar.playToServer(
                        VoiceMessageChunkC2S.TYPE,
                        VoiceMessageChunkC2S.STREAM_CODEC,
                        (payload, context) ->
                                VoiceMessagesServerNetworking.onVoiceMessageChunkReceived((ServerPlayer) context.player(), payload)
                );
                registrar.playToClient(
                        VoiceMessageChunkS2C.TYPE,
                        VoiceMessageChunkS2C.STREAM_CODEC,
                        (payload, context) ->
                                VoiceMessagesClientNetworking.onVoiceMessageChunkReceived(payload)
                );
            }
        }

    }

}

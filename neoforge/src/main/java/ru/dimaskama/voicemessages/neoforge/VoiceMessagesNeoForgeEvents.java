package ru.dimaskama.voicemessages.neoforge;

import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PermissionsChangedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesEvents;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.networking.*;

@EventBusSubscriber(modid = VoiceMessages.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class VoiceMessagesNeoForgeEvents {

    @SubscribeEvent
    private static void onPayloadRegister(RegisterPayloadHandlersEvent event) {
        if (VoiceMessages.isActive()) {
            PayloadRegistrar registrar = event.registrar("1")
                    .executesOn(HandlerThread.NETWORK)
                    .optional();
            registrar.configurationToClient(
                    VoiceMessagesConfigS2C.ID,
                    VoiceMessagesConfigS2C.PACKET_CODEC,
                    (payload, context) ->
                            VoiceMessagesClientNetworking.onVoiceMessagesConfigReceived(payload)
            );
            registrar.playToClient(
                    VoiceMessagesPermissionsS2C.ID,
                    VoiceMessagesPermissionsS2C.PACKET_CODEC,
                    (payload, context) ->
                            VoiceMessagesClientNetworking.onVoiceMessagesPermissionsReceived(payload)
            );
            registrar.playToServer(
                    VoiceMessageC2S.ID,
                    VoiceMessageC2S.PACKET_CODEC,
                    (payload, context) ->
                            VoiceMessagesServerNetworking.onVoiceMessageReceived((ServerPlayerEntity) context.player(), payload)
            );
            registrar.playToClient(
                    VoiceMessageS2C.ID,
                    VoiceMessageS2C.PACKET_CODEC,
                    (payload, context) ->
                            VoiceMessagesClientNetworking.onVoiceMessageReceived(payload)
            );
        }
    }

    @SubscribeEvent
    private static void onConfigurationTaskRegister(RegisterConfigurationTasksEvent event) {
        if (VoiceMessages.isActive()) {
            if (event.getListener() instanceof ServerConfigurationNetworkHandler handler) {
                VoiceMessagesEvents.onRegisterConfigurationTasks(handler, event::register, handler::onTaskFinished);
            } else {
                VoiceMessages.LOGGER.error("Forge implementation: provided ServerConfigurationPacketListener is not the instance of ServerConfigurationNetworkHandler");
            }
        }
    }

    @EventBusSubscriber(modid = VoiceMessages.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static final class GameBus {

        @SubscribeEvent
        private static void onServerStarted(ServerStartedEvent event) {
            if (VoiceMessages.isActive()) {
                VoiceMessagesEvents.onServerStarted(event.getServer());
            }
        }

        @SubscribeEvent
        private static void onPermissionsChanged(PermissionsChangedEvent event) {
            if (VoiceMessages.isActive()) {
                if (event.getEntity() instanceof ServerPlayerEntity player) {
                    VoiceMessagesEvents.onPermissionsUpdated(player);
                }
            }
        }

        @SubscribeEvent
        private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (VoiceMessages.isActive()) {
                if (event.getEntity() instanceof ServerPlayerEntity player) {
                    VoiceMessagesEvents.onPlayerJoined(player);
                }
            }
        }

        @SubscribeEvent
        private static void onPermissionGatherNodes(PermissionGatherEvent.Nodes event) {
            event.addNodes(VoiceMessagesNeoForge.VOICE_MESSAGE_SEND_PERMISSION);
        }

    }

}

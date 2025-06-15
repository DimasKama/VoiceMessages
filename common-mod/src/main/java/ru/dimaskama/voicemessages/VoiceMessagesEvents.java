package ru.dimaskama.voicemessages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import ru.dimaskama.voicemessages.config.ServerConfig;
import ru.dimaskama.voicemessages.networking.SendConfigTask;
import ru.dimaskama.voicemessages.networking.VoiceMessagesConfigS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessagesPermissionsS2C;

import java.util.function.Consumer;

public final class VoiceMessagesEvents {

    public static void onServerStarted(MinecraftServer server) {
        if (VoiceMessagesMod.isActive()) {
            VoiceMessages.SERVER_CONFIG.loadOrCreate();
        }
    }

    public static void onRegisterConfigurationTasks(
            ServerConfigurationPacketListenerImpl handler,
            Consumer<ConfigurationTask> startTask,
            Consumer<ConfigurationTask.Type> completeTask
    ) {
        if (VoiceMessagesMod.isActive()) {
            if (VoiceMessagesMod.getService().canSendConfigurationToPlayer(handler, VoiceMessagesConfigS2C.TYPE.id())) {
                startTask.accept(new SendConfigTask(() -> completeTask.accept(SendConfigTask.TYPE)));
            } else {
                ServerConfig config = VoiceMessages.SERVER_CONFIG.getData();
                if (config.modRequired()) {
                    handler.disconnect(Component.literal(config.modNotInstalledText()));
                }
            }
        }
    }

    public static void onPlayerJoined(ServerPlayer player) {
        if (VoiceMessagesMod.isActive()) {
            sendPermissions(player);
        }

    }

    public static void onPermissionsUpdated(ServerPlayer player) {
        if (VoiceMessagesMod.isActive()) {
            sendPermissions(player);
        }
    }

    private static void sendPermissions(ServerPlayer player) {
        VoiceMessagesModService service = VoiceMessagesMod.getService();
        service.sendToPlayer(player, new VoiceMessagesPermissionsS2C(service.hasVoiceMessageSendPermission(player)));
    }

}

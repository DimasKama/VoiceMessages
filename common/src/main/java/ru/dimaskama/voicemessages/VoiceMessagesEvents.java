package ru.dimaskama.voicemessages;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerConfigurationTask;
import net.minecraft.server.network.ServerPlayerEntity;
import ru.dimaskama.voicemessages.config.ServerConfig;
import ru.dimaskama.voicemessages.networking.SendConfigTask;
import ru.dimaskama.voicemessages.networking.VoiceMessagesConfigS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessagesPermissionsS2C;

import java.util.function.Consumer;

public final class VoiceMessagesEvents {

    public static void onServerStarted(MinecraftServer server) {
        if (VoiceMessages.isActive()) {
            VoiceMessages.SERVER_CONFIG.loadOrCreate();
        }
    }

    public static void onRegisterConfigurationTasks(
            ServerConfigurationNetworkHandler handler,
            Consumer<ServerPlayerConfigurationTask> startTask,
            Consumer<ServerPlayerConfigurationTask.Key> completeTask
    ) {
        if (VoiceMessages.isActive()) {
            if (VoiceMessages.getService().canSendConfigurationToPlayer(handler, VoiceMessagesConfigS2C.ID.id())) {
                startTask.accept(new SendConfigTask(() -> completeTask.accept(SendConfigTask.KEY)));
            } else {
                ServerConfig config = VoiceMessages.SERVER_CONFIG.getData();
                if (config.modRequired()) {
                    handler.disconnect(config.modNotInstalledText());
                }
            }
        }
    }

    public static void onPlayerJoined(ServerPlayerEntity player) {
        if (VoiceMessages.isActive()) {
            sendPermissions(player);
        }

    }

    public static void onPermissionsUpdated(ServerPlayerEntity player) {
        if (VoiceMessages.isActive()) {
            sendPermissions(player);
        }
    }

    private static void sendPermissions(ServerPlayerEntity player) {
        VoiceMessagesService service = VoiceMessages.getService();
        service.sendToPlayer(player, new VoiceMessagesPermissionsS2C(service.hasVoiceMessageSendPermission(player)));
    }

}

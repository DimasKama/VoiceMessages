package ru.dimaskama.voicemessages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.dimaskama.voicemessages.config.ServerConfig;
import ru.dimaskama.voicemessages.networking.VoiceMessagesPermissionsS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessagesServerNetworking;

public final class VoiceMessagesEvents {

    public static void onServerStarted(MinecraftServer server) {
        if (VoiceMessagesMod.isActive()) {
            VoiceMessages.SERVER_CONFIG.loadOrCreate();
        }
    }

    public static void onServerTick(MinecraftServer server) {
        if (VoiceMessagesMod.isActive()) {
            VoiceMessagesServerNetworking.tickBuildingVoiceMessages();
        }
    }

    public static void checkForCompatibleVersion(ServerPlayer player) {
        if (!VoiceMessagesServerNetworking.hasCompatibleVersion(player)) {
            ServerConfig config = VoiceMessages.SERVER_CONFIG.getData();
            if (config.modRequired()) {
                player.connection.disconnect(Component.literal(config.modNotInstalledText()));
            }
        }
    }

    public static void sendPermissions(ServerPlayer player) {
        VoiceMessagesModService service = VoiceMessagesMod.getService();
        service.sendToPlayer(player, new VoiceMessagesPermissionsS2C(service.hasVoiceMessageSendPermission(player)));
    }

}

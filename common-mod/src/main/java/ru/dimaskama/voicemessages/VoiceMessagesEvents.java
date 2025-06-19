package ru.dimaskama.voicemessages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.dimaskama.voicemessages.api.VoiceMessagesApiInitCallback;
import ru.dimaskama.voicemessages.config.ServerConfig;
import ru.dimaskama.voicemessages.impl.VoiceMessagesApiImpl;
import ru.dimaskama.voicemessages.networking.VoiceMessagesServerNetworking;

public final class VoiceMessagesEvents {

    public static void onServerStarted(MinecraftServer server) {
        if (VoiceMessagesMod.isActive()) {
            VoiceMessages.SERVER_CONFIG.loadOrCreate();
            VoiceMessagesApiInitCallback.EVENT.invoker().setVoiceMessagesApi(new VoiceMessagesApiImpl(server));
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

}

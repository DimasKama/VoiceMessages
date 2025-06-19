package ru.dimaskama.voicemessages.impl;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import ru.dimaskama.voicemessages.api.VoiceMessagesApi;
import ru.dimaskama.voicemessages.networking.VoiceMessagesServerNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record VoiceMessagesApiImpl(MinecraftServer server) implements VoiceMessagesApi {

    @Override
    public boolean isPlayerHasCompatibleModVersion(UUID playerUuid) {
        return VoiceMessagesServerNetworking.hasCompatibleVersion(playerUuid);
    }

    @Override
    public boolean updateAvailableTargets(UUID playerUuid) {
        if (VoiceMessagesServerNetworking.hasCompatibleVersion(playerUuid)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null) {
                VoiceMessagesServerNetworking.updateTargets(player);
                return true;
            }
        }
        return false;
    }

    @Override
    public void sendVoiceMessage(UUID senderUuid, Iterable<UUID> playerUuids, List<byte[]> message, String displayTarget) {
        PlayerList playerList = server.getPlayerList();
        List<ServerPlayer> players = new ArrayList<>();
        for (UUID playerUuid : playerUuids) {
            ServerPlayer player = playerList.getPlayer(playerUuid);
            if (player != null) {
                players.add(player);
            }
        }
        VoiceMessagesServerNetworking.sendVoiceMessage(senderUuid, players, message, displayTarget);
    }

}

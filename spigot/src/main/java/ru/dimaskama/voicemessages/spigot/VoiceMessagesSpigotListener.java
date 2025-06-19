package ru.dimaskama.voicemessages.spigot;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.config.ServerConfig;
import ru.dimaskama.voicemessages.spigot.networking.VoiceMessagesSpigotNetworking;

import java.util.UUID;

public record VoiceMessagesSpigotListener(VoiceMessagesSpigot plugin) implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        Server server = event.getPlayer().getServer();
        server.getScheduler().runTaskLater(plugin, () -> {
            Player player = server.getPlayer(playerUuid);
            if (player != null) {
                if (!VoiceMessagesSpigotNetworking.hasCompatibleVersion(player)) {
                    ServerConfig config = VoiceMessages.SERVER_CONFIG.getData();
                    if (config.modRequired()) {
                        player.kickPlayer(config.modNotInstalledText());
                    }
                }
            }
        }, 15L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        VoiceMessagesSpigotNetworking.onPlayerDisconnected(event.getPlayer());
    }

}

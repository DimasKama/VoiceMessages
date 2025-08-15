package ru.dimaskama.voicemessages.paper;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.config.ServerConfig;
import ru.dimaskama.voicemessages.paper.networking.VoiceMessagesPaperNetworking;

import java.util.UUID;

public record VoiceMessagesPaperListener(VoiceMessagesPaper plugin) implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        Server server = event.getPlayer().getServer();
        Runnable modPresenceCheck = () -> {
            Player player = server.getPlayer(playerUuid);
            if (player != null) {
                if (!VoiceMessagesPaperNetworking.hasCompatibleVersion(player)) {
                    ServerConfig config = VoiceMessages.SERVER_CONFIG.getData();
                    if (config.modRequired()) {
                        player.kickPlayer(config.modNotInstalledText());
                    }
                }
            }
        };
        if (VoiceMessagesPaper.isFolia()) {
            server
                    .getGlobalRegionScheduler()
                    .runDelayed(plugin, t -> modPresenceCheck.run(), 15L);
        } else {
            server
                    .getScheduler()
                    .runTaskLater(plugin, modPresenceCheck, 15L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        VoiceMessagesPaperNetworking.onPlayerDisconnected(event.getPlayer());
    }

}

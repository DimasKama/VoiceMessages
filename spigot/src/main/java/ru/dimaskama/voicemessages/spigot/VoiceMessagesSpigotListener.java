package ru.dimaskama.voicemessages.spigot;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.config.ServerConfig;
import ru.dimaskama.voicemessages.spigot.networking.VoiceMessagesPermissionsS2C;
import ru.dimaskama.voicemessages.spigot.networking.VoiceMessagesSpigotNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record VoiceMessagesSpigotListener(VoiceMessagesSpigot plugin) implements Listener {

    private static final Map<UUID, Boolean> LAST_SEND_PERMISSIONS = new HashMap<>();

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
        LAST_SEND_PERMISSIONS.remove(event.getPlayer().getUniqueId());
        VoiceMessagesSpigotNetworking.onPlayerDisconnected(event.getPlayer());
    }

    public static void updatePermissions(Plugin plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (VoiceMessagesSpigotNetworking.hasCompatibleVersion(player)) {
                boolean sendPermission = player.hasPermission(VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION);
                Boolean oldSendPermission = LAST_SEND_PERMISSIONS.put(player.getUniqueId(), sendPermission);
                if (oldSendPermission == null || sendPermission != oldSendPermission) {
                    VoiceMessagesSpigotNetworking.sendPermissions(
                            player,
                            plugin,
                            new VoiceMessagesPermissionsS2C(sendPermission)
                    );
                }
            }
        }
    }

}

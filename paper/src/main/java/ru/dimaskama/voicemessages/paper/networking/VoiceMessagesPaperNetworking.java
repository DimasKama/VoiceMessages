package ru.dimaskama.voicemessages.paper.networking;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesUtil;
import ru.dimaskama.voicemessages.api.ModifyAvailableTargetsCallback;
import ru.dimaskama.voicemessages.api.VoiceMessageReceivedCallback;
import ru.dimaskama.voicemessages.config.Punishment;
import ru.dimaskama.voicemessages.paper.VoiceMessagesPaper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ru.dimaskama.voicemessages.paper.networking.PacketUtils.*;

public final class VoiceMessagesPaperNetworking {

    public static final String VOICE_MESSAGES_VERSION_C2S = VoiceMessagesPaper.id("version");
    public static final String VOICE_MESSAGE_CHUNK_S2C_CHANNEL = VoiceMessagesPaper.id("voice_message_chunk_s2c");
    public static final String VOICE_MESSAGE_END_S2C_CHANNEL = VoiceMessagesPaper.id("voice_message_end_s2c");
    public static final String VOICE_MESSAGE_CHUNK_C2S_CHANNEL = VoiceMessagesPaper.id("voice_message_chunk_c2s");
    public static final String VOICE_MESSAGE_END_C2S_CHANNEL = VoiceMessagesPaper.id("voice_message_end_c2s");
    private static final Set<UUID> HAS_COMPATIBLE_VERSION = Sets.newConcurrentHashSet();
    private static final ListMultimap<UUID, String> AVAILABLE_TARGETS = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private static final Map<UUID, Long> VOICE_MESSAGES_TIMES = new ConcurrentHashMap<>();
    private static final Map<UUID, VoiceMessageBuilder> VOICE_MESSAGE_BUILDERS = new ConcurrentHashMap<>();

    public static void init(Plugin plugin) {
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, VOICE_MESSAGES_VERSION_C2S, (channel, player, message) -> onVoiceMessagesVersionReceived(player, message));
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, VoiceMessagesConfigS2C.CHANNEL);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, VoiceMessageTargetsS2C.CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, VOICE_MESSAGE_CHUNK_C2S_CHANNEL, (channel, player, message) -> onVoiceMessageChunkReceived(player, message));
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, VOICE_MESSAGE_END_C2S_CHANNEL, (channel, player, message) -> onVoiceMessageEndReceived(player, message));
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, VOICE_MESSAGE_CHUNK_S2C_CHANNEL);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, VOICE_MESSAGE_END_S2C_CHANNEL);
    }

    public static void sendConfig(Player player, VoiceMessagesConfigS2C config) {
        player.sendPluginMessage(VoiceMessagesPaper.getInstance(), VoiceMessagesConfigS2C.CHANNEL, config.encode());
    }

    public static void updateTargets() {
        for (UUID playerUuid : HAS_COMPATIBLE_VERSION) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                updateTargets(player);
            }
        }
    }

    public static void updateTargets(Player player) {
        List<String> targets = AVAILABLE_TARGETS.get(player.getUniqueId());
        synchronized (targets) {
            targets.clear();
            if (player.hasPermission(VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION)) {
                if (player.hasPermission(VoiceMessages.VOICE_MESSAGE_SEND_ALL_PERMISSION)) {
                    targets.add(VoiceMessages.TARGET_ALL);
                }
                if (player.hasPermission(VoiceMessages.VOICE_MESSAGE_SEND_TEAM_PERMISSION)) {
                    targets.add(VoiceMessages.TARGET_TEAM);
                }
                if (player.hasPermission(VoiceMessages.VOICE_MESSAGE_SEND_PLAYERS_PERMISSION)) {
                    Server server = player.getServer();
                    for (UUID playerUuid : HAS_COMPATIBLE_VERSION) {
                        Player p = server.getPlayer(playerUuid);
                        if (p != null) {
                            targets.add(p.getName());
                        }
                    }
                }
                ModifyAvailableTargetsCallback.EVENT.invoker().modifyAvailableTargets(player, targets);
            }
            player.sendPluginMessage(
                    VoiceMessagesPaper.getInstance(),
                    VoiceMessageTargetsS2C.CHANNEL,
                    new VoiceMessageTargetsS2C(List.copyOf(targets)).encode()
            );
        }
    }

    private static void onVoiceMessagesVersionReceived(Player player, byte[] message) {
        String version = readUtf8(message, 0).getFirst();
        if (VoiceMessages.isClientVersionCompatible(version)) {
            if (HAS_COMPATIBLE_VERSION.add(player.getUniqueId())) {
                addChannelsToPlayerWithReflection(player);
                sendConfig(player, new VoiceMessagesConfigS2C(VoiceMessages.SERVER_CONFIG.getData().maxVoiceMessageDurationMs()));
                updateTargets();
            } else {
                VoiceMessages.getLogger().warn(player.getName() + " sent his voicemessages mod version multiple times");
            }
        }
    }

    private static void addChannelsToPlayerWithReflection(Player player) {
        try {
            Class<?> craftPlayer = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftPlayer");
            Method addChannel = craftPlayer.getMethod("addChannel", String.class);
            Set<String> alreadyAdded = player.getListeningPluginChannels();
            if (!alreadyAdded.contains(VoiceMessagesConfigS2C.CHANNEL)) {
                addChannel.invoke(player, VoiceMessagesConfigS2C.CHANNEL);
            }
            if (!alreadyAdded.contains(VoiceMessageTargetsS2C.CHANNEL)) {
                addChannel.invoke(player, VoiceMessageTargetsS2C.CHANNEL);
            }
            if (!alreadyAdded.contains(VOICE_MESSAGE_CHUNK_S2C_CHANNEL)) {
                addChannel.invoke(player, VOICE_MESSAGE_CHUNK_S2C_CHANNEL);
            }
            if (!alreadyAdded.contains(VOICE_MESSAGE_END_S2C_CHANNEL)) {
                addChannel.invoke(player, VOICE_MESSAGE_END_S2C_CHANNEL);
            }
        } catch (Exception e) {
            VoiceMessages.getLogger().error("Failed to add plugin channels with reflection. Voice Messages may work broken!", e);
        }
    }

    public static boolean hasCompatibleVersion(Player player) {
        return hasCompatibleVersion(player.getUniqueId());
    }

    public static boolean hasCompatibleVersion(UUID playerUuid) {
        return HAS_COMPATIBLE_VERSION.contains(playerUuid);
    }

    public static void onPlayerDisconnected(Player player) {
        AVAILABLE_TARGETS.removeAll(player.getUniqueId());
        if (HAS_COMPATIBLE_VERSION.remove(player.getUniqueId())) {
            updateTargets();
        }
    }

    private static void onVoiceMessageChunkReceived(Player sender, byte[] message) {
        if (!hasCompatibleVersion(sender)) {
            VoiceMessages.getLogger().warn(sender.getName() + " sent voice message chunk without compatible VoiceMessages modVersion");
            return;
        }

        if (!sender.hasPermission(VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION)) {
            Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
            VoiceMessages.getLogger().warn(sender.getName() + " sent voice message chunk without " + VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION + " permission. Punishment: " + punishment.asString());
            switch (punishment) {
                case KICK:
                    sender.kick(Component.text("Voice messages permission is violated"));
                case PREVENT:
                    return;
            }
        }

        List<byte[]> voiceMessage = readVoiceMessage(message, 0).getFirst();
        VoiceMessageBuilder builder = VOICE_MESSAGE_BUILDERS.computeIfAbsent(sender.getUniqueId(), VoiceMessageBuilder::new);
        synchronized (builder) {
            if (!builder.discarded) {
                builder.appendChunk(voiceMessage);
                int duration = builder.getDuration();

                int maxDuration = VoiceMessages.SERVER_CONFIG.getData().maxVoiceMessageDurationMs();
                if (duration > maxDuration) {
                    Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
                    VoiceMessages.getLogger().warn("Building voice message exceeds the max duration of " + maxDuration + "ms. Punishment: " + punishment.asString());
                    switch (punishment) {
                        case KICK:
                            sender.kick(Component.text("You sent an invalid voice message"));
                        case PREVENT:
                            builder.discarded = true;
                            return;
                    }
                }
            }
        }
    }

    private static void onVoiceMessageEndReceived(Player sender, byte[] end) {
        VoiceMessageBuilder builder = VOICE_MESSAGE_BUILDERS.remove(sender.getUniqueId());
        if (builder != null) {
            String target = readUtf8(end, 0).getFirst();
            synchronized (builder) {
                if (!builder.discarded) {
                    int duration = builder.getDuration();
                    VoiceMessages.getLogger().info("Received voice message (" +  duration + "ms) from " + sender.getName());

                    long currentTime = System.currentTimeMillis();
                    Long lastTime = VOICE_MESSAGES_TIMES.put(sender.getUniqueId(), currentTime);
                    if (lastTime != null) {
                        int timePassed = (int) (currentTime - lastTime);
                        if (duration - timePassed > 100) {
                            Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageSpamPunishment();
                            VoiceMessages.getLogger().warn("Received voice message with duration (" + duration + "ms) greater than time passed from previous voice message (" + timePassed + "ms). Punishment:" + punishment.asString());
                            switch (punishment) {
                                case KICK:
                                    sender.kick(Component.text("The length of the sent voice message is longer than the time elapsed since the previous one"));
                                case PREVENT:
                                    return;
                            }
                        }
                    }

                    sendVoiceMessage(sender, builder.getFrames(), target);
                }
            }
        } else {
            Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
            VoiceMessages.getLogger().warn("Received voice message end packet without previous chunks from " + sender.getName() + ". Punishment: " + punishment.asString());
            if (punishment == Punishment.KICK) {
                sender.kick(Component.text("You sent an invalid voice message"));
            }
        }
    }

    public static void sendVoiceMessage(Player sender, List<byte[]> message, String target) {
        UUID senderUuid = sender.getUniqueId();
        if (!AVAILABLE_TARGETS.containsEntry(senderUuid, target)) {
            Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
            VoiceMessages.getLogger().warn(sender.getName() + " sent voice message with unknown target. Punishment: " + punishment.asString());
            switch (punishment) {
                case KICK:
                    sender.kick(Component.text("Unknown voice message target"));
                case PREVENT:
                    return;
            }
        }
        if (!VoiceMessageReceivedCallback.EVENT.invoker().onVoiceMessageReceived(sender, message, target)) {
            sendVoiceMessage(senderUuid, collectPlayers(sender, target), message, target);
        }
    }

    private static Iterable<Player> collectPlayers(Player sender, String target) {
        Server server = sender.getServer();
        if (VoiceMessages.TARGET_ALL.equals(target)) {
            return List.copyOf(server.getOnlinePlayers());
        }
        if (VoiceMessages.TARGET_TEAM.equals(target)) {
            Team team = sender.getScoreboard().getEntryTeam(sender.getUniqueId().toString());
            if (team != null) {
                List<Player> players = new ArrayList<>();
                for (String playerUuidStr : team.getEntries()) {
                    Player player = server.getPlayer(UUID.fromString(playerUuidStr));
                    if (player != null) {
                        players.add(player);
                    }
                }
                return players;
            }
            return List.of(sender);
        }
        Player otherPlayer = server.getPlayer(target);
        if (otherPlayer != null && !sender.equals(otherPlayer)) {
            return List.of(sender, otherPlayer);
        }
        return List.of(sender);
    }

    public static void sendVoiceMessage(UUID senderUuid, Iterable<Player> players, List<byte[]> message, String displayTarget) {
        List<byte[]> chunks = VoiceMessagesUtil.splitToChunks(
                message,
                VoiceMessagesUtil.S2C_VOICE_MESSAGE_CHUNK_MAX_SIZE,
                ch -> {
                    byte[] bytes = new byte[16 + getVoiceMessageSize(ch)];
                    writeUuid(bytes, 0, senderUuid);
                    writeVoiceMessage(bytes, 16, ch);
                    return bytes;
                }
        );
        byte[] end;
        {
            byte[] encodedTarget = displayTarget.getBytes(StandardCharsets.UTF_8);
            end = new byte[16 + getVarIntSize(encodedTarget.length) + encodedTarget.length];
            writeUuid(end, 0, senderUuid);
            int pos = 16;
            pos += writeVarInt(end, pos, encodedTarget.length);
            System.arraycopy(encodedTarget, 0, end, pos, encodedTarget.length);
        }
        Plugin plugin = VoiceMessagesPaper.getInstance();
        for (Player player : players) {
            if (hasCompatibleVersion(player)) {
                for (byte[] chunk : chunks) {
                    player.sendPluginMessage(plugin, VOICE_MESSAGE_CHUNK_S2C_CHANNEL, chunk);
                }
                player.sendPluginMessage(plugin, VOICE_MESSAGE_END_S2C_CHANNEL, end);
            }
        }
    }

    public static void tickBuildingVoiceMessages() {
        VOICE_MESSAGE_BUILDERS.values().removeIf(b -> {
            int timeSinceStarted = b.getTimeSinceStarted();
            if (timeSinceStarted > 4000L) {
                VoiceMessages.getLogger().warn("Voice message from " + b.sender + " is transfering longer than 4000ms. Cleaning up");
                return true;
            }
            return false;
        });
    }

    private static class VoiceMessageBuilder {

        private final long startTime = System.currentTimeMillis();
        private final List<byte[]> frames = new ArrayList<>();
        private final UUID sender;
        public boolean discarded;

        private VoiceMessageBuilder(UUID sender) {
            this.sender = sender;
        }

        public void appendChunk(List<byte[]> chunk) {
            frames.addAll(chunk);
        }

        public int getDuration() {
            return frames.size() * 1000 / VoiceMessages.FRAMES_PER_SEC;
        }

        public int getTimeSinceStarted() {
            return (int) (System.currentTimeMillis() - startTime);
        }

        public List<byte[]> getFrames() {
            return frames;
        }

    }

}

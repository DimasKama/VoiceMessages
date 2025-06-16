package ru.dimaskama.voicemessages.spigot.networking;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesUtil;
import ru.dimaskama.voicemessages.config.Punishment;
import ru.dimaskama.voicemessages.spigot.VoiceMessagesSpigot;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ru.dimaskama.voicemessages.spigot.networking.PacketUtils.*;

public final class VoiceMessagesSpigotNetworking {

    public static final String VOICE_MESSAGES_VERSION_C2S = VoiceMessagesSpigot.id("version");
    public static final String VOICE_MESSAGE_CHUNK_S2C_CHANNEL = VoiceMessagesSpigot.id("voice_message_chunk_s2c");
    public static final String VOICE_MESSAGE_CHUNK_C2S_CHANNEL = VoiceMessagesSpigot.id("voice_message_chunk_c2s");
    private static final Set<UUID> HAS_COMPATIBLE_VERSION = Sets.newConcurrentHashSet();
    private static final Map<UUID, Long> VOICE_MESSAGES_TIMES = new ConcurrentHashMap<>();
    private static final Map<UUID, VoiceMessageBuilder> VOICE_MESSAGE_BUILDERS = new ConcurrentHashMap<>();

    public static void init(Plugin plugin) {
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, VOICE_MESSAGES_VERSION_C2S, (channel, player, message) -> onVoiceMessagesVersionReceived(plugin, player, message));
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, VoiceMessagesConfigS2C.CHANNEL);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, VoiceMessagesPermissionsS2C.CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, VOICE_MESSAGE_CHUNK_C2S_CHANNEL, (channel, player, message) -> onVoiceMessageReceived(plugin, player, message));
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, VOICE_MESSAGE_CHUNK_S2C_CHANNEL);
    }

    public static void sendConfig(Player player, Plugin plugin, VoiceMessagesConfigS2C config) {
        player.sendPluginMessage(plugin, VoiceMessagesConfigS2C.CHANNEL, config.encode());
    }

    public static void sendPermissions(Player player, Plugin plugin, VoiceMessagesPermissionsS2C permissions) {
        player.sendPluginMessage(plugin, VoiceMessagesPermissionsS2C.CHANNEL, permissions.encode());
    }

    private static void onVoiceMessagesVersionReceived(Plugin plugin, Player player, byte[] message) {
        String version = readUtf8(message, 0).getFirst();
        if (VoiceMessages.isClientVersionCompatible(version)) {
            if (HAS_COMPATIBLE_VERSION.add(player.getUniqueId())) {
                addChannelsToPlayerWithReflection(player);
                sendConfig(player, plugin, new VoiceMessagesConfigS2C(VoiceMessages.SERVER_CONFIG.getData().maxVoiceMessageDurationMs()));
                sendPermissions(player, plugin, new VoiceMessagesPermissionsS2C(player.hasPermission(VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION)));
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
            if (!alreadyAdded.contains(VoiceMessagesPermissionsS2C.CHANNEL)) {
                addChannel.invoke(player, VoiceMessagesPermissionsS2C.CHANNEL);
            }
            if (!alreadyAdded.contains(VOICE_MESSAGE_CHUNK_S2C_CHANNEL)) {
                addChannel.invoke(player, VOICE_MESSAGE_CHUNK_S2C_CHANNEL);
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
        HAS_COMPATIBLE_VERSION.remove(player.getUniqueId());
    }

    // Broadcast a voice message without decoding it but validating
    private static void onVoiceMessageReceived(Plugin plugin, Player sender, byte[] message) {
        if (!hasCompatibleVersion(sender)) {
            VoiceMessages.getLogger().warn(sender.getName() + " sent voice message chunk without compatible VoiceMessages modVersion");
            return;
        }

        if (!sender.hasPermission(VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION)) {
            Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
            VoiceMessages.getLogger().warn(sender.getName() + " sent voice message chunk without " + VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION + " permission. Punishment: " + punishment.asString());
            switch (punishment) {
                case KICK:
                    sender.kickPlayer("Voice messages permission is violated");
                case PREVENT:
                    return;
            }
        }

        Pair<List<byte[]>, Integer> readChunk = readVoiceMessage(message, 0);
        int extraBytes = message.length - readChunk.getSecond();
        if (extraBytes > 0) {
            throw new IllegalArgumentException(extraBytes + " extra bytes");
        }
        List<byte[]> chunk = readChunk.getFirst();

        if (VoiceMessagesUtil.isFlush(chunk)) {
            VoiceMessageBuilder builder = VOICE_MESSAGE_BUILDERS.remove(sender.getUniqueId());
            if (builder != null) {
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
                                        sender.kickPlayer("The length of the sent voice message is longer than the time elapsed since the previous one");
                                    case PREVENT:
                                        return;
                                }
                            }
                        }

                        List<byte[]> chunksS2C = builder.buildEncodedS2CChunks();
                        for (Player player : sender.getServer().getOnlinePlayers()) {
                            if (hasCompatibleVersion(player)) {
                                for (byte[] chunkS2C : chunksS2C) {
                                    player.sendPluginMessage(plugin, VOICE_MESSAGE_CHUNK_S2C_CHANNEL, chunkS2C);
                                }
                            }
                        }
                    }
                }
            } else {
                Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
                VoiceMessages.getLogger().warn("Received voice message flush packet without previous chunks from " + sender.getName() + ". Punishment: " + punishment.asString());
                if (punishment == Punishment.KICK) {
                    sender.kickPlayer("You sent an invalid voice message");
                }
            }
        } else {
            VoiceMessageBuilder builder = VOICE_MESSAGE_BUILDERS.computeIfAbsent(sender.getUniqueId(), VoiceMessageBuilder::new);
            synchronized (builder) {
                if (!builder.discarded) {
                    builder.appendChunk(chunk);
                    int duration = builder.getDuration();
                    int maxDuration = VoiceMessages.SERVER_CONFIG.getData().maxVoiceMessageDurationMs();
                    if (duration > maxDuration) {
                        Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
                        VoiceMessages.getLogger().warn("Building voice message exceeds the max duration of " + maxDuration + "ms. Punishment: " + punishment.asString());
                        switch (punishment) {
                            case KICK:
                                sender.kickPlayer("You sent an invalid voice message");
                            case PREVENT:
                                builder.discarded = true;
                                return;
                        }
                    }
                }
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

        public List<byte[]> buildEncodedS2CChunks() {
            return VoiceMessagesUtil.splitToChunks(frames, VoiceMessagesUtil.S2C_VOICE_MESSAGE_CHUNK_MAX_SIZE, ch -> {
                byte[] bytes = new byte[16 + getVoiceMessageSize(ch)];
                writeUuid(bytes, 0, sender);
                writeVoiceMessage(bytes, 16, ch);
                return bytes;
            });
        }

    }

}

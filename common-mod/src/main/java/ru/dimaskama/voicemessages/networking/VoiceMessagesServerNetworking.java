package ru.dimaskama.voicemessages.networking;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.scores.PlayerTeam;
import ru.dimaskama.voicemessages.*;
import ru.dimaskama.voicemessages.api.ModifyAvailableTargetsCallback;
import ru.dimaskama.voicemessages.api.VoiceMessageReceivedCallback;
import ru.dimaskama.voicemessages.config.Punishment;
import ru.dimaskama.voicemessages.config.ServerConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceMessagesServerNetworking {

    private static final Set<UUID> HAS_COMPATIBLE_VERSION = Sets.newConcurrentHashSet();
    private static final ListMultimap<UUID, String> AVAILABLE_TARGETS = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private static final Map<UUID, Long> VOICE_MESSAGES_TIMES = new ConcurrentHashMap<>();
    private static final Map<UUID, VoiceMessageBuilder> VOICE_MESSAGE_BUILDERS = new ConcurrentHashMap<>();

    public static void onVoiceMessagesVersionReceived(ServerPlayer sender, VoiceMessagesVersionC2S version) {
        if (VoiceMessages.isClientVersionCompatible(version.modVersion())) {
            if (HAS_COMPATIBLE_VERSION.add(sender.getUUID())) {
                VoiceMessagesModService service = VoiceMessagesMod.getService();
                ServerConfig config = VoiceMessages.SERVER_CONFIG.getData();
                service.sendToPlayer(sender, new VoiceMessagesConfigS2C(config.maxVoiceMessageDurationMs()));
                updateTargets(sender.server);
            } else {
                VoiceMessages.getLogger().warn(sender.getGameProfile().getName() + " sent his voicemessages modVersion multiple times");
            }
        }
    }

    public static void onVoiceMessageChunkReceived(ServerPlayer sender, VoiceMessageChunkC2S chunk) {
        if (!hasCompatibleVersion(sender)) {
            VoiceMessages.getLogger().warn(sender.getGameProfile().getName() + " sent voice message chunk without compatible VoiceMessages modVersion");
            return;
        }

        if (!VoiceMessagesMod.getService().hasVoiceMessageSendPermission(sender)) {
            Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
            VoiceMessages.getLogger().warn(sender.getGameProfile().getName() + " sent voice message chunk without " + VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION + " permission. Punishment: " + punishment.asString());
            switch (punishment) {
                case KICK:
                    sender.connection.disconnect(Component.translatable("voicemessages.kick.permission_violated"));
                case PREVENT:
                    return;
            }
        }

        VoiceMessageBuilder builder = VOICE_MESSAGE_BUILDERS.computeIfAbsent(sender.getUUID(), VoiceMessageBuilder::new);
        synchronized (builder) {
            if (!builder.discarded) {
                builder.appendChunk(chunk.encodedAudio());
                int duration = builder.getDuration();

                int maxDuration = VoiceMessages.SERVER_CONFIG.getData().maxVoiceMessageDurationMs();
                if (duration > maxDuration) {
                    Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
                    VoiceMessages.getLogger().warn("Building voice message exceeds the max duration of " + maxDuration + "ms. Punishment: " + punishment.asString());
                    switch (punishment) {
                        case KICK:
                            sender.connection.disconnect(Component.translatable("voicemessages.kick.invalid"));
                        case PREVENT:
                            builder.discarded = true;
                            return;
                    }
                }
            }
        }
    }

    public static void onVoiceMessageEndReceived(ServerPlayer sender, VoiceMessageEndC2S end) {
        VoiceMessageBuilder builder = VOICE_MESSAGE_BUILDERS.remove(sender.getUUID());
        if (builder != null) {
            synchronized (builder) {
                if (!builder.discarded) {
                    int duration = builder.getDuration();
                    VoiceMessages.getLogger().info("Received voice message (" +  duration + "ms) from " + sender.getGameProfile().getName());

                    long currentTime = System.currentTimeMillis();
                    Long lastTime = VOICE_MESSAGES_TIMES.put(sender.getUUID(), currentTime);
                    if (lastTime != null) {
                        int timePassed = (int) (currentTime - lastTime);
                        if (duration - timePassed > 100) {
                            Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageSpamPunishment();
                            VoiceMessages.getLogger().warn("Received voice message with duration (" + duration + "ms) greater than time passed from previous voice message (" + timePassed + "ms). Punishment:" + punishment.asString());
                            switch (punishment) {
                                case KICK:
                                    sender.connection.disconnect(Component.translatable("voicemessages.kick.spam"));
                                case PREVENT:
                                    return;
                            }
                        }
                    }

                    sendVoiceMessage(sender, builder.getFrames(), end.target());
                }
            }
        } else {
            Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
            VoiceMessages.getLogger().warn("Received voice message end packet without previous chunks from " + sender.getGameProfile().getName() + ". Punishment: " + punishment.asString());
            if (punishment == Punishment.KICK) {
                sender.connection.disconnect(Component.translatable("voicemessages.kick.invalid"));
            }
        }
    }

    public static void sendVoiceMessage(ServerPlayer sender, List<byte[]> message, String target) {
        UUID senderUuid = sender.getUUID();
        if (!AVAILABLE_TARGETS.containsEntry(senderUuid, target)) {
            Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
            VoiceMessages.getLogger().warn(sender.getGameProfile().getName() + " sent voice message with unknown target. Punishment: " + punishment.asString());
            switch (punishment) {
                case KICK:
                    sender.connection.disconnect(Component.translatable("voicemessages.kick.unknown_target"));
                case PREVENT:
                    return;
            }
        }
        if (!VoiceMessageReceivedCallback.EVENT.invoker().onVoiceMessageReceived(sender, message, target)) {
            sendVoiceMessage(senderUuid, collectPlayers(sender, target), message, target);
        }
    }

    private static Iterable<ServerPlayer> collectPlayers(ServerPlayer sender, String target) {
        if (VoiceMessages.TARGET_ALL.equals(target)) {
            return List.copyOf(sender.server.getPlayerList().getPlayers());
        }
        if (VoiceMessages.TARGET_TEAM.equals(target)) {
            PlayerTeam team = sender.getTeam();
            if (team != null) {
                PlayerList playerList = sender.server.getPlayerList();
                List<ServerPlayer> players = new ArrayList<>();
                for (String playerUuidStr : team.getPlayers()) {
                    ServerPlayer player = playerList.getPlayer(UUID.fromString(playerUuidStr));
                    if (player != null) {
                        players.add(player);
                    }
                }
                return players;
            }
            return List.of(sender);
        }
        ServerPlayer otherPlayer = sender.server.getPlayerList().getPlayerByName(target);
        if (otherPlayer != null && !sender.equals(otherPlayer)) {
            return List.of(sender, otherPlayer);
        }
        return List.of(sender);
    }

    public static void sendVoiceMessage(UUID senderUuid, Iterable<ServerPlayer> players, List<byte[]> message, String displayTarget) {
        List<VoiceMessageChunkS2C> chunks = VoiceMessagesUtil.splitToChunks(
                message,
                VoiceMessagesUtil.S2C_VOICE_MESSAGE_CHUNK_MAX_SIZE,
                ch -> new VoiceMessageChunkS2C(senderUuid, ch)
        );
        VoiceMessageEndS2C end = new VoiceMessageEndS2C(senderUuid, displayTarget);
        VoiceMessagesModService service = VoiceMessagesMod.getService();
        for (ServerPlayer player : players) {
            if (hasCompatibleVersion(player)) {
                for (VoiceMessageChunkS2C chunk : chunks) {
                    service.sendToPlayer(player, chunk);
                }
                service.sendToPlayer(player, end);
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

    public static boolean hasCompatibleVersion(ServerPlayer player) {
        return hasCompatibleVersion(player.getUUID());
    }

    public static boolean hasCompatibleVersion(UUID playerUuid) {
        return HAS_COMPATIBLE_VERSION.contains(playerUuid);
    }

    public static void updateTargets(MinecraftServer server) {
        PlayerList playerList = server.getPlayerList();
        for (UUID p : HAS_COMPATIBLE_VERSION) {
            ServerPlayer player = playerList.getPlayer(p);
            if (player != null) {
                updateTargets(player);
            }
        }
    }

    public static void updateTargets(ServerPlayer player) {
        VoiceMessagesModService service = VoiceMessagesMod.getService();
        List<String> targets = AVAILABLE_TARGETS.get(player.getUUID());
        synchronized (targets) {
            targets.clear();
            if (service.hasVoiceMessageSendPermission(player)) {
                if (service.hasVoiceMessageSendAllPermission(player)) {
                    targets.add(VoiceMessages.TARGET_ALL);
                }
                if (service.hasVoiceMessageSendTeamPermission(player)) {
                    targets.add(VoiceMessages.TARGET_TEAM);
                }
                if (service.hasVoiceMessageSendPlayersPermission(player)) {
                    PlayerList playerList = player.server.getPlayerList();
                    for (UUID playerUuid : HAS_COMPATIBLE_VERSION) {
                        ServerPlayer p = playerList.getPlayer(playerUuid);
                        if (p != null) {
                            targets.add(p.getGameProfile().getName());
                        }
                    }
                }
                ModifyAvailableTargetsCallback.EVENT.invoker().modifyAvailableTargets(player, targets);
            }
            service.sendToPlayer(player, new VoiceMessageTargetsS2C(List.copyOf(targets)));
        }
    }

    public static void onPlayerDisconnected(MinecraftServer server, UUID playerUuid) {
        AVAILABLE_TARGETS.removeAll(playerUuid);
        if (HAS_COMPATIBLE_VERSION.remove(playerUuid)) {
            updateTargets(server);
        }
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

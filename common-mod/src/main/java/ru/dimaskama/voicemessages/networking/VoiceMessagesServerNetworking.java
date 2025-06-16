package ru.dimaskama.voicemessages.networking;

import com.google.common.collect.Sets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesEvents;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.VoiceMessagesModService;
import ru.dimaskama.voicemessages.config.Punishment;
import ru.dimaskama.voicemessages.config.ServerConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceMessagesServerNetworking {

    private static final Set<UUID> HAS_COMPATIBLE_VERSION = Sets.newConcurrentHashSet();
    private static final Map<UUID, Long> VOICE_MESSAGES_TIMES = new ConcurrentHashMap<>();
    private static final Map<UUID, VoiceMessageBuilder> VOICE_MESSAGE_BUILDERS = new ConcurrentHashMap<>();

    public static void onVoiceMessagesVersionReceived(ServerPlayer sender, VoiceMessagesVersionC2S version) {
        if (VoiceMessages.isClientVersionCompatible(version.modVersion())) {
            if (HAS_COMPATIBLE_VERSION.add(sender.getUUID())) {
                VoiceMessagesModService service = VoiceMessagesMod.getService();
                ServerConfig config = VoiceMessages.SERVER_CONFIG.getData();
                service.sendToPlayer(sender, new VoiceMessagesConfigS2C(config.maxVoiceMessageDurationMs()));
                VoiceMessagesEvents.sendPermissions(sender);
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

        if (chunk.isFlush()) {
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

                        List<VoiceMessageChunkS2C> chunks = builder.buildS2CChunks();
                        VoiceMessagesModService service = VoiceMessagesMod.getService();
                        for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
                            if (hasCompatibleVersion(player)) {
                                for (VoiceMessageChunkS2C chunkS2C : chunks) {
                                    service.sendToPlayer(player, chunkS2C);
                                }
                            }
                        }
                    }
                }
            } else {
                Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
                VoiceMessages.getLogger().warn("Received voice message flush packet without previous chunks from " + sender.getGameProfile().getName() + ". Punishment: " + punishment.asString());
                if (punishment == Punishment.KICK) {
                    sender.connection.disconnect(Component.translatable("voicemessages.kick.invalid"));
                }
            }
        } else {
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

    public static void onPlayerDisconnected(UUID playerUuid) {
        HAS_COMPATIBLE_VERSION.remove(playerUuid);
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

        public List<VoiceMessageChunkS2C> buildS2CChunks() {
            return VoiceMessageChunkS2C.split(sender, frames);
        }

    }

}

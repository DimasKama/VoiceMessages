package ru.dimaskama.voicemessages.client.networking;

import com.mojang.util.UndashedUuid;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesPlugin;
import ru.dimaskama.voicemessages.client.GuiMessageTagHack;
import ru.dimaskama.voicemessages.networking.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceMessagesClientNetworking {

    private static final Map<UUID, VoiceMessageBuilder> VOICE_MESSAGE_BUILDERS = new ConcurrentHashMap<>();
    private static List<String> availableTargets = List.of();
    private static int maxVoiceMessageDurationMs = VoiceMessages.MAX_VOICE_MESSAGE_DURATION_MS;
    private static int maxVoiceMessageFrames = VoiceMessages.MAX_VOICE_MESSAGE_FRAMES;

    public static void onConfigReceived(VoiceMessagesConfigS2C config) {
        VoiceMessages.getLogger().info("Received voice messages config");
        maxVoiceMessageDurationMs = config.maxVoiceMessageDurationMs();
        maxVoiceMessageFrames = maxVoiceMessageDurationMs * VoiceMessages.FRAMES_PER_SEC / 1000;
    }

    public static void onTargetsReceived(VoiceMessageTargetsS2C targets) {
        availableTargets = targets.targets();
    }

    public static void onVoiceMessageChunkReceived(VoiceMessageChunkS2C chunk) {
        UUID senderUuid = chunk.sender();
        VoiceMessageBuilder builder = VOICE_MESSAGE_BUILDERS.computeIfAbsent(senderUuid, VoiceMessageBuilder::new);
        synchronized (builder) {
            try {
                builder.appendChunk(chunk.encodedAudio());
            } catch (Exception e) {
                VoiceMessages.getLogger().warn("Failed to decode voice message chunk", e);
            }
        }
    }

    public static void onVoiceMessageEndReceived(VoiceMessageEndS2C end) {
        UUID senderUuid = end.sender();
        String target = end.target();
        VoiceMessageBuilder builder = VOICE_MESSAGE_BUILDERS.remove(senderUuid);
        if (builder != null) {
            synchronized (builder) {
                int duration = builder.getDuration();
                List<short[]> audio = builder.getFrames();
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.execute(() -> {
                    Component text;
                    PlayerInfo sender = minecraft.getConnection().getPlayerInfo(senderUuid);
                    if (sender != null) {
                        text = sender.getTabListDisplayName();
                        if (text == null) {
                            text = Component.literal(sender.getProfile().getName());
                        }
                        VoiceMessages.getLogger().info("(Client) Received voice message (" +  duration + "ms) from " + sender.getProfile().getName());
                    } else {
                        text = Component.empty();
                        VoiceMessages.getLogger().info("(Client) Received voice message (" + duration + "ms) from unknown player (" + UndashedUuid.toString(senderUuid) + ")");
                    }
                    if (!VoiceMessages.TARGET_ALL.equals(target)) {
                        Component targetName = null;
                        if (VoiceMessages.TARGET_TEAM.equals(target)) {
                            targetName = Component.translatable("voicemessages.target.team");
                        } else {
                            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(target);
                            if (playerInfo != null) {
                                targetName = playerInfo.getTabListDisplayName();
                            }
                            if (targetName == null) {
                                targetName = Component.literal(target);
                            }
                        }
                        text = Component.empty()
                                .append(text)
                                .append(" → ")
                                .append(targetName);
                    }
                    minecraft.gui.getChat().addMessage(text, null, GuiMessageTagHack.createAndAdd(audio));
                });
                builder.close();
            }
        } else {
            VoiceMessages.getLogger().warn("Received voice message end packet without previous chunks");
        }
    }

    public static void tickBuildingVoiceMessages() {
        VOICE_MESSAGE_BUILDERS.values().removeIf(b -> {
            synchronized (b) {
                int timeSinceStarted = b.getTimeSinceStarted();
                if (timeSinceStarted > 5000L) {
                    VoiceMessages.getLogger().warn("Voice message from " + b.sender + " is transfering longer than 5000ms. Cleaning up");
                    b.close();
                    return true;
                }
            }
            return false;
        });
    }

    public static List<String> getAvailableTargets() {
        return availableTargets;
    }

    public static int getMaxVoiceMessageDurationMs() {
        return maxVoiceMessageDurationMs;
    }

    public static int getMaxVoiceMessageFrames() {
        return maxVoiceMessageFrames;
    }

    public static void resetConfig() {
        availableTargets = List.of();
        maxVoiceMessageDurationMs = VoiceMessages.MAX_VOICE_MESSAGE_DURATION_MS;
        maxVoiceMessageFrames = VoiceMessages.MAX_VOICE_MESSAGE_FRAMES;
    }

    private static class VoiceMessageBuilder implements AutoCloseable {

        private final long startTime = System.currentTimeMillis();
        private final List<short[]> frames = new ArrayList<>();
        private final OpusDecoder opusDecoder = VoiceMessagesPlugin.getClientApi().createDecoder();
        private final UUID sender;

        private VoiceMessageBuilder(UUID sender) {
            this.sender = sender;
        }

        public void appendChunk(List<byte[]> chunk) {
            frames.addAll(VoiceMessagesPlugin.decodeList(opusDecoder, chunk));
        }

        public int getDuration() {
            return frames.size() * 1000 / VoiceMessages.FRAMES_PER_SEC;
        }

        public int getTimeSinceStarted() {
            return (int) (System.currentTimeMillis() - startTime);
        }

        public List<short[]> getFrames() {
            return frames;
        }

        @Override
        public void close() {
            opusDecoder.close();
        }

    }

}

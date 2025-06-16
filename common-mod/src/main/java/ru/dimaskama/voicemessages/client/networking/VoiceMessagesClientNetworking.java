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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceMessagesClientNetworking {

    private static final Map<UUID, VoiceMessageBuilder> VOICE_MESSAGE_BUILDERS = new ConcurrentHashMap<>();
    private static boolean canSendVoiceMessages;
    private static int maxVoiceMessageDurationMs = VoiceMessages.MAX_VOICE_MESSAGE_DURATION_MS;
    private static int maxVoiceMessageFrames = VoiceMessages.MAX_VOICE_MESSAGE_FRAMES;

    public static void onVoiceMessagesConfigReceived(VoiceMessagesConfigS2C config) {
        VoiceMessages.getLogger().info("Received voice messages config");
        maxVoiceMessageDurationMs = config.maxVoiceMessageDurationMs();
        maxVoiceMessageFrames = maxVoiceMessageDurationMs * VoiceMessages.FRAMES_PER_SEC / 1000;
    }

    public static void onVoiceMessagesPermissionsReceived(VoiceMessagesPermissionsS2C permissions) {
        canSendVoiceMessages = permissions.send();
    }

    public static void onVoiceMessageChunkReceived(VoiceMessageChunkS2C message) {
        UUID senderUuid = message.sender();
        if (message.isFlush()) {
            VoiceMessageBuilder builder = VOICE_MESSAGE_BUILDERS.remove(senderUuid);
            if (builder != null) {
                synchronized (builder) {
                    int duration = builder.getDuration();
                    List<short[]> audio = builder.getFrames();
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.execute(() -> {
                        PlayerInfo sender = minecraft.getConnection().getPlayerInfo(senderUuid);
                        Component name;
                        if (sender != null) {
                            name = sender.getTabListDisplayName();
                            if (name == null) {
                                name = Component.literal(sender.getProfile().getName());
                            }
                            VoiceMessages.getLogger().info("(Client) Received voice message (" +  duration + "ms) from " + sender.getProfile().getName());
                        } else {
                            name = Component.empty();
                            VoiceMessages.getLogger().info("(Client) Received voice message (" + duration + "ms) from unknown player (" + UndashedUuid.toString(senderUuid) + ")");
                        }
                        minecraft.gui.getChat().addMessage(name, null, GuiMessageTagHack.createPlayback(audio));
                    });
                    builder.close();
                }
            } else {
                VoiceMessages.getLogger().warn("Received voice message flush packet without previous chunks");
            }
        } else {
            VoiceMessageBuilder builder = VOICE_MESSAGE_BUILDERS.computeIfAbsent(senderUuid, VoiceMessageBuilder::new);
            synchronized (builder) {
                try {
                    builder.appendChunk(message.encodedAudio());
                } catch (Exception e) {
                    VoiceMessages.getLogger().warn("Failed to decode voice message chunk", e);
                }
            }
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

    public static boolean canSendVoiceMessages() {
        return canSendVoiceMessages;
    }

    public static int getMaxVoiceMessageDurationMs() {
        return maxVoiceMessageDurationMs;
    }

    public static int getMaxVoiceMessageFrames() {
        return maxVoiceMessageFrames;
    }

    public static void resetConfig() {
        canSendVoiceMessages = false;
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

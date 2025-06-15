package ru.dimaskama.voicemessages.client.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesPlugin;
import ru.dimaskama.voicemessages.client.GuiMessageTagHack;
import ru.dimaskama.voicemessages.networking.VoiceMessageS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessagesConfigS2C;
import ru.dimaskama.voicemessages.networking.VoiceMessagesPermissionsS2C;

import java.util.List;

public final class VoiceMessagesClientNetworking {

    private static boolean canSendVoiceMessages;
    private static int maxVoiceMessageDurationMs;
    private static int maxVoiceMessageFrames;

    public static void onVoiceMessagesConfigReceived(VoiceMessagesConfigS2C config) {
        VoiceMessages.LOGGER.info("Received voice messages config");
        maxVoiceMessageDurationMs = config.maxVoiceMessageDurationMs();
        maxVoiceMessageFrames = maxVoiceMessageDurationMs * VoiceMessages.FRAMES_PER_SEC / 1000;
    }

    public static void onVoiceMessagesPermissionsReceived(VoiceMessagesPermissionsS2C permissions) {
        canSendVoiceMessages = permissions.send();
    }

    public static void onVoiceMessageReceived(VoiceMessageS2C message) {
        List<short[]> audio;
        try {
            audio = VoiceMessagesPlugin.getClientOpus().decode(message.encodedAudio());
        } catch (Exception e) {
            VoiceMessages.LOGGER.error("Failed to decode voice message received from server", e);
            return;
        }
        int duration = 1000 * audio.size() / VoiceMessages.FRAMES_PER_SEC;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            PlayerInfo sender = minecraft.getConnection().getPlayerInfo(message.sender());
            Component name;
            if (sender != null) {
                name = sender.getTabListDisplayName();
                if (name == null) {
                    name = Component.literal(sender.getProfile().getName());
                }
                VoiceMessages.LOGGER.info("(Client) Received voice message ({}ms) from {}", duration, sender.getProfile().getName());
            } else {
                name = Component.empty();
                VoiceMessages.LOGGER.info("(Client) Received voice message ({}ms) from unknown player", duration);
            }
            minecraft.gui.getChat().addMessage(name, null, GuiMessageTagHack.createPlayback(audio));
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

}

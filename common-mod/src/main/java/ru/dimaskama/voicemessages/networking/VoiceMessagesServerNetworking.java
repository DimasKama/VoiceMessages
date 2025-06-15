package ru.dimaskama.voicemessages.networking;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.VoiceMessagesModService;
import ru.dimaskama.voicemessages.config.Punishment;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceMessagesServerNetworking {

    private static final Map<UUID, Long> VOICE_MESSAGES_TIMES = new ConcurrentHashMap<>();

    public static void onVoiceMessageReceived(ServerPlayer sender, VoiceMessageC2S message) {
        int duration = message.encodedAudio().size() * 1000 / VoiceMessages.FRAMES_PER_SEC;
        VoiceMessages.LOGGER.info("Received voice message ({}ms) from {}", duration, sender.getGameProfile().getName());

        if (!VoiceMessagesMod.getService().hasVoiceMessageSendPermission(sender)) {
            Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
            VoiceMessages.LOGGER.info("Received voice message from player that has no {} permission. Punishment: {}", VoiceMessages.VOICE_MESSAGE_SEND_PERMISSION, punishment.asString());
            switch (punishment) {
                case KICK:
                    sender.connection.disconnect(Component.translatable("voicemessages.kick.permission_violated"));
                case PREVENT:
                    return;
            }
        }

        long currentTime = System.currentTimeMillis();
        Long lastTime = VOICE_MESSAGES_TIMES.put(sender.getUUID(), currentTime);
        if (lastTime != null) {
            int timePassed = (int) (currentTime - lastTime);
            if (duration - timePassed > 100) {
                Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageSpamPunishment();
                VoiceMessages.LOGGER.info("Received voice message with duration ({}ms) greater than time passed from previous voice message ({}ms). Punishment: {}", duration, timePassed, punishment.asString());
                switch (punishment) {
                    case KICK:
                        sender.connection.disconnect(Component.translatable("voicemessages.kick.spam"));
                    case PREVENT:
                        return;
                }
            }
        }

        int maxDuration = VoiceMessages.SERVER_CONFIG.getData().maxVoiceMessageDurationMs();
        if (duration > maxDuration) {
            Punishment punishment = VoiceMessages.SERVER_CONFIG.getData().voiceMessageInvalidPunishment();
            VoiceMessages.LOGGER.info("Voice message of {}ms exceeds the max duration of {}ms. Punishment: {}", duration, maxDuration, punishment.asString());
            switch (punishment) {
                case KICK:
                    sender.connection.disconnect(Component.translatable("voicemessages.kick.invalid"));
                case PREVENT:
                    return;
            }
        }

        VoiceMessageS2C packet = new VoiceMessageS2C(sender.getUUID(), message.encodedAudio());
        VoiceMessagesModService service = VoiceMessagesMod.getService();
        for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
            if (service.canSendToPlayer(player, VoiceMessageS2C.TYPE.id())) {
                service.sendToPlayer(player, packet);
            }
        }
    }

}

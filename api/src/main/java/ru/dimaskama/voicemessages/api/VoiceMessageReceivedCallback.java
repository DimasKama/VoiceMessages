package ru.dimaskama.voicemessages.api;

import java.util.List;

/**
 * A callback for custom handing the voice message received from player to server
 */
@FunctionalInterface
public interface VoiceMessageReceivedCallback {

    /**
     * The event for this callback
     */
    Event<VoiceMessageReceivedCallback> EVENT = new Event<>(
            VoiceMessageReceivedCallback.class,
            callbacks -> (player, message, target) -> {
                for (VoiceMessageReceivedCallback callback : callbacks) {
                    if (callback.onVoiceMessageReceived(player, message, target)) {
                        return true;
                    }
                }
                return false;
            }
    );

    /**
     * @param sender a server player object of your framework, the sender of this voice message
     * @param message the voice message. A list of audio frames encoded with opus
     * @param targetName the name of the target chosen by the sender
     * @return {@code true} to mark the voice message as handled and cancel further handling. {@code false} otherwise
     */
    boolean onVoiceMessageReceived(Object sender, List<byte[]> message, String targetName);

}

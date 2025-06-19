package ru.dimaskama.voicemessages.api;

import java.util.List;
import java.util.UUID;

/**
 * Voice Messages API access
 * <p>
 *     To get an instance of it, you should use {@code VoiceMessagesApiInitCallback}
 * </p>
 */
public interface VoiceMessagesApi {

    /**
     * @param playerUuid player's UUID
     * @return {@code true} if the player with provided UUID is online
     * and has the compatible version of Voice Messages mod installed
     */
    boolean isPlayerHasCompatibleModVersion(UUID playerUuid);

    /**
     * Updates a list of available voice message targets for the player
     * <p>
     *     Use this if you want to modify player's available
     *     targets with {@code ModifyAvailableTargetsCallback}
     * </p>
     * @param playerUuid player's UUID
     * @return {@code true} if the player with provided UUID is online
     * and has the compatible version of Voice Messages mod installed
     * and available targets have been updated successfully
     */
    boolean updateAvailableTargets(UUID playerUuid);

    /**
     * Sends a voice message to all players in {@code playerUuids}
     * <p>
     *     {@code VoiceMessageReceivedCallback} will not be invoked
     * </p>
     * @param senderUuid UUID of the sender of this voice message, whose name will be displayed in chat
     * @param playerUuids list of players that will receive this voice message
     * @param message the voice message. A list of audio frames encoded with opus
     * @param displayTarget name of the target that will be displayed in chat
     */
    void sendVoiceMessage(UUID senderUuid, Iterable<UUID> playerUuids, List<byte[]> message, String displayTarget);

}

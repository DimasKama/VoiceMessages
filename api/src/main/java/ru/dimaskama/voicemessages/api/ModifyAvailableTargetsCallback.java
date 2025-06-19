package ru.dimaskama.voicemessages.api;

import java.util.List;

/**
 * <p>
 *     A callback for modifying the list of voice message's targets available to player
 * </p>
 * <p>
 *     Target represents a player or group of players who can receive a voice message.
 *     Target "all" represents all players on the server.
 *     Target "team" represents all players in the sender's team.
 *     Other targets usually represent the nickname of player.
 *     You can add custom targets and handle them with {@code VoiceMessageReceivedCallback}
 * </p>
 * <p>
 *     The event is invoked just before sending the list to the player
 * </p>
 * NOTE: If player has no {@code voicemessages.send} permission,
 * this event won't be invoked
 */
@FunctionalInterface
public interface ModifyAvailableTargetsCallback {

    /**
     * The event for this callback
     */
    Event<ModifyAvailableTargetsCallback> EVENT = new Event<>(
            ModifyAvailableTargetsCallback.class,
            callbacks -> (player, target) -> {
                for (ModifyAvailableTargetsCallback callback : callbacks) {
                    callback.modifyAvailableTargets(player, target);
                }
            }
    );

    /**
     * @param player a server player object of your framework
     * @param targets modifiable list of targets
     */
    void modifyAvailableTargets(Object player, List<String> targets);

}

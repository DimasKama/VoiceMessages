package ru.dimaskama.voicemessages.api;

/**
 * A callback for obtaining {@code VoiceMessagesApi} instance on server initialization
 */
public interface VoiceMessagesApiInitCallback {

    /**
     * The event for this callback
     */
    Event<VoiceMessagesApiInitCallback> EVENT = new Event<>(
            VoiceMessagesApiInitCallback.class,
            callbacks -> api -> {
                for (VoiceMessagesApiInitCallback callback : callbacks) {
                    callback.setVoiceMessagesApi(api);
                }
            }
    );

    /**
     * @param api the Voice Messages API access
     */
    void setVoiceMessagesApi(VoiceMessagesApi api);

}

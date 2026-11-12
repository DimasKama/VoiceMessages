package ru.dimaskama.voicemessages.fabric.client;

import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.voice.client.SoundManager;
import de.maxhenkel.voicechat.voice.client.speaker.Speaker;
import de.maxhenkel.voicechat.voice.client.speaker.SpeakerException;
import de.maxhenkel.voicechat.voice.client.speaker.SpeakerManager;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesPlugin;
import ru.dimaskama.voicemessages.client.backend.VoiceBackend;

/**
 * Plays voice messages with Simple Voice Chat when not connected to a voice chat server,
 * the same way its microphone test does
 */
public class FabricVoiceChatOfflineOutput implements VoiceBackend.AudioOutput {

    private static SoundManager soundManager;
    private Speaker speaker;
    private boolean errorLogged;

    private static synchronized SoundManager getSoundManager() throws SpeakerException {
        if (soundManager == null || soundManager.isClosed()) {
            soundManager = new SoundManager(VoicechatClient.CLIENT_CONFIG.speaker.get());
        }
        return soundManager;
    }

    @Override
    public void play(short[] frame) {
        if (VoicechatClient.CLIENT_CONFIG.disabled.get()) {
            return;
        }
        try {
            if (speaker == null) {
                speaker = SpeakerManager.createSpeaker(getSoundManager(), null);
            }
            float volume = (float) VoicechatClient.VOLUME_CONFIG.getCategoryVolume(VoiceMessagesPlugin.VOLUME_CATEGORY_ID)
                    * VoicechatClient.CLIENT_CONFIG.voiceChatVolume.get().floatValue();
            speaker.play(frame, volume, VoiceMessagesPlugin.VOLUME_CATEGORY_ID);
        } catch (Exception e) {
            if (!errorLogged) {
                errorLogged = true;
                VoiceMessages.getLogger().error("Failed to play voice message with Simple Voice Chat", e);
            }
        }
    }

    @Override
    public void stop() {
        if (speaker != null) {
            speaker.close();
            speaker = null;
        }
    }

}

package ru.dimaskama.voicemessages.client.backend.plasmovoice;

import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.client.backend.VoiceBackend;
import su.plo.voice.api.client.audio.device.source.AlSource;

final class PlasmoVoiceAudioOutput implements VoiceBackend.AudioOutput {

    private final PlasmoVoiceAddon addon;
    private AlSource source;
    private float volume = -1.0F;
    private boolean errorLogged;

    PlasmoVoiceAudioOutput(PlasmoVoiceAddon addon) {
        this.addon = addon;
    }

    @Override
    public void play(short[] frame) {
        try {
            if (addon.isPlaybackDisabled()) {
                return;
            }
            AlSource source = this.source;
            if (source == null || source.isClosed()) {
                source = addon.createPlaybackSource();
                this.source = source;
                volume = -1.0F;
            }
            float newVolume = addon.getPlaybackVolume();
            if (newVolume != volume) {
                volume = newVolume;
                AlSource finalSource = source;
                source.getDevice().runInContextBlocking(() -> finalSource.setVolume(newVolume));
            }
            source.write(frame, false);
        } catch (Exception e) {
            if (!errorLogged) {
                errorLogged = true;
                VoiceMessages.getLogger().error("Failed to play voice message with Plasmo Voice", e);
            }
        }
    }

    @Override
    public void stop() {
        AlSource source = this.source;
        if (source != null) {
            this.source = null;
            source.closeAsync();
        }
    }

}

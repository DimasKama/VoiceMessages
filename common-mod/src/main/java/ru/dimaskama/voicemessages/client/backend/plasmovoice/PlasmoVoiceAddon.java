package ru.dimaskama.voicemessages.client.backend.plasmovoice;

import org.jetbrains.annotations.Nullable;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.audio.capture.AudioCapture;
import su.plo.voice.api.client.audio.device.AlContextOutputDevice;
import su.plo.voice.api.client.audio.device.DeviceException;
import su.plo.voice.api.client.audio.device.InputDevice;
import su.plo.voice.api.client.audio.device.source.AlSource;
import su.plo.voice.api.client.audio.device.source.AlSourceParams;
import su.plo.voice.api.client.config.ClientConfig;
import su.plo.voice.api.client.event.audio.capture.AudioCaptureEvent;
import su.plo.voice.api.client.event.audio.capture.AudioCaptureProcessedEvent;
import su.plo.voice.api.event.EventSubscribe;
import ru.dimaskama.voicemessages.VoiceMessages;

@Addon(
        id = VoiceMessages.ID,
        name = "Voice Messages",
        version = "1.0.0",
        authors = "DimasKama",
        scope = AddonLoaderScope.CLIENT
)
public final class PlasmoVoiceAddon {

    private static final int AL_SOURCE_RELATIVE = 0x202;
    private static final int AL_MAX_GAIN = 0x100E;
    private static final long PLAYBACK_SOURCE_CLOSE_TIMEOUT_MS = 5000L;

    @InjectPlasmoVoice
    private volatile PlasmoVoiceClient voiceClient;
    @Nullable
    private volatile PlasmoVoiceRecorder capturingRecorder;
    @Nullable
    private AlContextOutputDevice outputDevice;

    @Nullable
    public PlasmoVoiceClient getVoiceClient() {
        return voiceClient;
    }

    public PlasmoVoiceClient requireVoiceClient() {
        PlasmoVoiceClient client = voiceClient;
        if (client == null) {
            throw new IllegalStateException("Plasmo Voice is not initialized");
        }
        return client;
    }

    public boolean isConnected() {
        PlasmoVoiceClient client = voiceClient;
        return client != null && client.getServerInfo().isPresent();
    }

    /**
     * @return true if Plasmo Voice is capturing the microphone right now, so we can take frames from it
     */
    public boolean isCaptureRunning() {
        PlasmoVoiceClient client = voiceClient;
        if (client == null || client.getServerInfo().isEmpty() || !client.getUdpClientManager().isConnected()) {
            return false;
        }
        AudioCapture capture = client.getAudioCapture();
        return capture.isActive()
                && capture.getDevice().map(InputDevice::isOpen).orElse(false)
                && client.getActivationManager().getParentActivation().isPresent();
    }

    public synchronized void setCapturingRecorder(PlasmoVoiceRecorder recorder) {
        capturingRecorder = recorder;
    }

    public synchronized void clearCapturingRecorder(PlasmoVoiceRecorder recorder) {
        if (capturingRecorder == recorder) {
            capturingRecorder = null;
        }
    }

    @EventSubscribe
    public void onAudioCapture(AudioCaptureEvent event) {
        if (capturingRecorder != null) {
            // Do not transmit the voice to the voice chat while recording a voice message
            event.setFlushActivations(true);
        }
    }

    @EventSubscribe
    public void onAudioCaptureProcessed(AudioCaptureProcessedEvent event) {
        PlasmoVoiceRecorder recorder = capturingRecorder;
        if (recorder != null) {
            // Mono samples with Plasmo Voice filters (microphone volume, noise suppression) applied
            recorder.onCapturedFrame(event.getProcessed().getMono());
        }
    }

    public InputDevice openInputDevice() throws DeviceException {
        return requireVoiceClient().getDeviceManager().openInputDevice(PlasmoVoiceBackend.AUDIO_FORMAT);
    }

    /**
     * Creates a non-positional source on our own 48kHz output device.
     * Plasmo Voice output device exists only while connected to a Plasmo Voice server
     * and uses the server sample rate, so it is not used here
     */
    public synchronized AlSource createPlaybackSource() throws DeviceException {
        PlasmoVoiceClient client = requireVoiceClient();
        AlContextOutputDevice device = outputDevice;
        if (device == null || !device.isOpen()) {
            if (device != null) {
                outputDevice = null;
                device.close();
            }
            device = client.getDeviceManager().openOutputDevice(PlasmoVoiceBackend.AUDIO_FORMAT);
            outputDevice = device;
        }
        AlSource source = device.createSource(false, AlSourceParams.DEFAULT);
        source.setCloseTimeoutMs(PLAYBACK_SOURCE_CLOSE_TIMEOUT_MS);
        device.runInContextBlocking(() -> {
            source.setFloat(AL_MAX_GAIN, 4.0F);
            source.setInt(AL_SOURCE_RELATIVE, 1);
            source.play();
        });
        return source;
    }

    public boolean isPlaybackDisabled() {
        PlasmoVoiceClient client = voiceClient;
        return client == null || client.getConfig().getVoice().getDisabled().value();
    }

    public float getPlaybackVolume() {
        ClientConfig config = requireVoiceClient().getConfig();
        float volume = config.getVoice().getVolume().value().floatValue();
        if (config.getAdvanced().getExponentialVolumeSlider().value()) {
            volume = (float) Math.pow(volume, 3);
        }
        return volume;
    }

}

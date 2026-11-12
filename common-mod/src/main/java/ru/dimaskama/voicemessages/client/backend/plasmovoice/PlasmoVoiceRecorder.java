package ru.dimaskama.voicemessages.client.backend.plasmovoice;

import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesModService;
import su.plo.voice.api.client.audio.device.InputDevice;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * If Plasmo Voice is capturing the microphone (connected to a Plasmo Voice server), takes frames from its capture
 * and mutes voice chat transmission while recording. Otherwise, opens the microphone configured in Plasmo Voice by itself
 */
public final class PlasmoVoiceRecorder implements VoiceMessagesModService.VoiceRecordThread {

    private static final AtomicInteger THREAD_COUNT = new AtomicInteger();
    private final PlasmoVoiceAddon addon;
    private final Predicate<short[]> frameConsumer;
    private final Consumer<IOException> onMicError;
    private volatile boolean running;
    private Thread ownDeviceThread;

    public PlasmoVoiceRecorder(PlasmoVoiceAddon addon, Predicate<short[]> frameConsumer, Consumer<IOException> onMicError) {
        this.addon = addon;
        this.frameConsumer = frameConsumer;
        this.onMicError = onMicError;
    }

    @Override
    public void startVoiceRecord() {
        running = true;
        if (addon.isCaptureRunning()) {
            addon.setCapturingRecorder(this);
        } else {
            ownDeviceThread = new Thread(this::runOwnDevice, "VoiceRecordThread#" + THREAD_COUNT.getAndIncrement());
            ownDeviceThread.setDaemon(true);
            ownDeviceThread.start();
        }
    }

    @Override
    public void stopVoiceRecord() {
        running = false;
        addon.clearCapturingRecorder(this);
        Thread thread = ownDeviceThread;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(1000L);
            } catch (Exception e) {
                VoiceMessages.getLogger().error("Failed to join voice record thread", e);
            }
        }
    }

    // Called from the Plasmo Voice capture thread
    void onCapturedFrame(short[] samples) {
        if (running && !frameConsumer.test(PlasmoVoiceBackend.toFrame(samples))) {
            running = false;
            addon.clearCapturingRecorder(this);
        }
    }

    private void runOwnDevice() {
        InputDevice device = null;
        try {
            device = addon.openInputDevice();
            device.start();
            while (running) {
                short[] samples = device.read();
                if (samples == null) {
                    Thread.sleep(5L);
                    continue;
                }
                running = frameConsumer.test(PlasmoVoiceBackend.toFrame(device.processFilters(samples)));
            }
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            running = false;
            onMicError.accept(e instanceof IOException ioException ? ioException : new IOException(e.getMessage(), e));
        } finally {
            if (device != null) {
                try {
                    device.close();
                } catch (Exception e) {
                    VoiceMessages.getLogger().warn("Failed to close Plasmo Voice input device", e);
                }
            }
        }
    }

}

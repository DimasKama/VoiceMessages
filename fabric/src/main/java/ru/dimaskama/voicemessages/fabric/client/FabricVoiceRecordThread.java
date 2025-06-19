package ru.dimaskama.voicemessages.fabric.client;

import de.maxhenkel.voicechat.voice.client.*;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesModService;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FabricVoiceRecordThread extends Thread implements VoiceMessagesModService.VoiceRecordThread {

    private static final AtomicInteger THREAD_COUNT = new AtomicInteger();
    private final Predicate<short[]> frameConsumer;
    private final MicThread micThread;
    private final boolean usesOwnMicThread;
    private volatile boolean running;

    public FabricVoiceRecordThread(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError) {
        this.frameConsumer = frameConsumer;
        setDaemon(true);
        setName("VoiceRecordThread#" + THREAD_COUNT.getAndIncrement());
        ClientVoicechat client = ClientManager.getClient();
        MicThread micThread = client != null ? client.getMicThread() : null;
        if (micThread == null) {
            micThread = new MicThread(client, null, onMicError::accept);
            usesOwnMicThread = true;
        } else {
            usesOwnMicThread = false;
        }
        micThread.setMicrophoneLocked(true);
        this.micThread = micThread;
        running = true;
    }

    @Override
    public void run() {
        while (running) {
            short[] buff = this.micThread.pollMic();
            if (buff != null) {
                running = frameConsumer.test(buff);
            }
        }
        micThread.setMicrophoneLocked(false);
        if (usesOwnMicThread) {
            micThread.close();
        }
    }

    @Override
    public void startVoiceRecord() {
        start();
    }

    @Override
    public void stopVoiceRecord() {
        try {
            running = false;
            join(1000L);
        } catch (Exception e) {
            VoiceMessages.getLogger().error("Failed to join voice record thread", e);
        }
    }

}

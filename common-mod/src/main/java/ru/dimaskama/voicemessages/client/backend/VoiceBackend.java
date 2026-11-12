package ru.dimaskama.voicemessages.client.backend;

import ru.dimaskama.voicemessages.VoiceMessagesModService;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Voice mod used for microphone capture, audio playback and Opus coding.
 * All audio is 48kHz mono, 960 samples (20ms) per frame.
 * Implementations must only be loaded when their voice mod is present.
 */
public interface VoiceBackend {

    String getName();

    /**
     * @return true if the client is connected to the voice server of this voice mod right now
     */
    boolean isConnected();

    /**
     * @return true if the current server announces this voice mod. Used while the voice connection is not established yet
     */
    boolean isAvailableOnServer();

    VoiceMessagesModService.VoiceRecordThread createRecorder(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError);

    AudioOutput createOutput();

    Encoder createEncoder();

    Decoder createDecoder();

    interface AudioOutput {

        /**
         * Called every 20ms from the playback thread
         */
        void play(short[] frame);

        /**
         * Called from the playback thread when the playback is paused or finished. Releases audio resources
         */
        default void stop() {}

    }

    interface Encoder extends AutoCloseable {

        byte[] encode(short[] frame);

        @Override
        void close();

    }

    interface Decoder extends AutoCloseable {

        short[] decode(byte[] encoded);

        @Override
        void close();

    }

}

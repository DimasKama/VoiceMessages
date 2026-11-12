package ru.dimaskama.voicemessages.client.backend.plasmovoice;

import net.minecraft.resources.Identifier;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesModService;
import ru.dimaskama.voicemessages.client.backend.VoiceBackend;
import ru.dimaskama.voicemessages.client.backend.VoiceBackends;
import su.plo.voice.api.audio.codec.AudioDecoder;
import su.plo.voice.api.audio.codec.AudioEncoder;
import su.plo.voice.api.audio.codec.CodecException;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.proto.data.audio.codec.CodecInfo;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class PlasmoVoiceBackend implements VoiceBackend {

    static final AudioFormat AUDIO_FORMAT = new AudioFormat(VoiceMessages.SAMPLE_RATE, 16, 1, true, false);
    private static final Identifier PLASMO_VOICE_CHANNEL = Identifier.fromNamespaceAndPath("plasmo", "voice/v2");
    // Same settings as Simple Voice Chat uses, so voice messages are compatible between voice mods
    private static final CodecInfo OPUS_ENCODER_INFO = new CodecInfo("opus", Map.of(
            "mode", "VOIP",
            "bitrate", "-1000" // OPUS_AUTO
    ));
    private static final CodecInfo OPUS_DECODER_INFO = new CodecInfo("opus", Map.of());
    private final PlasmoVoiceAddon addon;

    private PlasmoVoiceBackend(PlasmoVoiceAddon addon) {
        this.addon = addon;
    }

    public static VoiceBackend create() {
        PlasmoVoiceAddon addon = new PlasmoVoiceAddon();
        PlasmoVoiceClient.getAddonsLoader().load(addon);
        return new PlasmoVoiceBackend(addon);
    }

    @Override
    public String getName() {
        return "Plasmo Voice";
    }

    @Override
    public boolean isConnected() {
        return addon.isConnected();
    }

    @Override
    public boolean isAvailableOnServer() {
        return VoiceBackends.canSendToServer(PLASMO_VOICE_CHANNEL);
    }

    @Override
    public VoiceMessagesModService.VoiceRecordThread createRecorder(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError) {
        return new PlasmoVoiceRecorder(addon, frameConsumer, onMicError);
    }

    @Override
    public AudioOutput createOutput() {
        return new PlasmoVoiceAudioOutput(addon);
    }

    @Override
    public Encoder createEncoder() {
        AudioEncoder encoder = addon.requireVoiceClient().getCodecManager().createEncoder(
                OPUS_ENCODER_INFO,
                VoiceMessages.SAMPLE_RATE,
                false,
                VoiceMessages.MAX_ENCODED_FRAME_SIZE
        );
        return new Encoder() {
            @Override
            public byte[] encode(short[] frame) {
                try {
                    return encoder.encode(frame);
                } catch (CodecException e) {
                    throw new IllegalStateException("Failed to encode audio", e);
                }
            }

            @Override
            public void close() {
                encoder.close();
            }
        };
    }

    @Override
    public Decoder createDecoder() {
        AudioDecoder decoder = addon.requireVoiceClient().getCodecManager().createDecoder(
                OPUS_DECODER_INFO,
                VoiceMessages.SAMPLE_RATE,
                false,
                VoiceMessages.FRAME_SIZE
        );
        return new Decoder() {
            @Override
            public short[] decode(byte[] encoded) {
                try {
                    return decoder.decode(encoded);
                } catch (CodecException e) {
                    throw new IllegalStateException("Failed to decode audio", e);
                }
            }

            @Override
            public void close() {
                decoder.close();
            }
        };
    }

    /**
     * Plasmo Voice captures 20ms frames at the sample rate configured on the server.
     * Voice messages always use 48kHz, so frames with another sample rate are linearly resampled
     */
    static short[] toFrame(short[] samples) {
        int outLength = VoiceMessages.FRAME_SIZE;
        int inLength = samples.length;
        if (inLength == outLength || inLength == 0) {
            return samples;
        }
        short[] out = new short[outLength];
        double step = (double) inLength / outLength;
        for (int i = 0; i < outLength; i++) {
            double pos = i * step;
            int index = (int) pos;
            int next = Math.min(index + 1, inLength - 1);
            double fraction = pos - index;
            out[i] = (short) Math.round(samples[index] * (1.0 - fraction) + samples[next] * fraction);
        }
        return out;
    }

}

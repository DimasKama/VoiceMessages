package ru.dimaskama.voicemessages.client.backend.svc;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.audiochannel.ClientAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.resources.ResourceLocation;
import ru.dimaskama.voicemessages.VoiceMessagesModService;
import ru.dimaskama.voicemessages.VoiceMessagesPlugin;
import ru.dimaskama.voicemessages.client.backend.VoiceBackend;
import ru.dimaskama.voicemessages.client.backend.VoiceBackends;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class SimpleVoiceChatBackend implements VoiceBackend {

    private static final ResourceLocation REQUEST_SECRET_CHANNEL = ResourceLocation.fromNamespaceAndPath("voicechat", "request_secret");
    private final VoiceMessagesModService service;

    public SimpleVoiceChatBackend(VoiceMessagesModService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return "Simple Voice Chat";
    }

    @Override
    public boolean isConnected() {
        return VoiceMessagesPlugin.isClientConnected();
    }

    @Override
    public boolean isAvailableOnServer() {
        return VoiceBackends.canSendToServer(REQUEST_SECRET_CHANNEL);
    }

    @Override
    public VoiceMessagesModService.VoiceRecordThread createRecorder(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError) {
        // Uses Simple Voice Chat internals, which are only available in the loader-specific modules
        return service.createVoiceRecordThread(frameConsumer, onMicError);
    }

    @Override
    public AudioOutput createOutput() {
        return new Output(service);
    }

    @Override
    public Encoder createEncoder() {
        OpusEncoder encoder = getApi().createEncoder();
        return new Encoder() {
            @Override
            public byte[] encode(short[] frame) {
                return encoder.encode(frame);
            }

            @Override
            public void close() {
                encoder.close();
            }
        };
    }

    @Override
    public Decoder createDecoder() {
        OpusDecoder decoder = getApi().createDecoder();
        return new Decoder() {
            @Override
            public short[] decode(byte[] encoded) {
                return decoder.decode(encoded);
            }

            @Override
            public void close() {
                decoder.close();
            }
        };
    }

    private static VoicechatApi getApi() {
        VoicechatApi api = VoiceMessagesPlugin.getApi();
        if (api == null) {
            throw new IllegalStateException("Simple Voice Chat API is not initialized");
        }
        return api;
    }

    private static final class Output implements AudioOutput {

        private final VoiceMessagesModService service;
        private ClientAudioChannel channel;
        private AudioOutput offlineOutput;

        private Output(VoiceMessagesModService service) {
            this.service = service;
        }

        @Override
        public void play(short[] frame) {
            // Simple Voice Chat API plays audio only while connected to a voice chat server
            VoicechatClientApi clientApi = VoiceMessagesPlugin.getClientApi();
            if (VoiceMessagesPlugin.isClientConnected() && clientApi != null) {
                stopOffline();
                if (channel == null) {
                    channel = clientApi.createStaticAudioChannel(UUID.randomUUID());
                    channel.setCategory(VoiceMessagesPlugin.VOLUME_CATEGORY_ID);
                }
                channel.play(frame);
            } else {
                if (offlineOutput == null) {
                    offlineOutput = service.createVoiceChatOfflineOutput();
                }
                offlineOutput.play(frame);
            }
        }

        @Override
        public void stop() {
            stopOffline();
        }

        private void stopOffline() {
            if (offlineOutput != null) {
                offlineOutput.stop();
                offlineOutput = null;
            }
        }

    }

}

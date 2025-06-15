package ru.dimaskama.voicemessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dimaskama.voicemessages.config.JsonConfig;
import ru.dimaskama.voicemessages.config.ServerConfig;

public final class VoiceMessages {

    public static final String NAME = "VoiceMessages";
    public static final String ID = "voicemessages";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    public static final JsonConfig<ServerConfig> SERVER_CONFIG = new JsonConfig<>(
            "./config/voicemessages_server.json",
            ServerConfig.CODEC,
            ServerConfig::new
    );
    public static final String VOICE_MESSAGE_SEND_PERMISSION = "voicemessages.send";
    public static final int SAMPLE_RATE = 48000;
    public static final int FRAME_SIZE = 960;
    public static final int FRAMES_PER_SEC = SAMPLE_RATE / FRAME_SIZE;
    public static final int MAX_VOICE_MESSAGE_DURATION_MS = 300_000;
    public static final int MAX_VOICE_MESSAGE_FRAMES = MAX_VOICE_MESSAGE_DURATION_MS * FRAMES_PER_SEC / 1000;

    public static void init() {

    }

}

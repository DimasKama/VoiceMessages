package ru.dimaskama.voicemessages;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dimaskama.voicemessages.config.JsonConfig;
import ru.dimaskama.voicemessages.config.ServerConfig;

public final class VoiceMessages {

    public static final String MOD_NAME = "VoiceMessages";
    public static final String MOD_ID = "voicemessages";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
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
    private static VoiceMessagesService service;
    private static boolean active;

    public static void init(VoiceMessagesService service) {
        VoiceMessages.service = service;
        active = service.isModLoaded("voicechat");
    }

    // To remove voicechat crash-dependency, we check this method in every mod feature. If voicechat is not loaded, it returns false
    public static boolean isActive() {
        return active;
    }

    public static VoiceMessagesService getService() {
        return service;
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

}

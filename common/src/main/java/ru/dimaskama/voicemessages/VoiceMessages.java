package ru.dimaskama.voicemessages;

import ru.dimaskama.voicemessages.config.JsonConfig;
import ru.dimaskama.voicemessages.config.ServerConfig;
import ru.dimaskama.voicemessages.logger.AbstractLogger;
import ru.dimaskama.voicemessages.logger.StdoutLogger;

public final class VoiceMessages {

    public static final String NAME = "VoiceMessages";
    public static final String ID = "voicemessages";
    public static final JsonConfig<ServerConfig> SERVER_CONFIG = new JsonConfig<>(
            "./config/voicemessages_server.json",
            ServerConfig.CODEC,
            ServerConfig::new
    );
    public static final String VOICE_MESSAGE_SEND_PERMISSION = "voicemessages.send";
    public static final String VOICE_MESSAGE_SEND_ALL_PERMISSION = "voicemessages.send.all";
    public static final String VOICE_MESSAGE_SEND_TEAM_PERMISSION = "voicemessages.send.team";
    public static final String VOICE_MESSAGE_SEND_PLAYERS_PERMISSION = "voicemessages.send.players";
    public static final String TARGET_ALL = "all";
    public static final String TARGET_TEAM = "team";
    public static final int SAMPLE_RATE = 48000;
    public static final int FRAME_SIZE = 960;
    public static final int FRAMES_PER_SEC = SAMPLE_RATE / FRAME_SIZE;
    public static final int MAX_VOICE_MESSAGE_DURATION_MS = 300_000;
    public static final int MAX_VOICE_MESSAGE_FRAMES = MAX_VOICE_MESSAGE_DURATION_MS * FRAMES_PER_SEC / 1000;
    public static final int MAX_ENCODED_FRAME_SIZE = 256;
    private static String version;
    private static AbstractLogger logger = new StdoutLogger();

    public static void init(String version, AbstractLogger logger) {
        VoiceMessages.version = version;
        VoiceMessages.logger = logger;
    }

    public static AbstractLogger getLogger() {
        return logger;
    }

    public static String getVersion() {
        return version;
    }

    public static boolean isClientVersionCompatible(String modVersion) {
        return getVersionFromModVersion(modVersion).compareTo("1.0.0") >= 0;
    }

    public static String getVersionFromModVersion(String modVersion) {
        String[] split = modVersion.split("-");
        if (split.length < 2) {
            throw new IllegalArgumentException("Invalid VoiceMessages mod version: " + modVersion + ". Must be ${version}-${mcVersion}");
        }
        return split[0];
    }

}

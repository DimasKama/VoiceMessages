package ru.dimaskama.voicemessages;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dimaskama.voicemessages.logger.AbstractLogger;

public final class VoiceMessagesMod {

    public static final String SIMPLE_VOICE_CHAT_ID = "voicechat";
    public static final String PLASMO_VOICE_ID = "plasmovoice";
    private static String modVersion;
    private static VoiceMessagesModService service;
    private static boolean clientActive;

    public static void init(String modVersion, VoiceMessagesModService service) {
        VoiceMessagesMod.service = service;
        clientActive = service.isModLoaded(SIMPLE_VOICE_CHAT_ID) || service.isModLoaded(PLASMO_VOICE_ID);
        Logger logger = LoggerFactory.getLogger(VoiceMessages.NAME);
        VoiceMessagesMod.modVersion = modVersion;
        VoiceMessages.init(VoiceMessages.getVersionFromModVersion(modVersion), new AbstractLogger() {
            @Override
            public void info(String message) {
                logger.info(message);
            }

            @Override
            public void info(String message, Exception e) {
                logger.info(message, e);
            }

            @Override
            public void warn(String message) {
                logger.warn(message);
            }

            @Override
            public void warn(String message, Exception e) {
                logger.warn(message, e);
            }

            @Override
            public void error(String message) {
                logger.error(message);
            }

            @Override
            public void error(String message, Exception e) {
                logger.error(message, e);
            }
        });
    }

    // The server side works without any voice mod, it only relays encoded audio.
    // Client features need Simple Voice Chat or Plasmo Voice for the microphone, playback and Opus,
    // so every client feature checks this method. If neither of them is loaded, it returns false
    public static boolean isClientActive() {
        return clientActive;
    }

    public static String getModVersion() {
        return modVersion;
    }

    public static VoiceMessagesModService getService() {
        return service;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(VoiceMessages.ID, path);
    }

}

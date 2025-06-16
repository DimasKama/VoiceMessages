package ru.dimaskama.voicemessages;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dimaskama.voicemessages.logger.AbstractLogger;

public final class VoiceMessagesMod {

    private static String modVersion;
    private static VoiceMessagesModService service;
    private static boolean active;

    public static void init(String modVersion, VoiceMessagesModService service) {
        VoiceMessagesMod.service = service;
        active = service.isModLoaded("voicechat");
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

    // To remove voicechat crash-dependency, we check this method in every mod feature. If voicechat is not loaded, it returns false
    public static boolean isActive() {
        return active;
    }

    public static String getModVersion() {
        return modVersion;
    }

    public static VoiceMessagesModService getService() {
        return service;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(VoiceMessages.ID, path);
    }

}

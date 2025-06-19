package ru.dimaskama.voicemessages.spigot;

import org.bukkit.plugin.java.JavaPlugin;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.api.VoiceMessagesApiInitCallback;
import ru.dimaskama.voicemessages.logger.AbstractLogger;
import ru.dimaskama.voicemessages.spigot.impl.VoiceMessagesApiImpl;
import ru.dimaskama.voicemessages.spigot.networking.VoiceMessagesSpigotNetworking;

import java.util.logging.Logger;

public final class VoiceMessagesSpigot extends JavaPlugin {

    private static VoiceMessagesSpigot instance;

    public VoiceMessagesSpigot() {
        super();
    }

    @Override
    public void onEnable() {
        instance = this;
        Logger logger = getLogger();
        VoiceMessages.init(getDescription().getVersion(), new AbstractLogger() {
            @Override
            public void info(String message) {
                logger.info(message);
            }

            @Override
            public void info(String message, Exception e) {
                logger.info(message);
                logger.info(e.getLocalizedMessage());
            }

            @Override
            public void warn(String message) {
                logger.warning(message);
            }

            @Override
            public void warn(String message, Exception e) {
                logger.warning(message);
                logger.warning(e.getLocalizedMessage());
            }

            @Override
            public void error(String message) {
                logger.severe(message);
            }

            @Override
            public void error(String message, Exception e) {
                logger.severe(message);
                logger.severe(e.getLocalizedMessage());
            }
        });
        getServer().getPluginManager().registerEvents(new VoiceMessagesSpigotListener(this), this);
        getServer().getScheduler().runTaskTimer(this, VoiceMessagesSpigotNetworking::tickBuildingVoiceMessages, 5L, 5L);

        VoiceMessages.SERVER_CONFIG.loadOrCreate();

        VoiceMessagesSpigotNetworking.init(this);

        VoiceMessagesApiInitCallback.EVENT.invoker().setVoiceMessagesApi(new VoiceMessagesApiImpl());
    }

    public static VoiceMessagesSpigot getInstance() {
        return instance;
    }

    public static String id(String path) {
        return VoiceMessages.ID + ':' + path;
    }

}

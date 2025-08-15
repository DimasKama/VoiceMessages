package ru.dimaskama.voicemessages.paper;

import org.bukkit.plugin.java.JavaPlugin;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.api.VoiceMessagesApiInitCallback;
import ru.dimaskama.voicemessages.logger.AbstractLogger;
import ru.dimaskama.voicemessages.paper.impl.VoiceMessagesApiImpl;
import ru.dimaskama.voicemessages.paper.networking.VoiceMessagesPaperNetworking;

import java.util.logging.Logger;

public final class VoiceMessagesPaper extends JavaPlugin {

    private static VoiceMessagesPaper instance;

    public VoiceMessagesPaper() {
        super();
    }

    @Override
    public void onEnable() {
        instance = this;
        Logger logger = getLogger();
        VoiceMessages.init(getPluginMeta().getVersion(), new AbstractLogger() {
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
        getServer().getPluginManager().registerEvents(new VoiceMessagesPaperListener(this), this);
        if (isFolia()) {
            getServer()
                    .getGlobalRegionScheduler()
                    .runAtFixedRate(this, t -> VoiceMessagesPaperNetworking.tickBuildingVoiceMessages(), 5L, 5L);
        } else {
            getServer()
                    .getScheduler()
                    .runTaskTimer(this, VoiceMessagesPaperNetworking::tickBuildingVoiceMessages, 5L, 5L);
        }

        VoiceMessages.SERVER_CONFIG.loadOrCreate();

        VoiceMessagesPaperNetworking.init(this);

        VoiceMessagesApiInitCallback.EVENT.invoker().setVoiceMessagesApi(new VoiceMessagesApiImpl());
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static VoiceMessagesPaper getInstance() {
        return instance;
    }

    public static String id(String path) {
        return VoiceMessages.ID + ':' + path;
    }

}

package ru.dimaskama.voicemessages.client.backend;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.VoiceMessagesModService;
import ru.dimaskama.voicemessages.client.backend.plasmovoice.PlasmoVoiceBackend;
import ru.dimaskama.voicemessages.client.backend.svc.SimpleVoiceChatBackend;

/**
 * Selects the voice mod used for voice messages:
 * <ol>
 *     <li>If only one of Simple Voice Chat and Plasmo Voice is installed, it is used</li>
 *     <li>If both are installed, the one the client is connected to on the current server is used</li>
 *     <li>If none of them is connected yet, the one the server announces is used</li>
 *     <li>Otherwise Simple Voice Chat is used</li>
 * </ol>
 */
public final class VoiceBackends {

    @Nullable
    private static VoiceBackend simpleVoiceChat;
    @Nullable
    private static VoiceBackend plasmoVoice;
    private static boolean initialized;
    @Nullable
    private static volatile VoiceBackend lastSelected;

    private VoiceBackends() {}

    private static synchronized void init() {
        if (!initialized) {
            VoiceMessagesModService service = VoiceMessagesMod.getService();
            if (service.isModLoaded(VoiceMessagesMod.SIMPLE_VOICE_CHAT_ID)) {
                simpleVoiceChat = new SimpleVoiceChatBackend(service);
            }
            if (service.isModLoaded(VoiceMessagesMod.PLASMO_VOICE_ID)) {
                plasmoVoice = PlasmoVoiceBackend.create();
            }
            initialized = true;
        }
    }

    /**
     * Loads backends early (e.g. registers the Plasmo Voice addon), so they track voice connection state from the start
     */
    public static void initClient() {
        init();
    }

    public static VoiceBackend get() {
        init();
        VoiceBackend selected = select();
        if (selected == null) {
            throw new IllegalStateException("Neither Simple Voice Chat nor Plasmo Voice is installed");
        }
        if (lastSelected != selected) {
            lastSelected = selected;
            VoiceMessages.getLogger().info("Using " + selected.getName() + " for voice messages");
        }
        return selected;
    }

    @Nullable
    private static VoiceBackend select() {
        VoiceBackend svc = simpleVoiceChat;
        VoiceBackend pv = plasmoVoice;
        if (svc == null || pv == null) {
            return svc != null ? svc : pv;
        }
        if (svc.isConnected()) {
            return svc;
        }
        if (pv.isConnected()) {
            return pv;
        }
        if (svc.isAvailableOnServer()) {
            return svc;
        }
        if (pv.isAvailableOnServer()) {
            return pv;
        }
        return svc;
    }

    public static boolean canSendToServer(ResourceLocation channel) {
        if (Minecraft.getInstance().getConnection() == null) {
            return false;
        }
        try {
            return VoiceMessagesMod.getService().canSendToServer(channel);
        } catch (Exception e) {
            return false;
        }
    }

}

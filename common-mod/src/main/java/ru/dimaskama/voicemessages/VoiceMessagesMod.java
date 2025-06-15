package ru.dimaskama.voicemessages;

import net.minecraft.resources.ResourceLocation;

public final class VoiceMessagesMod {

    private static VoiceMessagesModService service;
    private static boolean active;

    public static void init(VoiceMessagesModService service) {
        VoiceMessagesMod.service = service;
        active = service.isModLoaded("voicechat");
        VoiceMessages.init();
    }

    // To remove voicechat crash-dependency, we check this method in every mod feature. If voicechat is not loaded, it returns false
    public static boolean isActive() {
        return active;
    }

    public static VoiceMessagesModService getService() {
        return service;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(VoiceMessages.ID, path);
    }

}

package ru.dimaskama.voicemessages;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

@ForgeVoicechatPlugin
public class VoiceMessagesPlugin implements VoicechatPlugin {

    private static VoicechatClientApi clientApi;
    private static OpusPair clientOpus;
    private static VolumeCategory volumeCategory;

    @Override
    public String getPluginId() {
        return VoiceMessages.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        clientOpus = new OpusPair(api.createEncoder(), api.createDecoder());
        volumeCategory = api.volumeCategoryBuilder()
                .setId("voice_messages")
                .setName("Voice Messages")
                .setDescription("Chat voice messages volume amplifier")
                .build();
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientVoicechatInitializationEvent.class, this::onClientVoicechatInitialization);
    }

    private void onClientVoicechatInitialization(ClientVoicechatInitializationEvent event) {
        clientApi = event.getVoicechat();
        clientApi.registerClientVolumeCategory(volumeCategory);
    }

    public static VoicechatClientApi getClientApi() {
        return clientApi;
    }

    public static OpusPair getClientOpus() {
        return clientOpus;
    }

    public static VolumeCategory getVolumeCategory() {
        return volumeCategory;
    }

}

package ru.dimaskama.voicemessages;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

// Simple Voice Chat plugin. Loaded only by Simple Voice Chat
@ForgeVoicechatPlugin
public class VoiceMessagesPlugin implements VoicechatPlugin {

    public static final String VOLUME_CATEGORY_ID = "voice_messages";
    private static VoicechatApi api;
    private static VoicechatClientApi clientApi;
    private static VolumeCategory volumeCategory;
    private static volatile boolean clientConnected;

    @Override
    public String getPluginId() {
        return VoiceMessages.ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        VoiceMessagesPlugin.api = api;
        volumeCategory = api.volumeCategoryBuilder()
                .setId(VOLUME_CATEGORY_ID)
                .setName("Voice Messages")
                .setDescription("Chat voice messages volume amplifier")
                .build();
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientVoicechatInitializationEvent.class, this::onClientVoicechatInitialization);
        registration.registerEvent(ClientVoicechatConnectionEvent.class, this::onClientVoicechatConnection);
    }

    private void onClientVoicechatInitialization(ClientVoicechatInitializationEvent event) {
        clientApi = event.getVoicechat();
        clientApi.registerClientVolumeCategory(volumeCategory);
    }

    private void onClientVoicechatConnection(ClientVoicechatConnectionEvent event) {
        clientConnected = event.isConnected();
    }

    public static VoicechatApi getApi() {
        return api;
    }

    public static VoicechatClientApi getClientApi() {
        return clientApi;
    }

    public static VolumeCategory getVolumeCategory() {
        return volumeCategory;
    }

    public static boolean isClientConnected() {
        return clientConnected;
    }

}

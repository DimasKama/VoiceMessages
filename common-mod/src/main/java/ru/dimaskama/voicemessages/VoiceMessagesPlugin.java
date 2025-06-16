package ru.dimaskama.voicemessages;

import com.google.common.collect.ImmutableList;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import java.util.List;

@ForgeVoicechatPlugin
public class VoiceMessagesPlugin implements VoicechatPlugin {

    private static VoicechatClientApi clientApi;
    private static OpusEncoder clientOpusEncoder;
    private static VolumeCategory volumeCategory;

    @Override
    public String getPluginId() {
        return VoiceMessages.ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        clientOpusEncoder = api.createEncoder();
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

    public static OpusEncoder getClientOpusEncoder() {
        return clientOpusEncoder;
    }

    public static VolumeCategory getVolumeCategory() {
        return volumeCategory;
    }

    public static List<byte[]> encodeList(OpusEncoder encoder, List<short[]> audio) {
        int size = audio.size();
        ImmutableList.Builder<byte[]> builder = ImmutableList.builderWithExpectedSize(size);
        for (short[] frame : audio) {
            builder.add(encoder.encode(frame));
        }
        return builder.build();
    }

    public static List<short[]> decodeList(OpusDecoder decoder, List<byte[]> encoded) {
        int size = encoded.size();
        ImmutableList.Builder<short[]> builder = ImmutableList.builderWithExpectedSize(size);
        for (byte[] frame : encoded) {
            builder.add(decoder.decode(frame));
        }
        return builder.build();
    }

}

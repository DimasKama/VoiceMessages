package ru.dimaskama.voicemessages;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface VoiceMessagesModService {

    boolean isModLoaded(String modId);

    void sendToServer(CustomPacketPayload payload);

    boolean canSendToServer(ResourceLocation payloadId);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    VoiceRecordThread createVoiceRecordThread(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError);

    boolean hasVoiceMessageSendPermission(ServerPlayer player);

    interface VoiceRecordThread {

        void startVoiceRecord();

        void stopVoiceRecord();

    }

}

package ru.dimaskama.voicemessages;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface VoiceMessagesService {

    boolean isModLoaded(String modId);

    void sendToServer(CustomPayload payload);

    boolean canSendToServer(Identifier payloadId);

    void sendToPlayer(ServerPlayerEntity player, CustomPayload payload);

    boolean canSendToPlayer(ServerPlayerEntity player, Identifier payloadId);

    boolean canSendConfigurationToPlayer(ServerConfigurationNetworkHandler handler, Identifier payloadId);

    VoiceRecordThread createVoiceRecordThread(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError);

    boolean hasVoiceMessageSendPermission(ServerPlayerEntity player);

    interface VoiceRecordThread {

        void startVoiceRecord();

        void stopVoiceRecord();

    }

}

package ru.dimaskama.voicemessages;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import ru.dimaskama.voicemessages.client.backend.VoiceBackend;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface VoiceMessagesModService {

    boolean isModLoaded(String modId);

    void sendToServer(CustomPacketPayload payload);

    boolean canSendToServer(Identifier payloadId);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    // Simple Voice Chat microphone capture. Uses its internals, so implemented in the loader-specific modules
    VoiceRecordThread createVoiceRecordThread(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError);

    // Simple Voice Chat playback when not connected to a voice chat server (its API can't play audio then).
    // Uses its internals, so implemented in the loader-specific modules
    VoiceBackend.AudioOutput createVoiceChatOfflineOutput();

    boolean hasVoiceMessageSendPermission(ServerPlayer player);

    boolean hasVoiceMessageSendAllPermission(ServerPlayer player);

    boolean hasVoiceMessageSendTeamPermission(ServerPlayer player);

    boolean hasVoiceMessageSendPlayersPermission(ServerPlayer player);

    interface VoiceRecordThread {

        void startVoiceRecord();

        void stopVoiceRecord();

    }

}

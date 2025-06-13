package ru.dimaskama.voicemessages.neoforge;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesService;
import ru.dimaskama.voicemessages.neoforge.client.NeoForgeVoiceRecordThread;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mod(VoiceMessages.MOD_ID)
public final class VoiceMessagesNeoForge {

    public static final PermissionNode<Boolean> VOICE_MESSAGE_SEND_PERMISSION = new PermissionNode<>(
            VoiceMessages.MOD_ID,
            "send",
            PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> true
    );

    public VoiceMessagesNeoForge() {
        VoiceMessages.init(new VoiceMessagesService() {
            @Override
            public boolean isModLoaded(String modId) {
                return ModList.get().isLoaded(modId);
            }

            @Override
            public void sendToServer(CustomPayload payload) {
                PacketDistributor.sendToServer(payload);
            }

            @Override
            public boolean canSendToServer(Identifier payloadId) {
                return net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler().hasChannel(payloadId);
            }

            @Override
            public void sendToPlayer(ServerPlayerEntity player, CustomPayload payload) {
                PacketDistributor.sendToPlayer(player, payload);
            }

            @Override
            public boolean canSendToPlayer(ServerPlayerEntity player, Identifier payloadId) {
                return player.networkHandler.hasChannel(payloadId);
            }

            @Override
            public boolean canSendConfigurationToPlayer(ServerConfigurationNetworkHandler handler, Identifier payloadId) {
                return handler.hasChannel(payloadId);
            }

            @Override
            public VoiceRecordThread createVoiceRecordThread(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError) {
                return new NeoForgeVoiceRecordThread(frameConsumer, onMicError);
            }

            @Override
            public boolean hasVoiceMessageSendPermission(ServerPlayerEntity player) {
                return PermissionAPI.getPermission(player, VOICE_MESSAGE_SEND_PERMISSION);
            }
        });
    }

}

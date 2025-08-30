package ru.dimaskama.voicemessages.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.VoiceMessagesModService;
import ru.dimaskama.voicemessages.neoforge.client.NeoForgeVoiceRecordThread;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mod(VoiceMessages.ID)
public final class VoiceMessagesNeoForge {

    public static final PermissionNode<Boolean> VOICE_MESSAGE_SEND_PERMISSION = new PermissionNode<>(
            VoiceMessages.ID,
            "send",
            PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> true
    );
    public static final PermissionNode<Boolean> VOICE_MESSAGE_SEND_ALL_PERMISSION = new PermissionNode<>(
            VoiceMessages.ID,
            "send.all",
            PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> true
    );
    public static final PermissionNode<Boolean> VOICE_MESSAGE_SEND_TEAM_PERMISSION = new PermissionNode<>(
            VoiceMessages.ID,
            "send.team",
            PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> true
    );
    public static final PermissionNode<Boolean> VOICE_MESSAGE_SEND_PLAYERS_PERMISSION = new PermissionNode<>(
            VoiceMessages.ID,
            "send.players",
            PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> true
    );

    public VoiceMessagesNeoForge() {
        VoiceMessagesMod.init(ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString(), new VoiceMessagesModService() {
            @Override
            public boolean isModLoaded(String modId) {
                return ModList.get().isLoaded(modId);
            }

            @Override
            public void sendToServer(CustomPacketPayload payload) {
                PacketDistributor.sendToServer(payload);
            }

            @Override
            public boolean canSendToServer(ResourceLocation payloadId) {
                return net.minecraft.client.Minecraft.getInstance().getConnection().hasChannel(payloadId);
            }

            @Override
            public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
                PacketDistributor.sendToPlayer(player, payload);
            }

            @Override
            public VoiceRecordThread createVoiceRecordThread(Predicate<short[]> frameConsumer, Consumer<IOException> onMicError) {
                return new NeoForgeVoiceRecordThread(frameConsumer, onMicError);
            }

            @Override
            public boolean hasVoiceMessageSendPermission(ServerPlayer player) {
                return PermissionAPI.getPermission(player, VOICE_MESSAGE_SEND_PERMISSION);
            }

            @Override
            public boolean hasVoiceMessageSendAllPermission(ServerPlayer player) {
                return PermissionAPI.getPermission(player, VOICE_MESSAGE_SEND_ALL_PERMISSION);
            }

            @Override
            public boolean hasVoiceMessageSendTeamPermission(ServerPlayer player) {
                return PermissionAPI.getPermission(player, VOICE_MESSAGE_SEND_TEAM_PERMISSION);
            }

            @Override
            public boolean hasVoiceMessageSendPlayersPermission(ServerPlayer player) {
                return PermissionAPI.getPermission(player, VOICE_MESSAGE_SEND_PLAYERS_PERMISSION);
            }
        });
    }

}

package ru.dimaskama.voicemessages.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dimaskama.voicemessages.VoiceMessagesEvents;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.networking.VoiceMessagesServerNetworking;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin extends Player {

    @Unique
    @Nullable
    private static Boolean voicemessages_hasVoiceMessageSendPermission = null;

    private ServerPlayerMixin() {
        super(null, null, 0.0F, null);
    }

    @Inject(method = "doTick", at = @At("TAIL"))
    private void doTickTail(CallbackInfo ci) {
        if (VoiceMessagesMod.isActive()) {
            if (VoiceMessagesServerNetworking.hasCompatibleVersion((ServerPlayer) (Object) this)) {
                boolean voiceMessageSendPermission = VoiceMessagesMod.getService().hasVoiceMessageSendPermission((ServerPlayer) (Object) this);
                if (voicemessages_hasVoiceMessageSendPermission == null) {
                    voicemessages_hasVoiceMessageSendPermission = voiceMessageSendPermission;
                } else if (voicemessages_hasVoiceMessageSendPermission != voiceMessageSendPermission) {
                    voicemessages_hasVoiceMessageSendPermission = voiceMessageSendPermission;
                    VoiceMessagesEvents.sendPermissions((ServerPlayer) (Object) this);
                }
            }
            if (tickCount == 15) {
                VoiceMessagesEvents.checkForCompatibleVersion((ServerPlayer) (Object) this);
            }
        }
    }

}

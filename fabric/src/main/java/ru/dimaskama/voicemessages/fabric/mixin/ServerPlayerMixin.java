package ru.dimaskama.voicemessages.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dimaskama.voicemessages.VoiceMessagesEvents;
import ru.dimaskama.voicemessages.VoiceMessagesMod;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {

    @Unique
    @Nullable
    private static Boolean voicemessages_hasVoiceMessageSendPermission = null;

    // Since fabric-api and fabric-permissions-api have no event for permissions update, we are manually checking player's permissions for update
    @Inject(method = "doTick", at = @At("TAIL"))
    private void doTickTail(CallbackInfo ci) {
        if (VoiceMessagesMod.isActive()) {
            boolean voiceMessageSendPermission = VoiceMessagesMod.getService().hasVoiceMessageSendPermission((ServerPlayer) (Object) this);
            if (voicemessages_hasVoiceMessageSendPermission == null) {
                voicemessages_hasVoiceMessageSendPermission = voiceMessageSendPermission;
            } else if (voicemessages_hasVoiceMessageSendPermission != voiceMessageSendPermission) {
                voicemessages_hasVoiceMessageSendPermission = voiceMessageSendPermission;
                VoiceMessagesEvents.onPermissionsUpdated((ServerPlayer) (Object) this);
            }
        }
    }

}

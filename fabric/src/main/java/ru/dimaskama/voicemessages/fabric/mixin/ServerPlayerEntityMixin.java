package ru.dimaskama.voicemessages.fabric.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesEvents;

@Mixin(ServerPlayerEntity.class)
abstract class ServerPlayerEntityMixin {

    @Unique
    @Nullable
    private static Boolean voicemessages_hasVoiceMessageSendPermission = null;

    // Since fabric-api and fabric-permissions-api have no event for permissions update, we are manually checking player's permissions for update
    @Inject(method = "playerTick", at = @At("TAIL"))
    private void playerTickTail(CallbackInfo ci) {
        if (VoiceMessages.isActive()) {
            boolean voiceMessageSendPermission = VoiceMessages.getService().hasVoiceMessageSendPermission((ServerPlayerEntity) (Object) this);
            if (voicemessages_hasVoiceMessageSendPermission == null) {
                voicemessages_hasVoiceMessageSendPermission = voiceMessageSendPermission;
            } else if (voicemessages_hasVoiceMessageSendPermission != voiceMessageSendPermission) {
                voicemessages_hasVoiceMessageSendPermission = voiceMessageSendPermission;
                VoiceMessagesEvents.onPermissionsUpdated((ServerPlayerEntity) (Object) this);
            }
        }
    }

}

package ru.dimaskama.voicemessages.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dimaskama.voicemessages.VoiceMessagesEvents;
import ru.dimaskama.voicemessages.VoiceMessagesMod;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin extends Player {

    private ServerPlayerMixin() {
        super(null, null, 0.0F, null);
    }

    @Inject(method = "doTick", at = @At("TAIL"))
    private void doTickTail(CallbackInfo ci) {
        if (VoiceMessagesMod.isActive() && tickCount == 15) {
            VoiceMessagesEvents.checkForCompatibleVersion((ServerPlayer) (Object) this);
        }
    }

}

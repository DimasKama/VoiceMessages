package ru.dimaskama.voicemessages.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("TAIL"))
    private void onDisconnect(CallbackInfo ci) {
        if (VoiceMessagesMod.isActive()) {
            VoiceMessagesClientNetworking.resetConfig();
        }
    }

}

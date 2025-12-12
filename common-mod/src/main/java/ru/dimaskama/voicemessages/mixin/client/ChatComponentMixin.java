package ru.dimaskama.voicemessages.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.GuiMessageTagHack;
import ru.dimaskama.voicemessages.client.Playback;
import ru.dimaskama.voicemessages.client.PlaybackManager;
import ru.dimaskama.voicemessages.client.PlaybackPlayer;
import ru.dimaskama.voicemessages.client.screen.OverlayScreen;
import ru.dimaskama.voicemessages.duck.client.ChatComponentDuck;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatComponent.class)
abstract class ChatComponentMixin implements ChatComponentDuck {

    @Shadow @Final Minecraft minecraft;

    @Unique
    private List<PlaybackPlayer> voicemessages_visiblePlaybackPlayers;

    @ModifyReturnValue(method = "isChatFocused", at = @At("TAIL"))
    private boolean modifyChatFocused(boolean original) {
        if (VoiceMessagesMod.isActive()) {
            return original || minecraft.screen instanceof OverlayScreen overlayScreen && overlayScreen.parent instanceof ChatScreen;
        }
        return false;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V", at = @At("HEAD"))
    private void renderHead(CallbackInfo ci) {
        if (VoiceMessagesMod.isActive()) {
            if (voicemessages_visiblePlaybackPlayers == null) {
                voicemessages_visiblePlaybackPlayers = new ArrayList<>();
            } else {
                voicemessages_visiblePlaybackPlayers.clear();
            }
        }
    }

    @ModifyExpressionValue(
            method = "addMessageToDisplayQueue",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;removeLast()Ljava/lang/Object;"
            )
    )
    private Object clearRemovedMessage(Object original) {
        if (VoiceMessagesMod.isActive()) {
            Playback playback = GuiMessageTagHack.getPlayback((GuiMessage.Line) original);
            if (playback != null) {
                PlaybackManager.MAIN.remove(playback);
            }
            return original;
        }
        return original;
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void clearHead(CallbackInfo ci) {
        if (VoiceMessagesMod.isActive()) {
            PlaybackManager.MAIN.clearAll();
        }
    }

    @Override
    public List<PlaybackPlayer> voicemessages_getVisiblePlaybackPlayers() {
        return voicemessages_visiblePlaybackPlayers;
    }

}

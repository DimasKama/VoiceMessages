package ru.dimaskama.voicemessages.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
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

    @Shadow @Final private Minecraft minecraft;
    @Shadow public abstract int getWidth();
    @Shadow protected abstract int getLineHeight();
    @Shadow public abstract double getScale();

    @Unique
    private List<PlaybackPlayer> voicemessages_visiblePlaybackPlayers;

    @ModifyReturnValue(method = "isChatFocused", at = @At("TAIL"))
    private boolean modifyChatFocused(boolean original) {
        if (VoiceMessagesMod.isActive()) {
            return original || minecraft.screen instanceof OverlayScreen overlayScreen && overlayScreen.parent instanceof ChatScreen;
        }
        return false;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderHead(GuiGraphics guiGraphics, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (VoiceMessagesMod.isActive()) {
            if (voicemessages_visiblePlaybackPlayers == null) {
                voicemessages_visiblePlaybackPlayers = new ArrayList<>();
            } else {
                voicemessages_visiblePlaybackPlayers.clear();
            }
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I",
                    ordinal = 0
            )
    )
    private int wrapRenderLine(GuiGraphics instance, Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color, Operation<Integer> original, @Local GuiMessage.Line line) {
        int addX = original.call(instance, font, formattedCharSequence, x, y, color);
        if (VoiceMessagesMod.isActive()) {
            Playback playback = GuiMessageTagHack.getPlayback(line);
            if (playback != null) {
                double scale = getScale();
                if (scale >= 0.1) {
                    if (addX > 0) {
                        addX += 4;
                    }
                    int lineHeight = getLineHeight();
                    int playerX = x + addX;
                    int playerY = y - ((lineHeight - 8) >> 1);
                    int playerWidth = (int) (getWidth() / scale) - addX - x;
                    int playerHeight = lineHeight;
                    PlaybackPlayer player = new PlaybackPlayer(PlaybackManager.MAIN, playback, 0).setRectangle(
                            playerX,
                            playerY,
                            playerWidth,
                            playerHeight
                    );
                    player.setAlpha(ARGB.alpha(color));
                    player.render(instance);
                    player.transform(instance.pose().last().pose());
                    voicemessages_visiblePlaybackPlayers.add(player);
                }
            }
        }
        return addX;
    }

    @ModifyExpressionValue(
            method = "addMessageToDisplayQueue",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;remove(I)Ljava/lang/Object;"
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

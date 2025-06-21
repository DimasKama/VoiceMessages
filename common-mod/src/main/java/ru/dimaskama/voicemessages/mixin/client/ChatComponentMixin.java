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
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Vector3f;
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
import ru.dimaskama.voicemessages.client.screen.OverlayScreen;
import ru.dimaskama.voicemessages.client.screen.widget.PlaybackPlayerWidget;
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
    private List<PlaybackPlayerWidget> voicemessages_visiblePlaybackPlayerWidgets;

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
            if (voicemessages_visiblePlaybackPlayerWidgets == null) {
                voicemessages_visiblePlaybackPlayerWidgets = new ArrayList<>();
            } else {
                voicemessages_visiblePlaybackPlayerWidgets.clear();
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
            if (addX > 0) {
                addX += 4;
            }
            Playback playback = GuiMessageTagHack.getPlayback(line);
            if (playback != null) {
                int lineHeight = getLineHeight();
                PlaybackPlayerWidget playbackPlayerWidget = new PlaybackPlayerWidget(PlaybackManager.MAIN, playback, 0);
                Matrix4f matrix4f = instance.pose().last().pose();
                Vector3f vector3f = new Vector3f();
                vector3f.set(x + addX, y - ((lineHeight - 9) >> 1), 0.0F);
                matrix4f.transformPosition(vector3f);
                int playbackWidth = getWidth() - addX - x;
                playbackPlayerWidget.setRectangle(
                        playbackWidth,
                        lineHeight,
                        (int) vector3f.x,
                        (int) vector3f.y
                );
                addX += playbackWidth;
                playbackPlayerWidget.setScale((float) getScale());
                voicemessages_visiblePlaybackPlayerWidgets.add(playbackPlayerWidget);
            }
        }
        return addX;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderTail(GuiGraphics guiGraphics, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (VoiceMessagesMod.isActive()) {
            if (!voicemessages_visiblePlaybackPlayerWidgets.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0.0F, 0.0F, 150.0);
                for (PlaybackPlayerWidget playbackPlayerWidget : voicemessages_visiblePlaybackPlayerWidgets) {
                    playbackPlayerWidget.render(guiGraphics, mouseX, mouseY, 1.0F);
                }
                guiGraphics.pose().popPose();
            }
        }
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
    public List<PlaybackPlayerWidget> voicemessages_getVisiblePlaybackPlayerWidgets() {
        return voicemessages_visiblePlaybackPlayerWidgets;
    }

}

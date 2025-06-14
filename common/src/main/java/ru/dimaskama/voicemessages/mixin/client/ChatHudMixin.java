package ru.dimaskama.voicemessages.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.OrderedText;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.client.MessageIndicatorHack;
import ru.dimaskama.voicemessages.client.Playback;
import ru.dimaskama.voicemessages.client.PlaybackManager;
import ru.dimaskama.voicemessages.client.screen.OverlayScreen;
import ru.dimaskama.voicemessages.client.screen.widget.PlaybackPlayerWidget;
import ru.dimaskama.voicemessages.duck.client.ChatHudDuck;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatHud.class)
abstract class ChatHudMixin implements ChatHudDuck {

    @Shadow @Final private MinecraftClient client;
    @Shadow public abstract int getWidth();
    @Shadow protected abstract int getLineHeight();
    @Shadow public abstract double getChatScale();

    @Unique
    private List<PlaybackPlayerWidget> voicemessages_visiblePlaybackPlayerWidgets;

    @ModifyReturnValue(method = "isChatFocused", at = @At("TAIL"))
    private boolean modifyChatFocused(boolean original) {
        if (VoiceMessages.isActive()) {
            return original || client.currentScreen instanceof OverlayScreen overlayScreen && overlayScreen.parent instanceof ChatScreen;
        }
        return false;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderHead(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (VoiceMessages.isActive()) {
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
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I",
                    ordinal = 0
            )
    )
    private int wrapRenderLine(DrawContext instance, TextRenderer textRenderer, OrderedText text, int x, int y, int color, Operation<Integer> original, @Local ChatHudLine.Visible visible) {
        int addX = original.call(instance, textRenderer, text, x, y, color);
        if (addX > 0) {
            addX += 4;
        }
        if (VoiceMessages.isActive()) {
            Playback playback = MessageIndicatorHack.getPlayback(visible);
            if (playback != null) {
                int lineHeight = getLineHeight();
                PlaybackPlayerWidget playbackPlayerWidget = new PlaybackPlayerWidget(PlaybackManager.MAIN, playback, 0);
                Matrix4f matrix4f = instance.getMatrices().peek().getPositionMatrix();
                Vector3f vector3f = new Vector3f();
                vector3f.set(x + addX, y - ((lineHeight - 9) >> 1), 0.0F);
                matrix4f.transformPosition(vector3f);
                int playbackWidth = getWidth() - addX - x;
                playbackPlayerWidget.setDimensionsAndPosition(
                        playbackWidth,
                        lineHeight,
                        (int) vector3f.x,
                        (int) vector3f.y
                );
                addX += playbackWidth;
                playbackPlayerWidget.setScale((float) getChatScale());
                voicemessages_visiblePlaybackPlayerWidgets.add(playbackPlayerWidget);
            }
        }
        return addX;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderTail(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (!voicemessages_visiblePlaybackPlayerWidgets.isEmpty()) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 150.0);
            if (VoiceMessages.isActive()) {
                for (PlaybackPlayerWidget playbackPlayerWidget : voicemessages_visiblePlaybackPlayerWidgets) {
                    playbackPlayerWidget.render(context, mouseX, mouseY, 1.0F);
                }
            }
            context.getMatrices().pop();
        }
    }

    @ModifyExpressionValue(
            method = "addVisibleMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;remove(I)Ljava/lang/Object;"
            )
    )
    private Object clearRemovedMessage(Object original) {
        if (VoiceMessages.isActive()) {
            Playback playback = MessageIndicatorHack.getPlayback((ChatHudLine.Visible) original);
            if (playback != null) {
                PlaybackManager.MAIN.remove(playback);
            }
            return original;
        }
        return original;
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void clearHead(CallbackInfo ci) {
        if (VoiceMessages.isActive()) {
            PlaybackManager.MAIN.clearAll();
        }
    }

    @Override
    public List<PlaybackPlayerWidget> voicemessages_getVisiblePlaybackPlayerWidgets() {
        return voicemessages_visiblePlaybackPlayerWidgets;
    }

}

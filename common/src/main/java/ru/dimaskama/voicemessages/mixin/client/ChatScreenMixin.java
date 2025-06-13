package ru.dimaskama.voicemessages.mixin.client;

import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.client.screen.RecordVoiceMessageScreen;
import ru.dimaskama.voicemessages.client.screen.widget.PlaybackPlayerWidget;
import ru.dimaskama.voicemessages.duck.client.ChatHudDuck;
import ru.dimaskama.voicemessages.networking.VoiceMessageC2S;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin extends Screen {

    @Shadow protected TextFieldWidget chatField;

    @Unique
    private static final ButtonTextures voicemessages_BUTTON_TEXTURES = new ButtonTextures(
            VoiceMessages.id("microphone"),
            VoiceMessages.id("microphone_disabled"),
            VoiceMessages.id("microphone_hovered")
    );
    @Unique
    private boolean voicemessages_canSendVoiceMessages;
    @Unique
    private TexturedButtonWidget voicemessages_button;

    private ChatScreenMixin() {
        super(null);
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void initTail(CallbackInfo ci) {
        if (VoiceMessages.isActive()) {
            voicemessages_canSendVoiceMessages = VoiceMessagesClientNetworking.canSendVoiceMessages()
                    && VoiceMessages.getService().canSendToServer(VoiceMessageC2S.ID.id());
            if (voicemessages_canSendVoiceMessages) {
                int x = chatField.getX();
                int y = chatField.getY();
                voicemessages_button = addDrawableChild(new TexturedButtonWidget(
                        x - 3,
                        y - 3,
                        14,
                        14,
                        voicemessages_BUTTON_TEXTURES,
                        button -> client.setScreen(new RecordVoiceMessageScreen(this, button.getX(), height - button.getY() + 1))
                ));
                voicemessages_button.active = client.currentScreen == this;
                chatField.setWidth(chatField.getWidth() - 14);
                chatField.setX(x + 14);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClickedHead(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (VoiceMessages.isActive()) {
            for (PlaybackPlayerWidget playbackPlayerWidget : ((ChatHudDuck) client.inGameHud.getChatHud()).voicemessages_getVisiblePlaybackPlayerWidgets()) {
                if (playbackPlayerWidget.mouseClicked(mouseX, mouseY, button)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderHead(CallbackInfo ci) {
        if (VoiceMessages.isActive()) {
            // Always focus on chat input field
            if (getFocused() == voicemessages_button) {
                setFocused(chatField);
            }
        }
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
            ),
            index = 0
    )
    private int modifyChatFieldBackgroundX(int x1) {
        if (VoiceMessages.isActive()) {
            return voicemessages_canSendVoiceMessages ? x1 + 14 : x1;
        }
        return x1;
    }

}

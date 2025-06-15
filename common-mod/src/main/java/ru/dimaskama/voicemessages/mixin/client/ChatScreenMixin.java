package ru.dimaskama.voicemessages.mixin.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.client.screen.RecordVoiceMessageScreen;
import ru.dimaskama.voicemessages.client.screen.widget.PlaybackPlayerWidget;
import ru.dimaskama.voicemessages.duck.client.ChatComponentDuck;
import ru.dimaskama.voicemessages.networking.VoiceMessageC2S;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin extends Screen {

    @Shadow protected EditBox input;

    @Unique
    private static final WidgetSprites voicemessages_WIDGET_SPRITES = new WidgetSprites(
            VoiceMessagesMod.id("microphone"),
            VoiceMessagesMod.id("microphone_disabled"),
            VoiceMessagesMod.id("microphone_hovered")
    );
    @Unique
    private boolean voicemessages_canSendVoiceMessages;
    @Unique
    private ImageButton voicemessages_button;

    private ChatScreenMixin() {
        super(null);
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void initTail(CallbackInfo ci) {
        if (VoiceMessagesMod.isActive()) {
            voicemessages_canSendVoiceMessages = VoiceMessagesClientNetworking.canSendVoiceMessages()
                    && VoiceMessagesMod.getService().canSendToServer(VoiceMessageC2S.TYPE.id());
            if (voicemessages_canSendVoiceMessages) {
                int x = input.getX();
                int y = input.getY();
                voicemessages_button = addRenderableWidget(new ImageButton(
                        x - 3,
                        y - 3,
                        14,
                        14,
                        voicemessages_WIDGET_SPRITES,
                        button -> minecraft.setScreen(new RecordVoiceMessageScreen(this, button.getX(), height - button.getY() + 1))
                ));
                voicemessages_button.active = minecraft.screen == this;
                input.setWidth(input.getWidth() - 14);
                input.setX(x + 14);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClickedHead(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (VoiceMessagesMod.isActive()) {
            for (PlaybackPlayerWidget playbackPlayerWidget : ((ChatComponentDuck) minecraft.gui.getChat()).voicemessages_getVisiblePlaybackPlayerWidgets()) {
                if (playbackPlayerWidget.mouseClicked(mouseX, mouseY, button)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderHead(CallbackInfo ci) {
        if (VoiceMessagesMod.isActive()) {
            // Always focus on chat input field
            if (getFocused() == voicemessages_button) {
                setFocused(input);
            }
        }
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
            ),
            index = 0
    )
    private int modifyChatFieldBackgroundX(int x1) {
        if (VoiceMessagesMod.isActive()) {
            return voicemessages_canSendVoiceMessages ? x1 + 14 : x1;
        }
        return x1;
    }

}

package ru.dimaskama.voicemessages.mixin.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.PlaybackPlayer;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.client.screen.RecordVoiceMessageScreen;
import ru.dimaskama.voicemessages.client.screen.VoiceMessageConfirmScreen;
import ru.dimaskama.voicemessages.duck.client.ChatComponentDuck;

import java.util.List;

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
            List<String> availableTargets = VoiceMessagesClientNetworking.getAvailableTargets();
            String target;
            voicemessages_canSendVoiceMessages = false;
            if (!availableTargets.isEmpty()) {
                target = availableTargets.getFirst();
                // If the first target is not the player
                if (minecraft.getConnection().getPlayerInfo(target) == null) {
                    voicemessages_canSendVoiceMessages = true;
                }
            } else {
                target = null;
            }
            if (voicemessages_canSendVoiceMessages) {
                int x = input.getX();
                int y = input.getY();
                voicemessages_button = addRenderableWidget(new ImageButton(
                        x - 3,
                        y - 3,
                        14,
                        14,
                        voicemessages_WIDGET_SPRITES,
                        button -> minecraft.setScreen(new RecordVoiceMessageScreen(this, button.getX(), height - button.getY() + 1, target))
                ));
                if (minecraft.screen == this) {
                    voicemessages_button.setTooltip(Tooltip.create(
                            VoiceMessages.TARGET_ALL.equals(target)
                                    ? Component.translatable("voicemessages.voice_message")
                                    : Component.translatable("voicemessages.voice_message_to", VoiceMessageConfirmScreen.getTargetText(target))
                    ));
                } else {
                    voicemessages_button.active = false;
                }
                input.setWidth(input.getWidth() - 14);
                input.setX(x + 14);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClickedHead(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (VoiceMessagesMod.isActive()) {
            for (PlaybackPlayer player : ((ChatComponentDuck) minecraft.gui.getChat()).voicemessages_getVisiblePlaybackPlayers()) {
                if (player.mouseClicked((int) mouseX, (int) mouseY, button)) {
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

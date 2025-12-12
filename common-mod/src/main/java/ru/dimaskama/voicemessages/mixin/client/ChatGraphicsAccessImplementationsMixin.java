package ru.dimaskama.voicemessages.mixin.client;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.GuiMessageTagHack;
import ru.dimaskama.voicemessages.client.Playback;
import ru.dimaskama.voicemessages.client.PlaybackManager;
import ru.dimaskama.voicemessages.client.PlaybackPlayer;
import ru.dimaskama.voicemessages.duck.client.ChatComponentDuck;

@Mixin(targets = {
        "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess",
        "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"
})
abstract class ChatGraphicsAccessImplementationsMixin {

    @Shadow(remap = false, aliases = {"graphics", "field_64427", "field_63876"})
    @Final
    private GuiGraphics graphics;
    @Unique
    private FormattedCharSequence voicemessages_lastText;

    @Inject(method = "handleMessage", at = @At("TAIL"))
    private void handleMessageTail(int i, float f, FormattedCharSequence formattedCharSequence, CallbackInfoReturnable<Boolean> cir) {
        voicemessages_lastText = formattedCharSequence;
    }

    @Inject(method = "handleTag", at = @At("TAIL"))
    private void handleTagTail(int x1, int y1, int x2, int y2, float alpha, GuiMessageTag tag, CallbackInfo ci) {
        if (VoiceMessagesMod.isActive() && voicemessages_lastText != null) {
            int y = y1;
            int x = x2 + 2;
            Playback playback = GuiMessageTagHack.getPlayback(tag);
            if (playback != null) {
                double scale = Minecraft.getInstance().options.chatScale().get();
                if (scale >= 0.1) {
                    int addX = Minecraft.getInstance().font.width(voicemessages_lastText);
                    if (addX > 0) {
                        addX += 4;
                    }
                    int lineHeight = (int) (9.0 * (Minecraft.getInstance().options.chatLineSpacing().get() + 1.0));
                    int playerX = x + addX;
                    int playerY = y + 1;
                    int playerWidth = (int) (ChatComponent.getWidth(Minecraft.getInstance().options.chatWidth().get()) / scale) - addX - x;
                    int playerHeight = lineHeight;
                    PlaybackPlayer player = new PlaybackPlayer(PlaybackManager.MAIN, playback, 0).setRectangle(
                            playerX,
                            playerY,
                            playerWidth,
                            playerHeight
                    );
                    player.setAlpha(ARGB.as8BitChannel(alpha));
                    player.render(graphics);
                    player.transform(graphics.pose());
                    ((ChatComponentDuck) Minecraft.getInstance().gui.getChat()).voicemessages_getVisiblePlaybackPlayers().add(player);
                }
            }
        }
    }
    
}

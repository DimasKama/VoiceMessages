package ru.dimaskama.voicemessages.mixin.client;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.dimaskama.voicemessages.duck.client.ChatHudLineVisibleDuck;

@Mixin(ChatHudLine.Visible.class)
abstract class ChatHudLineVisibleMixin implements ChatHudLineVisibleDuck {

    @Shadow @Final private MessageIndicator indicator;

    @Override
    public MessageIndicator voicemessages_getMessageIndicator() {
        return indicator;
    }

}

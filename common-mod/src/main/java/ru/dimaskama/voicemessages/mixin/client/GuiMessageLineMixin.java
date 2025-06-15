package ru.dimaskama.voicemessages.mixin.client;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.dimaskama.voicemessages.duck.client.GuiMessageLineDuck;

@Mixin(GuiMessage.Line.class)
abstract class GuiMessageLineMixin implements GuiMessageLineDuck {

    @Shadow @Final private GuiMessageTag tag;

    @Override
    public GuiMessageTag voicemessages_getGuiMessageTag() {
        return tag;
    }

}

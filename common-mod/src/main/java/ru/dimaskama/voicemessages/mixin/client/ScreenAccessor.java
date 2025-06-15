package ru.dimaskama.voicemessages.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {

    @Invoker("init")
    void voicemessages_init();

    @Invoker("rebuildWidgets")
    void voicemessages_rebuildWidgets();

}

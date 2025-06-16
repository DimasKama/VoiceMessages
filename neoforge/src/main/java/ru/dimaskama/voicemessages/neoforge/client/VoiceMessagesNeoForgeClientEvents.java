package ru.dimaskama.voicemessages.neoforge.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.VoiceMessagesClientEvents;

@EventBusSubscriber(modid = VoiceMessages.ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class VoiceMessagesNeoForgeClientEvents {

    @SubscribeEvent
    private static void onClientTick(ClientTickEvent.Post event) {
        VoiceMessagesClientEvents.onClientTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    private static void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (VoiceMessagesMod.isActive()) {
            VoiceMessagesClientEvents.onJoinedServer();
        }
    }

}

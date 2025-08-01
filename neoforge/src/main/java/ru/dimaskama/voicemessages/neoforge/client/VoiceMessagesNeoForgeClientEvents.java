package ru.dimaskama.voicemessages.neoforge.client;

import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.client.VoiceMessagesClientEvents;
import ru.dimaskama.voicemessages.client.VoicemsgCommand;

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

    @SubscribeEvent
    private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        if (VoiceMessagesMod.isActive()) {
            LiteralCommandNode<CommandSourceStack> command = event.getDispatcher().register(new VoicemsgCommand<>(
                    Commands::literal,
                    Commands::argument
            ).createCommand());
            event.getDispatcher().register(Commands.literal(VoicemsgCommand.ALIAS)
                    .executes(command.getCommand())
                    .redirect(command));
        }
    }

}

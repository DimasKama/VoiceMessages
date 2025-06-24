package ru.dimaskama.voicemessages.client;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.client.screen.RecordVoiceMessageScreen;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class VoicemsgCommand<S> {

    public static final String ALIAS = "vmsg";
    private static final DynamicCommandExceptionType UNKNOWN_TARGET = new DynamicCommandExceptionType(o -> new LiteralMessage("Unknown target"));

    private final LiteralFactory<S> literal;
    private final ArgumentFactory<S> argument;

    public VoicemsgCommand(LiteralFactory<S> literal, ArgumentFactory<S> argument) {
        this.literal = literal;
        this.argument = argument;
    }

    public LiteralArgumentBuilder<S> createCommand() {
        return literal.get("voicemsg")
                .then(argument.get("target", StringArgumentType.greedyString())
                        .suggests((context, builder) -> suggestTarget(builder))
                        .executes(this::execute));
    }

    private int execute(CommandContext<S> context) throws CommandSyntaxException {
        String target = StringArgumentType.getString(context, "target");
        if (!VoiceMessagesClientNetworking.getAvailableTargets().contains(target)) {
            throw UNKNOWN_TARGET.create(target);
        }
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        minecraft.tell(() -> minecraft.setScreen(new RecordVoiceMessageScreen(
                screen instanceof ChatScreen ? screen : new ChatScreen(""),
                1,
                15,
                target
        )));
        return 0;
    }

    private CompletableFuture<Suggestions> suggestTarget(SuggestionsBuilder builder) {
        String remain = builder.getRemainingLowerCase();
        VoiceMessagesClientNetworking.getAvailableTargets()
                .stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(remain))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    @FunctionalInterface
    public interface LiteralFactory<S> {

        LiteralArgumentBuilder<S> get(String literal);

    }

    @FunctionalInterface
    public interface ArgumentFactory<S> {

        <T> RequiredArgumentBuilder<S, T> get(String argument, ArgumentType<T> type);

    }

}

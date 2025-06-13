package ru.dimaskama.voicemessages.client;

import com.mojang.datafixers.util.Pair;
import com.mojang.util.UndashedUuid;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.PlayerListEntry;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.voicemessages.duck.client.ChatHudLineVisibleDuck;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MessageIndicatorHack {

    @Nullable
    public static Pair<Optional<PlayerListEntry>, Playback> getPlayback(ChatHudLine.Visible chatHudLine) {
        MessageIndicator indicator = ((ChatHudLineVisibleDuck) (Object) chatHudLine).voicemessages_getMessageIndicator();
        return indicator != null ? getPlayback(indicator) : null;
    }

    @Nullable
    public static Pair<Optional<PlayerListEntry>, Playback> getPlayback(MessageIndicator indicator) {
        String text = indicator.loggedName();
        if (text != null && text.startsWith("VoiceMessage#")) {
            String[] splited = text.split("#");
            UUID playbackUuid = UndashedUuid.fromStringLenient(splited[1]);
            Playback playback = PlaybackManager.MAIN.get(playbackUuid);
            if (playback != null) {
                PlayerListEntry sender = null;
                if (splited.length >= 3) {
                    UUID senderUuid = UndashedUuid.fromStringLenient(splited[2]);
                    sender = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(senderUuid);
                }
                return Pair.of(Optional.ofNullable(sender), playback);
            }
        }
        return null;
    }

    public static MessageIndicator createPlayback(@Nullable PlayerListEntry sender, List<short[]> audio) {
        UUID uuid = PlaybackManager.MAIN.add(audio);
        String text = "VoiceMessage#" + UndashedUuid.toString(uuid);
        if (sender != null) {
            text += "#" + UndashedUuid.toString(sender.getProfile().getId());
        }
        return new MessageIndicator(0xFF5555FF, null, null, text);
    }

}

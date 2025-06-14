package ru.dimaskama.voicemessages.client;

import com.mojang.util.UndashedUuid;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.voicemessages.duck.client.ChatHudLineVisibleDuck;

import java.util.List;
import java.util.UUID;

public final class MessageIndicatorHack {

    @Nullable
    public static Playback getPlayback(ChatHudLine.Visible chatHudLine) {
        MessageIndicator indicator = ((ChatHudLineVisibleDuck) (Object) chatHudLine).voicemessages_getMessageIndicator();
        return indicator != null ? getPlayback(indicator) : null;
    }

    @Nullable
    public static Playback getPlayback(MessageIndicator indicator) {
        String text = indicator.loggedName();
        if (text != null && text.startsWith("VoiceMessage#")) {
            String[] splited = text.split("#");
            UUID playbackUuid = UndashedUuid.fromStringLenient(splited[1]);
            return PlaybackManager.MAIN.get(playbackUuid);
        }
        return null;
    }

    public static MessageIndicator createPlayback(List<short[]> audio) {
        UUID uuid = PlaybackManager.MAIN.add(audio);
        String text = "VoiceMessage#" + UndashedUuid.toString(uuid);
        return new MessageIndicator(0xFF5555FF, null, null, text);
    }

}

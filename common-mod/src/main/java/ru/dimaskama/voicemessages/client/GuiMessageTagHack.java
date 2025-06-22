package ru.dimaskama.voicemessages.client;

import com.mojang.util.UndashedUuid;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.voicemessages.duck.client.GuiMessageLineDuck;

import java.util.List;
import java.util.UUID;

public final class GuiMessageTagHack {

    @Nullable
    public static Playback getPlayback(GuiMessage.Line chatHudLine) {
        GuiMessageTag tag = ((GuiMessageLineDuck) (Object) chatHudLine).voicemessages_getGuiMessageTag();
        return tag != null ? getPlayback(tag) : null;
    }

    @Nullable
    public static Playback getPlayback(GuiMessageTag tag) {
        String text = tag.logTag();
        if (text != null && text.startsWith("VoiceMessage#")) {
            String[] splited = text.split("#");
            UUID playbackUuid = UndashedUuid.fromStringLenient(splited[1]);
            return PlaybackManager.MAIN.get(playbackUuid);
        }
        return null;
    }

    public static GuiMessageTag createAndAdd(List<short[]> audio) {
        UUID uuid = PlaybackManager.MAIN.addFromChat(audio);
        String text = "VoiceMessage#" + UndashedUuid.toString(uuid);
        return new GuiMessageTag(0xFF5555FF, null, null, text);
    }

}

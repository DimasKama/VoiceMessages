package ru.dimaskama.voicemessages.client;

import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlaybackManager {

    public static final PlaybackManager MAIN = new PlaybackManager();
    private final Map<UUID, Playback> playbacks = new HashMap<>();
    private Playback lastInChat;
    private Playback playingNow;

    private PlaybackManager() {}

    public void stopPlaying() {
        if (playingNow != null && playingNow.isPlaying()) {
            playingNow.stop();
            playingNow = null;
        }
    }

    public Playback get(UUID uuid) {
        return playbacks.get(uuid);
    }

    public UUID addFromChat(List<short[]> audio) {
        Playback playback = new Playback(audio);
        addFromChat(playback);
        return playback.getChannel().getId();
    }

    public void addFromChat(Playback playback) {
        if (lastInChat != null) {
            lastInChat.setOnFinish(() -> Minecraft.getInstance().execute(() -> {
                playback.setProgress(0.0F);
                play(playback);
            }));
        }
        lastInChat = playback;
        add(playback);
    }

    public void add(Playback playback) {
        playbacks.put(playback.getChannel().getId(), playback);
    }

    public void remove(Playback playback) {
        playbacks.remove(playback.getChannel().getId());
    }

    public void play(Playback playback) {
        if (playingNow != playback || !playback.isPlaying()) {
            add(playback);
            stopPlaying();
            playback.play();
            playingNow = playback;
        }
    }

    public void clearAll() {
        stopPlaying();
        playbacks.clear();
    }

}

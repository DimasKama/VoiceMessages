package ru.dimaskama.voicemessages.client;

import de.maxhenkel.voicechat.api.audiochannel.ClientAudioChannel;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesPlugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Playback {

    private static final ScheduledExecutorService SOUND_PLAYER_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r ->
            new Thread(r, "VoiceMessagesPlayer"));
    private final ClientAudioChannel channel;
    private final List<short[]> audio;
    private final AtomicInteger framePosition = new AtomicInteger();
    private volatile ScheduledFuture<?> playFuture;

    public Playback(List<short[]> audio) {
        this(createChannel(), audio);
    }

    public Playback(ClientAudioChannel channel, List<short[]> audio) {
        this.channel = channel;
        this.audio = audio;
    }

    private static ClientAudioChannel createChannel() {
        ClientAudioChannel channel = VoiceMessagesPlugin.getClientApi().createStaticAudioChannel(UUID.randomUUID());
        channel.setCategory(VoiceMessagesPlugin.getVolumeCategory().getId());
        return channel;
    }

    public ClientAudioChannel getChannel() {
        return channel;
    }

    public List<short[]> getAudio() {
        return audio;
    }

    public int getFramePosition() {
        return framePosition.get();
    }

    public int getDurationMs() {
        return 1000 * audio.size() / VoiceMessages.FRAMES_PER_SEC;
    }

    public float getProgress() {
        return (float) framePosition.get() / audio.size();
    }

    public void setProgress(float progress) {
        framePosition.set(Math.round(progress * audio.size()));
    }

    public boolean isPlaying() {
        synchronized (this) {
            return playFuture != null && !playFuture.isDone();
        }
    }

    public void play() {
        synchronized (this) {
            if (getProgress() >= 1.0F) {
                setProgress(0.0F);
            }
            if (playFuture == null || playFuture.isDone()) {
                playFuture = SOUND_PLAYER_EXECUTOR.scheduleAtFixedRate(this::playNextFrame, 0L, 1000L / VoiceMessages.FRAMES_PER_SEC, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void stop() {
        synchronized (this) {
            if (playFuture != null) {
                playFuture.cancel(true);
            }
        }
    }

    private void playNextFrame() {
        int pos = framePosition.getAndIncrement();
        if (pos < 0 || pos >= audio.size()) {
            throw new RuntimeException("playback finished");
        }
        channel.play(audio.get(pos));
    }

}

package ru.dimaskama.voicemessages.client;

import de.maxhenkel.voicechat.api.audiochannel.ClientAudioChannel;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.util.Mth;
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
    private final FloatList audioLevels;
    private final AtomicInteger framePosition = new AtomicInteger();
    private volatile ScheduledFuture<?> playFuture;
    private volatile Runnable onFinish;

    public Playback(List<short[]> audio) {
        this(audio, calculateAudioLevels(audio));
    }

    public Playback(List<short[]> audio, FloatList audioLevels) {
        this(createChannel(), audio, audioLevels);
    }

    public Playback(ClientAudioChannel channel, List<short[]> audio, FloatList audioLevels) {
        this.channel = channel;
        this.audio = audio;
        this.audioLevels = audioLevels;
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

    public FloatList getAudioLevels() {
        return audioLevels;
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

    public synchronized boolean isPlaying() {
        return playFuture != null && !playFuture.isDone();
    }

    public synchronized void play() {
        if (getProgress() >= 1.0F) {
            setProgress(0.0F);
        }
        if (playFuture == null || playFuture.isDone()) {
            playFuture = SOUND_PLAYER_EXECUTOR.scheduleAtFixedRate(this::playNextFrame, 0L, 1000L / VoiceMessages.FRAMES_PER_SEC, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void stop() {
        if (playFuture != null) {
            playFuture.cancel(true);
        }
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    private void playNextFrame() {
        int pos = framePosition.getAndIncrement();
        if (pos < 0 || pos >= audio.size()) {
            Runnable onFinish = this.onFinish;
            if (onFinish != null) {
                onFinish.run();
            }
            throw new RuntimeException("playback finished");
        }
        channel.play(audio.get(pos));
    }

    public static float calculateAudioLevel(short[] frame) {
        float rms = 0.0F;

        for (short value : frame) {
            float sample = (float) value / 32767.0F;
            rms += sample * sample;
        }

        int sampleCount = frame.length / 2;
        rms = sampleCount == 0 ? 0.0F : (float) Math.sqrt(rms / sampleCount);

        float db = rms > 0.0F
                ? Math.min(Math.max(20.0F * (float) Math.log10(rms), -127.0F), 0.0F)
                : -127.0F;

        return Mth.clamp(Mth.inverseLerp(db, -40.0F, -15.0F), 0.0F, 0.999F);
    }

    public static FloatList calculateAudioLevels(List<short[]> audio) {
        FloatList levels = new FloatArrayList(audio.size());
        for (short[] shorts : audio) {
            levels.add(calculateAudioLevel(shorts));
        }
        return levels;
    }

}

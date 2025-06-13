package ru.dimaskama.voicemessages.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesService;
import ru.dimaskama.voicemessages.client.PlaybackManager;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.client.render.PlaybackRenderer;
import ru.dimaskama.voicemessages.client.screen.widget.PlaybackPlayerWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordVoiceMessageScreen extends OverlayScreen {

    public static final ButtonTextures CANCEL_TEXTURES = new ButtonTextures(
            VoiceMessages.id("cancel"),
            VoiceMessages.id("cancel_hovered")
    );
    private static final ButtonTextures DONE_TEXTURES = new ButtonTextures(
            VoiceMessages.id("done"),
            VoiceMessages.id("done_hovered")
    );
    private final int leftX;
    private final int fromBottomY;
    private volatile boolean recorded;
    private List<short[]> recordedFrames = new ArrayList<>();
    private VoiceMessagesService.VoiceRecordThread recordThread;
    private Exception microphoneException;
    private TexturedButtonWidget doneButton, cancelButton;

    public RecordVoiceMessageScreen(Screen parent, int leftX, int fromBottomY) {
        super(Text.translatable("voicemessages.recording"), parent);
        this.leftX = leftX;
        this.fromBottomY = fromBottomY;
    }

    @Override
    protected void init() {
        super.init();
        PlaybackManager.MAIN.stopPlaying();
        if (!recorded && recordThread == null) {
            recordThread = VoiceMessages.getService().createVoiceRecordThread(this::appendFrame, e -> microphoneException = e);
            recordThread.startVoiceRecord();
        }
        doneButton = addDrawableChild(new TexturedButtonWidget(
                14,
                14,
                DONE_TEXTURES,
                button -> {
                    stopRecording();
                    if (recorded) {
                        close();
                    }
                },
                ScreenTexts.EMPTY
        ));
        doneButton.setTooltip(Tooltip.of(ScreenTexts.DONE));
        cancelButton = addDrawableChild(new TexturedButtonWidget(
                14,
                14,
                CANCEL_TEXTURES,
                button -> client.setScreen(parent),
                ScreenTexts.EMPTY
        ));
        cancelButton.setTooltip(Tooltip.of(ScreenTexts.CANCEL));
    }

    private boolean appendFrame(short[] frame) {
        recordedFrames.add(frame);
        if (recordedFrames.size() < VoiceMessagesClientNetworking.getMaxVoiceMessageFrames()) {
            return true;
        }
        recordThread = null;
        onStoppedRecording();
        return false;
    }

    private void stopRecording() {
        if (recordThread != null) {
            recordThread.stopVoiceRecord();
            recordThread = null;
            onStoppedRecording();
        }
    }

    private void onStoppedRecording() {
        recordedFrames = Collections.unmodifiableList(recordedFrames);
        recorded = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (microphoneException != null) {
            VoiceMessages.LOGGER.warn("Microphone error", microphoneException);
            client.player.sendMessage(Text.translatable("voicemessages.microphone_error", microphoneException.getLocalizedMessage())
                    .formatted(Formatting.RED), true);
            client.setScreen(null);
        } else if (recorded) {
            close();
        }
    }

    @Override
    public void close() {
        if (recordedFrames.isEmpty()) {
            super.close();
        } else {
            client.setScreen(new VoiceMessageConfirmScreen(parent, leftX, fromBottomY, recordedFrames));
        }
    }

    @Override
    public void removed() {
        stopRecording();
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    protected void actualRender(DrawContext context, int mouseX, int mouseY, float delta) {
        int bottomY = height - fromBottomY;
        context.fill(leftX - 1, bottomY - 16, leftX + 243, bottomY + 1, 0xFFFFFFFF);
        context.fill(leftX, bottomY - 15, leftX + 242, bottomY, 0xFFFF5555);
        float recordProgress = (float) recordedFrames.size() / VoiceMessagesClientNetworking.getMaxVoiceMessageFrames();
        PlaybackRenderer.renderPlayback(
                context,
                leftX + 1,
                bottomY - 15,
                240,
                15,
                PlaybackRenderer.getSeed(recordedFrames),
                recordProgress,
                recordProgress
        );
        int maxDuration = VoiceMessagesClientNetworking.getMaxVoiceMessageDurationMs();
        String timeStr = PlaybackPlayerWidget.formatTime((int) (recordProgress * maxDuration))
                + '/'
                + PlaybackPlayerWidget.formatTime(maxDuration);
        context.drawTextWithShadow(
                textRenderer,
                timeStr,
                leftX + 247,
                bottomY - 12,
                0xFFFFFFFF
        );
        int timeStrWidth = textRenderer.getWidth(timeStr);

        doneButton.setPosition(leftX + 247 + timeStrWidth + 5, bottomY - 15);
        cancelButton.setPosition(leftX + 247 + timeStrWidth + 21, bottomY - 15);

        super.actualRender(context, mouseX, mouseY, delta);
    }

}

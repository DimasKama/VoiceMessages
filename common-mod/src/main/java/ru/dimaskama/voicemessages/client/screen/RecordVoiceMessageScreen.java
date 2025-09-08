package ru.dimaskama.voicemessages.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.floats.FloatLists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.voicemessages.VoiceMessages;
import ru.dimaskama.voicemessages.VoiceMessagesMod;
import ru.dimaskama.voicemessages.VoiceMessagesModService;
import ru.dimaskama.voicemessages.client.Playback;
import ru.dimaskama.voicemessages.client.PlaybackManager;
import ru.dimaskama.voicemessages.client.PlaybackPlayer;
import ru.dimaskama.voicemessages.client.networking.VoiceMessagesClientNetworking;
import ru.dimaskama.voicemessages.client.render.PlaybackRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordVoiceMessageScreen extends OverlayScreen {

    public static final WidgetSprites CANCEL_SPRITES = new WidgetSprites(
            VoiceMessagesMod.id("cancel"),
            VoiceMessagesMod.id("cancel_hovered")
    );
    private static final WidgetSprites DONE_SPRITES = new WidgetSprites(
            VoiceMessagesMod.id("done"),
            VoiceMessagesMod.id("done_hovered")
    );
    private final int leftX;
    private final int fromBottomY;
    private final String target;
    @Nullable
    private final Component targetText;
    private volatile boolean recorded;
    private List<short[]> recordedFrames = new ArrayList<>();
    private FloatList audioLevels = new FloatArrayList();
    private VoiceMessagesModService.VoiceRecordThread recordThread;
    private Exception microphoneException;
    private ImageButton doneButton, cancelButton;

    public RecordVoiceMessageScreen(Screen parent, int leftX, int fromBottomY, String target) {
        super(Component.translatable("voicemessages.recording"), parent);
        this.leftX = leftX;
        this.fromBottomY = fromBottomY;
        this.target = target;
        targetText = VoiceMessageConfirmScreen.getScreenTargetText(target);
    }

    @Override
    protected void init() {
        super.init();
        PlaybackManager.MAIN.stopPlaying();
        if (!recorded && recordThread == null) {
            recordThread = VoiceMessagesMod.getService().createVoiceRecordThread(this::appendFrame, e -> microphoneException = e);
            recordThread.startVoiceRecord();
        }
        doneButton = addRenderableWidget(new ImageButton(
                14,
                14,
                DONE_SPRITES,
                button -> {
                    stopRecording();
                    if (recorded) {
                        onClose();
                    }
                },
                CommonComponents.EMPTY
        ));
        doneButton.setTooltip(Tooltip.create(CommonComponents.GUI_DONE));
        cancelButton = addRenderableWidget(new ImageButton(
                14,
                14,
                CANCEL_SPRITES,
                button -> minecraft.setScreen(parent),
                CommonComponents.EMPTY
        ));
        cancelButton.setTooltip(Tooltip.create(CommonComponents.GUI_CANCEL));
    }

    private boolean appendFrame(short[] frame) {
        recordedFrames.add(frame);
        audioLevels.add(Playback.calculateAudioLevel(frame));
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
        audioLevels = FloatLists.unmodifiable(audioLevels);
        recorded = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (microphoneException != null) {
            VoiceMessages.getLogger().warn("Microphone error", microphoneException);
            minecraft.player.displayClientMessage(Component.translatable("voicemessages.microphone_error", microphoneException.getLocalizedMessage())
                    .withStyle(ChatFormatting.RED), true);
            minecraft.setScreen(null);
        } else if (recorded) {
            onClose();
        }
    }

    @Override
    public void onClose() {
        if (recordedFrames.isEmpty()) {
            super.onClose();
        } else {
            minecraft.setScreen(new VoiceMessageConfirmScreen(parent, leftX, fromBottomY, recordedFrames, target));
        }
    }

    @Override
    public void removed() {
        stopRecording();
        super.removed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {

    }

    @Override
    protected void actualRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 150.0F);
        int bottomY = height - fromBottomY;
        guiGraphics.fill(leftX - 1, bottomY - 16, leftX + 243, bottomY + 1, 0xFFFFFFFF);
        guiGraphics.fill(leftX, bottomY - 15, leftX + 242, bottomY, 0xFFFF5555);
        int maxFrames = VoiceMessagesClientNetworking.getMaxVoiceMessageFrames();
        float recordProgress = (float) recordedFrames.size() / maxFrames;
        PlaybackRenderer.renderPlayback(
                guiGraphics,
                leftX + 1,
                bottomY - 15,
                240,
                15,
                0xFF,
                recordProgress,
                maxFrames,
                audioLevels
        );
        int maxDuration = VoiceMessagesClientNetworking.getMaxVoiceMessageDurationMs();
        String timeStr = PlaybackPlayer.formatTime((int) (recordProgress * maxDuration))
                + '/'
                + PlaybackPlayer.formatTime(maxDuration);
        guiGraphics.drawString(font, timeStr, leftX + 247, bottomY - 12, 0xFFFFFFFF);
        int timeStrWidth = font.width(timeStr);

        doneButton.setPosition(leftX + 247 + timeStrWidth + 5, bottomY - 15);
        cancelButton.setPosition(leftX + 247 + timeStrWidth + 21, bottomY - 15);

        if (targetText != null) {
            guiGraphics.drawString(font, targetText, leftX + 247 + timeStrWidth + 40, bottomY - 12, 0xFFFFFFFF);
        }

        super.actualRender(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.pose().popPose();
    }

}

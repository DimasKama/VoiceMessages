package ru.dimaskama.voicemessages.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ru.dimaskama.voicemessages.VoiceMessages;

import static ru.dimaskama.voicemessages.config.JsonConfig.defaultedField;

public record ServerConfig(
        int maxVoiceMessageDurationMs,
        Punishment voiceMessageInvalidPunishment,
        Punishment voiceMessageSpamPunishment,
        boolean modRequired,
        String modNotInstalledText
) {

    private static final String DEFAULT_MOD_NOT_INSTALLED_TEXT = VoiceMessages.NAME + " is not installed or is of an old version";
    public static final Codec<ServerConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            defaultedField(Codec.intRange(3000, VoiceMessages.MAX_VOICE_MESSAGE_DURATION_MS), "max_voice_message_duration_ms", () -> 90000).forGetter(ServerConfig::maxVoiceMessageDurationMs),
            defaultedField(Punishment.CODEC, "voice_message_invalid_punishment", () -> Punishment.PREVENT).forGetter(ServerConfig::voiceMessageInvalidPunishment),
            defaultedField(Punishment.CODEC, "voice_message_spam_punishment", () -> Punishment.NONE).forGetter(ServerConfig::voiceMessageSpamPunishment),
            defaultedField(Codec.BOOL, "mod_required", () -> false).forGetter(ServerConfig::modRequired),
            defaultedField(Codec.STRING, "mod_not_installed_text", () -> DEFAULT_MOD_NOT_INSTALLED_TEXT).forGetter(ServerConfig::modNotInstalledText)
    ).apply(instance, ServerConfig::new));

    public ServerConfig() {
        this(90000, Punishment.PREVENT, Punishment.NONE, false, DEFAULT_MOD_NOT_INSTALLED_TEXT);
    }

}

package ru.dimaskama.voicemessages.config;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

public enum Punishment implements StringIdentifiable {

    NONE("none"),
    PREVENT("prevent"),
    KICK("kick");

    public static final Codec<Punishment> CODEC = StringIdentifiable.createBasicCodec(Punishment::values);
    private final String key;

    Punishment(String key) {
        this.key = key;
    }

    @Override
    public String asString() {
        return key;
    }

}

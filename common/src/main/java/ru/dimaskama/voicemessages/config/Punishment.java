package ru.dimaskama.voicemessages.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public enum Punishment {

    NONE("none"),
    PREVENT("prevent"),
    KICK("kick");

    public static final Codec<Punishment> CODEC = Codec.STRING.comapFlatMap(str -> {
        for (Punishment punishment : values()) {
            if (str.equalsIgnoreCase(punishment.key)) {
                return DataResult.success(punishment);
            }
        }
        return DataResult.error(() -> "Unknown punishment type: " + str);
    }, Punishment::asString);
    private final String key;

    Punishment(String key) {
        this.key = key;
    }

    public String asString() {
        return key;
    }

}

package ru.dimaskama.voicemessages.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.*;
import ru.dimaskama.voicemessages.VoiceMessages;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class JsonConfig<D> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final transient String path;
    private final Codec<D> codec;
    private final Supplier<D> defaultSupplier;
    private D data;

    public JsonConfig(String path, Codec<D> codec, Supplier<D> defaultSupplier) {
        this.path = path;
        this.codec = codec;
        this.defaultSupplier = defaultSupplier;
    }

    public String getPath() {
        return path;
    }

    public D getData() {
        if (data == null) {
            reset();
        }
        return data;
    }

    public void setData(D data) {
        this.data = data;
    }

    public void loadOrCreate() {
        File file = new File(getPath());
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (!(parent.exists() || parent.mkdirs())) {
                VoiceMessages.getLogger().warn("Can't create config: " + file.getAbsolutePath());
                return;
            }
            try {
                saveWithoutCatch();
            } catch (IOException e) {
                VoiceMessages.getLogger().warn("Exception occurred while writing new config. ", e);
            }
        } else {
            load(file);
        }
    }

    private void load(File file) {
        try (FileReader f = new FileReader(file)) {
            deserialize(JsonParser.parseReader(f));
        } catch (Exception e) {
            VoiceMessages.getLogger().warn("Exception occurred while reading config. ", e);
        }
    }

    protected void deserialize(JsonElement element) {
        data = codec.decode(JsonOps.INSTANCE, element).getOrThrow().getFirst();
    }

    public void save() {
        save(true);
    }

    public void save(boolean log) {
        try {
            saveWithoutCatch();
            if (log) VoiceMessages.getLogger().info("Config saved: " + getPath());
        } catch (IOException e) {
            VoiceMessages.getLogger().warn("Exception occurred while saving config. ", e);
        }
    }

    public void saveWithoutCatch() throws IOException {
        JsonElement json = serialize();
        try (FileWriter w = new FileWriter(getPath())) {
            GSON.toJson(json, w);
        }
    }

    protected JsonElement serialize() {
        return codec
                .encode(getData(), JsonOps.INSTANCE, JsonOps.INSTANCE.empty())
                .getOrThrow();
    }

    public void reset() {
        data = defaultSupplier.get();
    }

    public static <T> MapCodec<T> defaultedField(Codec<T> codec, String fieldName, Supplier<T> defaultSupplier) {
        MapCodec<T> delegate = codec.fieldOf(fieldName);
        return new MapCodec<>() {
            @Override
            public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
                return delegate.keys(ops);
            }

            @Override
            public <T1> DataResult<T> decode(DynamicOps<T1> ops, MapLike<T1> input) {
                if (input.get(fieldName) != null) {
                    return delegate.decode(ops, input);
                }
                return DataResult.success(defaultSupplier.get());
            }

            @Override
            public <T1> RecordBuilder<T1> encode(T input, DynamicOps<T1> ops, RecordBuilder<T1> prefix) {
                return delegate.encode(input, ops, prefix);
            }

            @Override
            public String toString() {
                return "Defaulted[" + delegate + "]";
            }
        };
    }

}
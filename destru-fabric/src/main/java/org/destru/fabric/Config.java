package org.destru.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

public class Config {
    public static final Value<Integer> RENDER_POS1_COLOR = new Value<>(new Color(0, 255, 200).getRGB());
    public static final Value<Integer> RENDER_POS2_COLOR = new Value<>(new Color(0, 200, 255).getRGB());
    public static final Value<Integer> RENDER_SECTION_COLOR = new Value<>(new Color(0, 255, 255).getRGB());
    public static final Value<Integer> RENDER_REGION_COLOR = new Value<>(new Color(0, 255, 255, 52).getRGB());
    public static final Value<Integer> RENDER_CLIPBOARD_COLOR = new Value<>(new Color(200, 255, 255, 255).getRGB());

    public static final Value<Boolean> CONTROL_ACTIVE_TOGGLE = new Value<>(false);
    public static final Value<InputConstants.Key> CONTROL_ACTIVE_KEY = new Value<>(InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_CAPSLOCK));
    public static final Value<Boolean> CONTROL_ACTIVE_PASS = new Value<>(true);
    public static final Value<InputConstants.Key> CONTROL_POS1_KEY = new Value<>(InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_LEFT));
    public static final Value<Boolean> CONTROL_POS1_PASS = new Value<>(false);
    public static final Value<InputConstants.Key> CONTROL_POS2_KEY = new Value<>(InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_RIGHT));
    public static final Value<Boolean> CONTROL_POS2_PASS = new Value<>(false);
    public static final Value<InputConstants.Key> CONTROL_CLIPBOARD_KEY = new Value<>(InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_MIDDLE));
    public static final Value<Boolean> CONTROL_CLIPBOARD_PASS = new Value<>(false);

    public static final Value<Boolean> FLAGS_PUSH_BIOME = new Value<>(false);
    public static final Value<Boolean> FLAGS_PUSH_ENTITY = new Value<>(false);
    public static final Value<Boolean> FLAGS_LOAD_BIOME = new Value<>(true);
    public static final Value<Boolean> FLAGS_LOAD_ENTITY = new Value<>(true);

    public static void load(Path path) {
        if (Files.exists(path)) {
            var properties = new Properties();
            try (var reader = Files.newBufferedReader(path)) {
                properties.load(reader);
                setValue(RENDER_POS1_COLOR, properties, "render_pos1_color", Integer::parseInt);
                setValue(RENDER_POS2_COLOR, properties, "render_pos2_color", Integer::parseInt);
                setValue(RENDER_SECTION_COLOR, properties, "render_section_color", Integer::parseInt);
                setValue(RENDER_REGION_COLOR, properties, "render_region_color", Integer::parseInt);
                setValue(CONTROL_ACTIVE_TOGGLE, properties, "control_active_toggle", Boolean::parseBoolean);
                setValue(CONTROL_ACTIVE_KEY, properties, "control_active_key", InputConstants::getKey);
                setValue(CONTROL_ACTIVE_PASS, properties, "control_active_pass", Boolean::parseBoolean);
                setValue(CONTROL_POS1_KEY, properties, "control_pos1_key", InputConstants::getKey);
                setValue(CONTROL_POS1_PASS, properties, "control_pos1_pass", Boolean::parseBoolean);
                setValue(CONTROL_POS2_KEY, properties, "control_pos2_key", InputConstants::getKey);
                setValue(CONTROL_POS2_PASS, properties, "control_pos2_pass", Boolean::parseBoolean);
                setValue(FLAGS_PUSH_BIOME, properties, "flags_push_biome", Boolean::parseBoolean);
                setValue(FLAGS_PUSH_ENTITY, properties, "flags_push_entity", Boolean::parseBoolean);
                setValue(FLAGS_LOAD_BIOME, properties, "flags_load_biome", Boolean::parseBoolean);
                setValue(FLAGS_LOAD_ENTITY, properties, "flags_load_entity", Boolean::parseBoolean);
            } catch (Exception e) {
                Destru.LOGGER.error("Failed to load config", e);
            }
        }
    }

    public static void save(Path path) {
        if (Files.notExists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                Destru.LOGGER.error("Failed to create config file", e);
            }
        }
        try (var writer = new FileWriter(path.toFile())) {
            writer.write("render_pos1_color=" + RENDER_POS1_COLOR.getValue()
                    + "\nrender_pos2_color=" + RENDER_POS2_COLOR.getValue()
                    + "\nrender_section_color=" + RENDER_SECTION_COLOR.getValue()
                    + "\nrender_region_color=" + RENDER_REGION_COLOR.getValue()
                    + "\ncontrol_active_toggle=" + CONTROL_ACTIVE_TOGGLE.getValue()
                    + "\ncontrol_active_key=" + CONTROL_ACTIVE_KEY.getValue()
                    + "\ncontrol_active_pass=" + CONTROL_ACTIVE_PASS.getValue()
                    + "\ncontrol_pos1_key=" + CONTROL_POS1_KEY.getValue()
                    + "\ncontrol_pos1_pass=" + CONTROL_POS1_PASS.getValue()
                    + "\ncontrol_pos2_key=" + CONTROL_POS2_KEY.getValue()
                    + "\ncontrol_pos2_pass=" + CONTROL_POS2_PASS.getValue()
                    + "\nflags_push_biome=" + FLAGS_PUSH_BIOME.getValue()
                    + "\nflags_push_entity=" + FLAGS_PUSH_ENTITY.getValue()
                    + "\nflags_load_biome=" + FLAGS_LOAD_BIOME.getValue()
                    + "\nflags_load_entity=" + FLAGS_LOAD_ENTITY.getValue()
            );
        } catch (IOException e) {
            Destru.LOGGER.error("Failed to save config", e);
        }
    }

    private static <T> void setValue(Value<T> value, @NotNull Properties properties, @NotNull String key, @NotNull Function<String, T> function) {
        var s = properties.getProperty(key);
        if (Objects.nonNull(s)) {
            value.setValue(function.apply(s));
        }
    }

    public static class Value<T> {
        private T value;
        private final T defaultValue;

        public Value(T value) {
            this.value = value;
            this.defaultValue = value;
        }

        public T getValue() {
            return value;
        }

        public T getDefaultValue() {
            return defaultValue;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }
}

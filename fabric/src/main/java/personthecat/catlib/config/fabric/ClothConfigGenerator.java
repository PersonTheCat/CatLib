package personthecat.catlib.config.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import personthecat.catlib.config.CategoryValue;
import personthecat.catlib.config.ConfigGenerator;
import personthecat.catlib.config.ConfigValue;
import personthecat.catlib.config.Validation;
import personthecat.catlib.config.ValidationException;
import personthecat.catlib.config.Validations;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.serialization.json.XjsUtils;
import xjs.data.Json;
import xjs.data.JsonObject;
import xjs.data.JsonValue;

import java.io.File;
import java.util.Arrays;

public class ClothConfigGenerator extends ConfigGenerator {
    private final File file;

    public ClothConfigGenerator(ModDescriptor mod, File file, CategoryValue config) {
        super(mod, config);
        this.file = file;
    }

    public void loadConfig() {
        final JsonObject json = XjsUtils.readJson(this.file).orElse(null);
        if (json == null) {
            this.error(this.config, "Could not load file from disk.");
            return;
        }
        this.loadCategory(this.config, this.instance, json);
        this.fireOnConfigUpdated();
        this.saveConfig();
    }

    private void loadCategory(CategoryValue category, Object instance, JsonObject json) {
        for (final ConfigValue value : category.values()) {
            final Object o = value.get(this.mod, instance);
            final JsonValue j = json.get(value.formattedName());
            if (j == null) {
                continue;
            }
            if (value instanceof CategoryValue c) {
                if (j.isObject()) {
                    this.loadCategory(c, o, j.asObject());
                } else {
                    this.warn(c, "Not an object. Expected category: " + j);
                }
            } else {
                try {
                    this.setValue(value, instance, j.unwrap());
                } catch (final ValidationException e) {
                    this.warn(e);
                    value.set(this.mod, instance, value.defaultValue());
                }
            }
        }
    }

    public void saveConfig() {
        XjsUtils.writeJson(this.toJson(this.config, this.instance).asObject(), this.file)
            .ifErr(e -> this.error(this.config, "Could not save value to the disk."));
    }

    private JsonValue toJson(ConfigValue value, Object instance) {
        final String comment = this.getFullComment(value);
        final Object o = value.get(this.mod, instance);
        if (value instanceof CategoryValue category) {
            final JsonObject json = Json.object();
            if (!comment.isEmpty()) {
                json.setComment(comment);
            }
            for (final ConfigValue v : category.values()) {
                json.add(v.formattedName(), toJson(v, o));
            }
            return json;
        }
        final JsonValue json = Json.any(o);
        if (!comment.isEmpty()) {
            json.setComment(comment);
        }
        return json;
    }

    private String getFullComment(ConfigValue value) {
        final Validations validations = this.getValidations(value);
        final String prefix = value.comment();
        StringBuilder comment = new StringBuilder();

        if (prefix != null) {
            comment.append(prefix);
        }
        if (validations != null) {
            final String details = Validation.buildComment(validations.values());
            if (!details.isEmpty()) {
                if (!comment.isEmpty()) {
                    comment.append('\n');
                }
                comment.append(details);
            }
        }
        if (value.type().isEnum()) {
            if (!comment.isEmpty()) {
                comment.append('\n');
            }
            final String possible = Arrays.toString(value.type().getEnumConstants());
            comment.append("Possible values: ").append(possible);
        }
        return comment.toString();
    }

    @Environment(EnvType.CLIENT)
    public Screen createScreen(Screen parent) {
        return new ClothScreenGenerator(this).buildScreen(parent);
    }
}

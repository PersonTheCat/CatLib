package personthecat.catlib.config.fabric;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;
import me.shedaniel.autoconfig.util.Utils;
import personthecat.catlib.exception.fabric.NonSerializableObjectException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DjsConfigSerializer<T extends ConfigData> implements ConfigSerializer<T> {

    private final Class<T> configClass;
    private final Path path;

    public DjsConfigSerializer(final Config definition, final Class<T> configClass) {
        this.configClass = configClass;
        this.path = Utils.getConfigFolder().resolve(definition.name() + ".djs");
    }

    @Override
    public void serialize(final T t) throws SerializationException {
        try {
            Files.createDirectories(this.path.getParent());
            DjsObjectMapper.serializeObject(this.path, t);
        } catch (final IOException | NonSerializableObjectException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public T deserialize() throws SerializationException {
        try {
            return DjsObjectMapper.deserializeObject(this.path, this.configClass);
        } catch (final RuntimeException | NonSerializableObjectException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public T createDefault() {
        return Utils.constructUnsafely(this.configClass);
    }
}

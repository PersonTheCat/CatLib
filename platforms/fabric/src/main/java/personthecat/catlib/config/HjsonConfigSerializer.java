package personthecat.catlib.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;
import me.shedaniel.autoconfig.util.Utils;
import personthecat.catlib.exception.NonSerializableObjectException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HjsonConfigSerializer<T extends ConfigData> implements ConfigSerializer<T> {

    private final Class<T> configClass;
    private final Path path;

    public HjsonConfigSerializer(final Config definition, final Class<T> configClass) {
        this.configClass = configClass;
        this.path = Utils.getConfigFolder().resolve(definition.name() + ".hjson");
    }

    @Override
    public void serialize(final T t) throws SerializationException {
        try {
            Files.createDirectories(this.path.getParent());
            HjsonObjectMapper.serializeObject(this.path, t);
        } catch (final IOException | NonSerializableObjectException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public T deserialize() throws SerializationException {
        try {
            return HjsonObjectMapper.deserializeObject(this.path, this.configClass);
        } catch (final RuntimeException | NonSerializableObjectException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public T createDefault() {
        return Utils.constructUnsafely(this.configClass);
    }
}

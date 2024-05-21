package personthecat.catlib.config.forge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.utils.FakeCommentedConfig;
import personthecat.catlib.util.LibUtil;
import xjs.data.comments.CommentType;
import xjs.data.Json;
import xjs.data.JsonObject;
import xjs.data.JsonValue;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static personthecat.catlib.serialization.json.XjsUtils.readJson;
import static personthecat.catlib.serialization.json.XjsUtils.writeJson;

/**
 * Contains the necessary procedures for handling a Forge-friendly configuration
 * based on XJS, an elegant and powerful JSON superset.
 * <p>
 *   One of the benefits of using XJS over the Forge-preferred TOML is that string arrays
 *   in XJS do not require double quotes. This provides a setup that a bit more similar
 *   to the older config formats used prior to MC 1.13.
 * </p>
 */
public class DjsFileConfig implements CommentedFileConfig  {

    /** The main file which stores all data represented by this config. */
    private final File file;

    /** The primary collection of values. */
    private final Map<String, Object> map;

    /** Comments are stored separately to be consistent with CommentedFileConfig's spec. */
    private final Map<String, String> comments;

    /** Data about this config's state for concurrency. */
    private volatile boolean writing, closed;

    /** Constructs a new instance solely from the path to this config file. */
    public DjsFileConfig(String path) {
        this(new File(path));
    }

    /** Constructs a new instance from the object representing this config file. */
    public DjsFileConfig(File file) {
        this(file, loadContainer(file));
    }

    /** Constructs a new instance from an existing JSON object. */
    public DjsFileConfig(File file, JsonObject json) {
        this(file, getContainer(file, json));
    }

    /** Constructs a new instance with data that have been previously loaded. */
    private DjsFileConfig(File file, Container container) {
        this.file = file;
        this.map = container.map;
        this.comments = container.comments;
    }

    /** Converts all of this config's data into an XJS object. */
    private JsonObject toXjs() {
        JsonObject json = new JsonObject();
        for (String key : map.keySet()) {
            final JsonValue value = toXjs(map.get(key));
            LibUtil.getOptional(comments, key).ifPresent(value::setComment);
            json.set(key, value);
        }
        return json;
    }

    /** Converts a raw value into an Xjs value. */
    private static JsonValue toXjs(Object o) {
        return o instanceof DjsFileConfig
            ? ((DjsFileConfig) o).toXjs()
            : Json.any(o);
    }

    /** Reads the json from the disk and parses its data into an ElectronWill-friendly format. */
    private static Container loadContainer(File file) {
        return getContainer(file, readJson(file).orElse(new JsonObject()));
    }

    /** Converts the input Xjs data into an ElectronWill-friendly format. */
    private static Container getContainer(File file, JsonObject json) {
        final Container container = new Container();
        for (JsonObject.Member member : json) {
            put(file, container, member.getKey(), member.getValue());
        }
        return container;
    }

    /** Puts the JsonObject's raw value and comments into the container. */
    private static void put(File file, Container container, String key, JsonValue value) {
        container.map.put(key, toRaw(file, value));
        final String comment = value.getComment(CommentType.HEADER);
        container.comments.put(key, comment != null ? comment.replace("\r", "\n") : null);
    }

    /** Converts an Xjs value into its raw counterpart. */
    private static Object toRaw(File file, JsonValue value) {
        return value.isObject()
            ? toConfig(file, value.asObject())
            : value.unwrap();
    }

    /** Converts the input Xjs object into a configuration. */
    private static DjsFileConfig toConfig(File file, JsonObject object) {
        return new DjsFileConfig(file, getContainer(file, object));
    }

    /** Returns the second to last object in the path, asserting that it is a configuration. */
    private DjsFileConfig getLastConfig(List<String> path) {
        Object val = this;
        for (int i = 0; i < path.size() - 1; i++) {
            val = ((DjsFileConfig) val).map.get(path.get(i));
        }
        return (DjsFileConfig) val;
    }

    /** Returns the last element of the input array. */
    private static String endOfPath(List<String> path) {
        return path.get(path.size() - 1);
    }

    @Override
    public String setComment(List<String> path, String comment) {
        return getLastConfig(path).comments.put(endOfPath(path), comment);
    }

    @Override
    public String removeComment(List<String> path) {
        return getLastConfig(path).comments.remove(endOfPath(path));
    }

    @Override
    public void clearComments() {
        comments.clear();
        for (Object o : map.values()) {
            if (o instanceof DjsFileConfig) {
                ((DjsFileConfig) o).clearComments();
            }
        }
    }

    @Override
    public String getComment(List<String> path) {
        return getLastConfig(path).comments.get(endOfPath(path));
    }

    @Override
    public boolean containsComment(List<String> path) {
        return getLastConfig(path).comments.containsKey(endOfPath(path));
    }

    @Override
    public Map<String, String> commentMap() {
        return comments;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T set(List<String> path, Object value) {
        return (T) getLastConfig(path).map.put(endOfPath(path), value);
    }

    @Override
    public boolean add(List<String> path, Object value) {
        return add(getLastConfig(path), endOfPath(path), value);
    }

    /** Adds a value directly to the config, if it does not already exist. */
    private static boolean add(DjsFileConfig config, String key, Object value) {
        if (!config.map.containsKey(key)) {
            config.map.put(key, value);
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T remove(List<String> path) {
        return (T) getLastConfig(path).map.remove(endOfPath(path));
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getRaw(List<String> path) {
        return (T) getLastConfig(path).map.get(endOfPath(path));
    }

    @Override
    public boolean contains(List<String> path) {
        DjsFileConfig config = this;
        final int lastIndex = path.size() - 1;
        for (int i = 0; i < lastIndex; i++) {
            final String s = path.get(i);
            if (!config.map.containsKey(s)) {
                return false;
            }
            config = (DjsFileConfig) config.map.get(s);
        }
        return config.map.containsKey(path.get(lastIndex));
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Map<String, Object> valueMap() {
        return map;
    }

    @Override
    public Set<? extends CommentedConfig.Entry> entrySet() {
        return new FakeCommentedConfig(this).entrySet();
    }

    /** No proper implementation. Doing so is still too much effort at this time. */
    @Override public ConfigFormat<CommentedFileConfig> configFormat() {
        return null;
    }

    @Override
    public CommentedConfig createSubConfig() {
        return new DjsFileConfig(file.getPath());
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public Path getNioPath() {
        return file.toPath();
    }

    @Override
    public synchronized void save() {
        if (closed) {
            throw new IllegalStateException("Cannot save a closed file config.");
        }
        writing = true;
        writeJson(toXjs(), file).expect("Error writing to config file.");
        writing = false;
    }

    @Override
    public void load() {
        if (!writing) {
            synchronized (this) {
                if (closed) {
                    throw new IllegalStateException("Cannot (re)load a closed file config");
                }
                map.clear();
                comments.clear();
                final Container container = loadContainer(file);
                map.putAll(container.map);
                comments.putAll(container.comments);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
    }

    /** A DTO holding both the value map and comments. */
    private static class Container {
        final Map<String, Object> map;
        final Map<String, String> comments;

        Container(Map<String, Object> map, Map<String, String> comments) {
            this.map = map;
            this.comments = comments;
        }

        Container() {
            this(new LinkedHashMap<>(), new HashMap<>());
        }
    }
}
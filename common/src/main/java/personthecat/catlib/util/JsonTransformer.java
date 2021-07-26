package personthecat.catlib.util;

import org.apache.commons.lang3.tuple.Pair;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static personthecat.catlib.util.Shorthand.f;

/**
 * This class contains a series of high level utilities to be used for updating old JSON presets
 * to contain the current field names and syntax standards. It is designed to be used in a builder
 * pattern and can handle renaming fields and collapsing nested objects into single objects.
 * <br><br>
 * <h3>Renaming Values:</h3>
 * <p>
 *   For example, when given the following JSON data:
 * </p>
 * <pre>
 *   a: {
 *     b: [
 *       {
 *         old: value
 *       }
 *       {
 *         other: value
 *       }
 *     ]
 *   }
 * </pre>
 * <p>
 *   And the following history:
 * </p>
 * <pre>
 *   JsonTransformer.withPath("a", "b")
 *     .history("old", "other", "new")
 *     .updateAll(json);
 * </pre>
 * <p>
 *   The object will be transformed as follows:
 * </p>
 * <pre>
 *   a: {
 *     b: [
 *       {
 *         new: value
 *       }
 *       {
 *         new: value
 *       }
 *     ]
 *   }
 * </pre>
 * <h3>Marking Fields as Removed</h3>
 * <p>
 *   For another example, when given the following JSON data:
 * </p>
 * <pre>
 *   a: {
 *     container: {
 *       removed: value
 *     }
 *   }
 *   b: {
 *     container: {
 *       removed: value
 *     }
 *   }
 * </pre>
 * <p>
 *   And the following history:
 * </p>
 * <pre>
 *   JsonTransformer.recursive("container")
 *     .markRemoved("removed", "1.0')
 *     .updateAll(json);
 * </pre>
 * <p>
 *   The object will be transformed as follows:
 * </p>
 * <pre>
 *   a: {
 *     container: {
 *       # Removed in 1.0. You can delete this field.
 *       removed: value
 *     }
 *   }
 *   b: {
 *     container: {
 *       # Removed in 1.0. You can delete this field.
 *       removed: value
 *     }
 *   }
 * </pre>
 * <h3>Author's Note:</h3>
 * <p>
 *   I am especially fond of this class. If you would like additional transformations
 *   to be supported by the library, <b>please</b> create an issue on
 *   <a href="">GitHub</a>.
 *   Thanks for your help!
 * </p>
 */
@SuppressWarnings("unused")
public class JsonTransformer {

    public static ObjectResolver withPath(final String... path) {
        return new StaticObjectResolver(path);
    }

    public static ObjectResolver recursive(final String key) {
        return new RecursiveObjectResolver(key);
    }

    public static abstract class ObjectResolver {
        private final List<Updater> updates = new LinkedList<>();

        /**
         * A series of names for any given field over time.
         * <p>
         *   For example, a field with the following history:
         * </p>
         * <ul>
         *   <li><code>name1</code></li>
         *   <li><code>name2</code></li>
         *   <li><code>name3</code></li>
         * </ul>
         * <p>
         *   Will be transformed to <code>name3</code> regardless of which name it has in
         *   the current file.
         * </p>
         * @param names A list of names for the given field over time.
         * @return This, for method chaining.
         */
        public final ObjectResolver history(final String... names) {
            updates.add(new RenameHistory(this, names));
            return this;
        }

        /**
         * Collapses the fields inside of a nested object into its parent.
         * <p>
         *   For example, given the following JSON:
         * </p>
         * <pre>
         *   outer: {
         *     inner: {
         *       a: value
         *       b: value
         *     }
         *   }
         * </pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>
         *   JsonTransformer.withPath()
         *     .collapse("outer", "inner")
         *     .updateAll(json);
         * </pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>
         *   outer: {
         *     a: value
         *     b: value
         *   }
         * </pre>
         *
         * @param outer The name of the outer object.
         * @param inner The name of the inner object.
         * @return This, for method chaining.
         */
        public final ObjectResolver collapse(final String outer, final String inner) {
            updates.add(new PathCollapseHelper(this, outer, inner));
            return this;
        }

        /**
         * Converts a JSON value in the following format:
         * <pre>
         *   minValue: 0
         *   maxValue: 1
         * </pre>
         * <p>
         *   Into an array as follows:
         * </p>
         * <pre>
         *   value: [ 0, 1 ]
         * </pre>
         * @param minKey The name of the original key for the minimum value.
         * @param minDefault The default value minimum value.
         * @param maxKey The name of the original key for the maximum value.
         * @param maxDefault The default maximum value.
         * @param newKey The new key for the combined value.
         * @return This, for method chaining.
         */
        public final ObjectResolver toRange(final String minKey, final Number minDefault, final String maxKey,
                                            final Number maxDefault, final String newKey) {
            updates.add(new RangeConverter(this, minKey, minDefault, maxKey, maxDefault, newKey));
            return this;
        }

        /**
         * Adds a comment indicating that a field has been removed. This is preferable
         * to outright removing the field, which the user may not be aware of.
         *
         * @param key The name of the field being removed.
         * @param version The version in which this field was removed.
         * @return This, for method chaining.
         */
        public final ObjectResolver markRemoved(final String key, final String version) {
            updates.add(new RemovedFieldNotifier(this, key, version));
            return this;
        }

        /**
         * Renames a value if it matches the given string.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>
         *   a: [
         *     {
         *       b: old1
         *     }
         *     {
         *       b: old2
         *     }
         *   ]
         * </pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>
         *   JsonTransformer.withPath("a")
         *     .renameValue("b", "old1", "new1")
         *     .renameValue("b", "old2", "new2")
         *     .updateAll(json);
         * </pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>
         *   a: [
         *     {
         *       b: new1
         *     }
         *     {
         *       b: new2
         *     }
         *   ]
         * </pre>
         *
         * @param key The key of the value being renamed
         * @param from The original name which has been changed
         * @param to The new name for this value.
         * @return This, for method chaining.
         */
        public final ObjectResolver renameValue(final String key, final String from, final String to) {
            updates.add(new FieldRenameHelper(this, key, from, to));
            return this;
        }

        /**
         * Applies a generic transformation with the given instructions.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>
         *   a: {
         *     old1: old2
         *   }
         * </pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>
         *   JsonTransformer.withPath("a")
         *     .transform((k, v) -> Pair.of("new1", JsonValue.valueOf("new2")))
         *     .updateAll(json)
         * </pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>
         *   a: {
         *     new1: new2
         *   }
         * </pre>
         * @param key The name of the field being transformed.
         * @param transformation A functional interface for updating the field programatically.
         * @return This, for method chaining.
         */
        public final ObjectResolver transform(final String key, final MemberTransformation transformation) {
            updates.add(new MemberTransformationHelper(this, key, transformation));
            return this;
        }

        /**
         * Runs all of the transformations defined on the given JSON object.
         *
         * @param json The JSON object target for these transformations.
         */
        public final void updateAll(final JsonObject json) {
            for (final Updater update : updates) {
                update.update(json);
            }
        }

        /**
         * Executes an operation for each last container when given a path. Each path element
         * is treated as <em>either</em> an object <em>or</em> an array.
         *
         * @param json The parent JSON file being operated on.
         * @param fn What to do for each element at <code>path[path.length - 1]</code>
         */
        public abstract void forEach(JsonObject json, Consumer<JsonObject> fn);
    }

    public static class StaticObjectResolver extends ObjectResolver {
        private final String[] path;

        private StaticObjectResolver(final String[] path) {
            this.path = path;
        }

        @Override
        public void forEach(final JsonObject json, final Consumer<JsonObject> fn) {
            forEachContainer(json, 0, fn);
        }

        private void forEachContainer(final JsonObject container, final int index, final Consumer<JsonObject> fn) {
            if (index < path.length) {
                for (JsonObject o : HjsonUtils.getRegularObjects(container, path[index])) {
                    forEachContainer(o, index + 1, fn);
                }
            } else if (index == path.length) {
                fn.accept(container);
            }
        }
    }

    public static class RecursiveObjectResolver extends ObjectResolver {
        private final String key;

        private RecursiveObjectResolver(final String key) {
            this.key = key;
        }

        @Override
        public void forEach(final JsonObject json, final Consumer<JsonObject> fn) {
            for (final JsonObject.Member member : json) {
                final JsonValue value = member.getValue();
                if (member.getName().equals(key)) {
                    HjsonUtils.getRegularObjects(json, key).forEach(fn);
                }
                if (value.isObject()) {
                    forEach(value.asObject(), fn);
                } else if (value.isArray()) {
                    forEachInArray(value.asArray(), fn);
                }
            }
        }

        private void forEachInArray(final JsonArray array, final Consumer<JsonObject> fn) {
            for (final JsonValue value : array) {
                if (value.isObject()) {
                    forEach(value.asObject(), fn);
                } else if (value.isArray()) {
                    forEachInArray(value.asArray(), fn);
                }
            }
        }
    }

    public interface Updater {
        void update(final JsonObject json);
    }

    public static class RenameHistory implements Updater {
        private final ObjectResolver resolver;
        private final String[] history;

        private RenameHistory(final ObjectResolver resolver, final String[] history) {
            this.resolver = resolver;
            this.history = history;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.forEach(json, this::renameFields);
        }

        private void renameFields(final JsonObject json) {
            final String current = history[history.length - 1];
            for (int i = 0; i < history.length - 1; i++) {
                final String key = history[i];
                final JsonValue original = json.get(key);
                if (original != null) {
                    json.set(current, original);
                    json.remove(key);
                }
            }
        }
    }

    public static class PathCollapseHelper implements Updater {
        private final ObjectResolver resolver;
        private final String outer;
        private final String inner;

        private PathCollapseHelper(final ObjectResolver resolver, final String outer, final String inner) {
            this.resolver = resolver;
            this.outer = outer;
            this.inner = inner;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.forEach(json, this::collapse);
        }

        private void collapse(final JsonObject json) {
            final JsonValue outerValue = json.get(outer);
            if (outerValue != null && outerValue.isObject()) {
                final JsonValue innerValue = outerValue.asObject().get(inner);
                if (innerValue != null) {
                    json.set(outer, innerValue);
                }
            }
        }
    }

    public static class RangeConverter implements Updater {

        private final ObjectResolver resolver;
        private final String minKey;
        private final Number minDefault;
        private final String maxKey;
        private final Number maxDefault;
        private final String newKey;

        private RangeConverter(final ObjectResolver resolver, final String minKey, final Number minDefault,
                               final String maxKey, final Number maxDefault, final String newKey) {
            this.resolver = resolver;
            this.minKey = minKey;
            this.minDefault = minDefault;
            this.maxKey = maxKey;
            this.maxDefault = maxDefault;
            this.newKey = newKey;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.forEach(json, this::convert);
        }

        private void convert(final JsonObject json) {
            if (json.has(minKey) || json.has(maxKey)) {
                if (minDefault instanceof Double || minDefault instanceof Float) {
                    final float min = HjsonUtils.getFloat(json, minKey).orElse(minDefault.floatValue());
                    final float max = HjsonUtils.getFloat(json, maxKey).orElse(maxDefault.floatValue());
                    json.set(newKey, getRange(min, max));
                } else {
                    final int min = HjsonUtils.getInt(json, minKey).orElse(minDefault.intValue());
                    final int max = HjsonUtils.getInt(json, maxKey).orElse(maxDefault.intValue());
                    json.set(newKey, getRange(min, max));
                }
                json.remove(minKey);
                json.remove(maxKey);
            }
        }

        private JsonValue getRange(final float min, final float max) {
            if (min == max) {
                return JsonValue.valueOf(min);
            }
            return new JsonArray().add(min).add(max).setCondensed(true);
        }

        private JsonValue getRange(final int min, final int max) {
            if (min == max) {
                return JsonValue.valueOf(min);
            }
            return new JsonArray().add(min).add(max).setCondensed(true);
        }
    }

    public static class RemovedFieldNotifier implements Updater {
        private final ObjectResolver resolver;
        private final String key;
        private final String version;

        private RemovedFieldNotifier(final ObjectResolver resolver, final String key, final String version) {
            this.resolver = resolver;
            this.key = key;
            this.version = version;
        }

        @Override
        public void update(JsonObject json) {
            resolver.forEach(json, this::markRemoved);
        }

        private void markRemoved(JsonObject json) {
            final JsonValue value = json.get(key);
            if (value != null) {
                json.setCondensed(false);
                value.setEOLComment(f("Removed in {}. You can delete this field.", version));
            }
        }
    }

    public static class FieldRenameHelper implements Updater {
        private final ObjectResolver resolver;
        private final String key;
        private final String from;
        private final String to;

        private FieldRenameHelper(final ObjectResolver resolver, final String key, final String from, final String to) {
            this.resolver = resolver;
            this.key = key;
            this.from = from;
            this.to = to;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.forEach(json, this::renameValue);
        }

        private void renameValue(final JsonObject json) {
            final JsonValue value = json.get(key);
            if (value != null && value.isString() && from.equalsIgnoreCase(value.asString())) {
                json.set(key, to);
            }
        }
    }

    @FunctionalInterface
    public interface MemberTransformation {
        Pair<String, JsonValue> transform(final String name, final JsonValue value);
    }

    public static class MemberTransformationHelper implements Updater {
        private final ObjectResolver resolver;
        private final String key;
        private final MemberTransformation transformation;

        private MemberTransformationHelper(final ObjectResolver resolver, final String key,
                                           final MemberTransformation transformation) {
            this.resolver = resolver;
            this.key = key;
            this.transformation = transformation;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.forEach(json, this::transform);
        }

        private void transform(final JsonObject json) {
            final JsonValue value = json.get(key);
            if (value != null) {
                final Pair<String, JsonValue> updated = transformation.transform(key, value);
                json.remove(key);
                json.set(updated.getKey(), updated.getValue());
            }
        }
    }

}

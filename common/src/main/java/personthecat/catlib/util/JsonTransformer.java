package personthecat.catlib.util;

import org.apache.commons.lang3.tuple.Pair;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
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
 * <pre>{@code
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
 * }</pre>
 * <p>
 *   And the following history:
 * </p>
 * <pre>{@code
 *   JsonTransformer.withPath("a", "b")
 *     .history("old", "other", "new")
 *     .updateAll(json);
 * }</pre>
 * <p>
 *   The object will be transformed as follows:
 * </p>
 * <pre>{@code
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
 * }</pre>
 * <h3>Marking Fields as Removed</h3>
 * <p>
 *   For another example, when given the following JSON data:
 * </p>
 * <pre>{@code
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
 * }</pre>
 * <p>
 *   And the following history:
 * </p>
 * <pre>{@code
 *   JsonTransformer.recursive("container")
 *     .markRemoved("removed", "1.0')
 *     .updateAll(json);
 * }</pre>
 * <p>
 *   The object will be transformed as follows:
 * </p>
 * <pre>{@code
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
 * }</pre>
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
         *
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
         * <pre>{@code
         *   outer: {
         *     inner: {
         *       a: value
         *       b: value
         *     }
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.withPath()
         *     .collapse("outer", "inner")
         *     .updateAll(json);
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   outer: {
         *     a: value
         *     b: value
         *   }
         * }</pre>
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
         * <pre>{@code
         *   minValue: 0
         *   maxValue: 1
         * }</pre>
         * <p>
         *   Into an array as follows:
         * </p>
         * <pre>{@code
         *   value: [ 0, 1 ]
         * }</pre>
         *
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
         * <pre>{@code
         *   a: [
         *     {
         *       b: old1
         *     }
         *     {
         *       b: old2
         *     }
         *   ]
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.withPath("a")
         *     .renameValue("b", "old1", "new1")
         *     .renameValue("b", "old2", "new2")
         *     .updateAll(json);
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   a: [
         *     {
         *       b: new1
         *     }
         *     {
         *       b: new2
         *     }
         *   ]
         * }</pre>
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
         * <pre>{@code
         *   a: {
         *     old1: old2
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.withPath("a")
         *     .transform((k, v) -> Pair.of("new1", JsonValue.valueOf("new2")))
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   a: {
         *     new1: new2
         *   }
         * }</pre>
         *
         * @param key The name of the field being transformed.
         * @param transformation A functional interface for updating the field programatically.
         * @return This, for method chaining.
         */
        public final ObjectResolver transform(final String key, final MemberTransformation transformation) {
            updates.add(new MemberTransformationHelper(this, key, transformation));
            return this;
        }

        /**
         * Exposes the parent object and value in the presence of a given key. This
         * allows for manual transformation of various JSON members.
         * <p>
         *   For example, when given the following JSON data.
         * </p>
         * <pre>{@code
         *   a: {
         *     k1: v1
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.withPath("a")
         *     .ifPresent("k1", (j, v) -> j.add("k2", v))
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   a: {
         *     k1: v1
         *     k2: v1
         *   }
         * }</pre>
         *
         * @param key The name of the field being transformed.
         * @param f An event to fire for manual transformations in the presence of <code>key</code>.
         * @return Thisd, for method chaining.
         */
        public final ObjectResolver ifPresent(final String key, final BiConsumer<JsonObject, JsonValue> f) {
            updates.add(new MemberPredicateHelper(this, key, f));
            return this;
        }

        /**
         * Variant of {@link #ifPresent(String, BiConsumer)} which ignores the present value.
         * <p>
         *   For example, when given the following JSON data.
         * </p>
         * <pre>{@code
         *   a: {
         *     k1: v1
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.withPath("a")
         *     .ifPresent("k1", j -> j.add("k2", "v2"))
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   a: {
         *     k1: v1
         *     k2: v2
         *   }
         * }</pre>
         *
         * @param key The name of the field being transformed.
         * @param f An event to fire for manual transformations in the presence of <code>key</code>.
         * @return This, for method chaining.
         */
        public final ObjectResolver ifPresent(final String key, final Consumer<JsonObject> f) {
            updates.add(new MemberPredicateHelper(this, key, (j, v) -> f.accept(j)));
            return this;
        }

        /**
         * Transfers the contents of an expected array, if present, into a different array,
         * regardless of whether it is present.
         * <p>
         *   For example, when given the following JSON data.
         * </p>
         * <pre>{@code
         *   a: {
         *     a1: [ 1, 2, 3 ]
         *     o1: [ 4, 5, 6 ]
         *     o2: [ 7, 8, 9 ]
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.withPath("a")
         *     .moveArray("o1", "a1")
         *     .moveArray("o2", "a2")
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   a: {
         *     a1: [ 1, 2, 3, 4, 5, 6 ]
         *     a2: [ 7, 8, 9 ]
         *   }
         * }</pre>
         *
         * @param from The name of the source array.
         * @param to The name of the destination array.
         * @return This,
         */
        public final ObjectResolver moveArray(final String from, final String to) {
            updates.add(new ArrayCopyHelper(this, from, to));
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

    public static class MemberPredicateHelper implements Updater {
        private final ObjectResolver resolver;
        private final String key;
        private final BiConsumer<JsonObject, JsonValue> ifPresent;

        private MemberPredicateHelper(final ObjectResolver resolver, final String key,
                                      final BiConsumer<JsonObject, JsonValue> ifPresent) {
            this.resolver = resolver;
            this.key = key;
            this.ifPresent = ifPresent;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.forEach(json, this::test);
        }

        private void test(final JsonObject json) {
            final JsonValue value = json.get(key);
            if (value != null) {
                ifPresent.accept(json, value);
            }
        }
    }

    public static class ArrayCopyHelper implements Updater {
        private final ObjectResolver resolver;
        private final String from;
        private final String to;

        private ArrayCopyHelper(final ObjectResolver resolver, final String from, final String to) {
            this.resolver = resolver;
            this.from = from;
            this.to = to;
        }

        @Override
        public void update(JsonObject json) {
            resolver.forEach(json, this::copy);
        }

        private void copy(final JsonObject json) {
            final JsonValue source = json.get(from);
            if (source != null) {
                final JsonArray array = HjsonUtils.getArrayOrNew(json, to);
                for (final JsonValue value : HjsonUtils.asOrToArray(source)) {
                    array.add(value);
                }
                json.remove(from);
            }
        }
    }

}

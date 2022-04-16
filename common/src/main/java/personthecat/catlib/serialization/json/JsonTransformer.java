package personthecat.catlib.serialization.json;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import xjs.core.*;
import xjs.transform.JsonCollectors;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
 *   <a href="https://github.com/PersonTheCat/CatLib/issues">GitHub</a>.
 *   Thanks for your help!
 * </p>
 */
@SuppressWarnings("unused")
public class JsonTransformer {

    private JsonTransformer() {}

    /**
     * This resolver is intended for all transformations that occur directly on the object
     * being passed into the transformer. Its {@link RootObjectResolver#forEach forEach}
     * method is non-recursive and designed for transformers that house additional nested
     * object updates.
     * <p>
     *   The following code is an assertion of this behavior:
     * </p>
     * <pre>{@code
     *   final JsonObject json = parse("{inner:{}}");
     *   final List<JsonObject> resolved = JsonTransformer.root().collect(json);
     *
     *   assert resolved != null;
     *   assert resolved.size == 1;
     *   assert resolved.get(0) == json;
     * }</pre>
     *
     * @return A new root object resolver to house any global JSON transformations.
     */
    public static ObjectResolver root() {
        return new RootObjectResolver();
    }

    /**
     * This resolver is intended for transforming <b>all</b> available objects in a given
     * JSON object.
     * <p>
     *   The following code is an assertion of this behavior:
     * </p>
     * <pre>{@code
     *   final JsonObject json = parse("{k1:{},k2:{k3:{}}}");
     *   final List<JsonObject> resolved = JsonTransformer.all().collect(json);
     *
     *   assert resolved != null;
     *   assert resolved.size == 4;
     * }</pre>
     *
     * @return A new object resolver to house transformations on all possible objects.
     */
    public static ObjectResolver all() {
        return new MatchingObjectResolver(null, (k, o) -> true);
    }

    /**
     * This resolver is intended for any constant object paths nested within the root
     * JSON object. It accepts an array of keys which refer <b>objects</b> by name.
     * It is capable of resolving all objects that are nested arbitrarily within arrays.
     * <p>
     *   The following code is an assertion of this behavior:
     * </p>
     * <pre>{@code
     *   final JsonObject json = parse("{inner:[[[{}],{},{}],{},{}]}");
     *   final List<JsonObject> resolved = JsonTransformer.withPath("inner").collect(json);
     *
     *   assert resolved != null;
     *   assert resolved.size() == 5;
     * }</pre>
     *
     * @param path Every key leading up to the objects being transformed.
     * @return A new static object resolver for transformations on this path.
     */
    public static ObjectResolver withPath(final String... path) {
        return new StaticObjectResolver(path);
    }

    /**
     * This resolver is intended for any objects whatsoever that are paired with the given
     * key. It can resolve data at any arbitrary depth nested within the root object.
     * <p>
     *   The following code is an assertion of this behavior:
     * </p>
     * <pre>{@code
     *   final JsonObject json = parse("{a:{x:{x:{}}},b:{x:[{},{}]}}");
     *   final List<JsonObject> resolved = JsonTransformer.scan("x").collect(json);
     *
     *   assert resolved != null;
     *   assert resolved.size() == 4;
     * }</pre>
     *
     * <p><b>Note</b>: This method will be replaced with <code>global</code> in CatLib 2.0.
     * Expect its behavior to change slightly in the future.
     *
     * @param key The name of every object being resolved.
     * @return A new matching object resolver for all transformations of this kind.
     */
    public static ObjectResolver scan(final String key) {
        return new MatchingObjectResolver(null, (k, o) -> key.equals(k));
    }

    /**
     * This resolver is intended for any objects containing the given key.
     * <p>
     *   The following code is an assertion of this behavior:
     * </p>
     * <pre>{@code
     *   final JsonObject json = parse("a:{b:{},c:{}}");
     *   final List<JsonObject> resolved = JsonTransformer.containing("b").collect();
     *
     *   assert resolved != null;
     *   assert resolved.size() == 1;
     *   assert resolved.get(0).has("c");
     * }</pre>
     * @param key The key which must be present.
     * @return A new matching object resolver for all transformations of this kind.
     */
    public static ObjectResolver containing(final String key) {
        return new MatchingObjectResolver(null, (k, o) -> o.has(key));
    }

    /**
     * This resolver is intended for any objects containing the given key, provided
     * that its value matches the given predicate.
     * <p>
     *   The following code is an assertion of this behavior:
     * </p>
     * <pre>{@code
     *   final JsonObject json = parse("a:[{b:1},{b:''}]");
     *   final List<JsonObject> resolved = JsonTransformer.containing("b", JsonValue::isNumber).collect();
     *
     *   assert resolved != null;
     *   assert resolved.size() == 1;
     *   assert resolved.get(0).get("b").asInt() == 1;
     * }</pre>
     *
     * @param key The key which must be present.
     * @param predicate The predicate for matching the value of this key.
     * @return A new matching object resolver for all transformations of this kind.
     */
    public static ObjectResolver containing(final String key, final Predicate<JsonValue> predicate) {
        return new MatchingObjectResolver(null, (k, o) -> {
            final JsonValue value = o.get(key);
            return value != null && predicate.test(value);
        });
    }

    /**
     * This resolver is intended for matching any condition at all when given a JSON object.
     * <p>
     *   The following code is an assertion of this behavior:
     * </p>
     * <pre>{@code
     *   final JsonObject json = parse("a:[{b:1,c:2},{d:4,e:5}]");
     *   final List<JsonObject> resolved = JsonTransformer.matching(null, o -> o.has("c") && o.has("d")).collect();
     *
     *   assert resolved != null;
     *   assert resolved.size() == 1;
     *   assert resolved.get(0).get("d").asInt() == 4;
     *   assert resolved.get(0).get("e").asInt() == 5;
     * }</pre>
     *
     * @param defaultKey An optional key to be used as the root key.
     * @param predicate The condition which any given object must match.
     * @return A new matching object resolver for all transformations of this kind.
     */
    public static ObjectResolver matching(final @Nullable String defaultKey, final Predicate<JsonObject> predicate) {
        return new MatchingObjectResolver(defaultKey, (k, o) -> predicate.test(o));
    }

    /**
     * This resolver is intended for matching any condition at all when given a JSON object.
     * <p>
     *   The following code is an assertion of this behavior:
     * </p>
     * <pre>{@code
     *   final JsonObject json = parse("a:[{b:1,c:2},{d:4}]");
     *   final List<JsonObject> resolved =
     *     JsonTransformer.matching(null, (k, o) -> "a".equals(k) && o.size() == 2).collect();
     *
     *   assert resolved != null;
     *   assert resolved.size() == 1;
     *   assert resolved.get(0).get("b").asInt() == 1;
     *   assert resolved.get(0).get("c").asInt() == 2;
     * }</pre>
     *
     * @param defaultKey An optional key to be used as the root key.
     * @param predicate The condition which any given object must match.
     * @return A new matching object resolver for all transformations of this kind.
     */
    public static ObjectResolver matching(final @Nullable String defaultKey, final ObjectMemberPredicate predicate) {
        return new MatchingObjectResolver(defaultKey, predicate);
    }

    /**
     * Variant of {@link JsonTransformer#matching(String, Predicate)} with no explicit
     * parameter for <code>defaultKey</code>.
     *
     * @param predicate The condition which any given object must match.
     * @return A new matching object resolver for all transformations of this kind.
     */
    public static ObjectResolver matching(final Predicate<JsonObject> predicate) {
        return new MatchingObjectResolver(null, (k, o) -> predicate.test(o));
    }

    /**
     * Variant of {@link JsonTransformer#matching(String, ObjectMemberPredicate)} with no
     * explicit parameter for <code>defaultKey</code>.
     *
     * @param predicate The condition which any given object must match.
     * @return A new matching object resolver for all transformations of this kind.
     */
    public static ObjectResolver matching(final ObjectMemberPredicate predicate) {
        return new MatchingObjectResolver(null, predicate);
    }

    public static abstract class ObjectResolver {
        private final List<Updater> updates;

        private ObjectResolver() {
            this.updates = new LinkedList<>();
        }

        private ObjectResolver(final List<Updater> updates) {
            this.updates = updates;
        }

        /**
         * Bundles an additional transformer into this object. All of its updates will
         * be applied in the order in which they are provided.
         * <p>
         *   The following code demonstrates the expected use case for this:
         * </p>
         * <pre>{@code
         *   static final ObjectResolver OBJECT_A =
         *     JsonTransformer.withPath("a")
         *       .history("x", "y", "z")
         *       .freeze();
         *
         *   static final ObjectResolver OBJECT_B =
         *     JsonTransformer.withPath("b")
         *       .history("1", "2", "3")
         *       .freeze();
         *
         *   static final ObjectResolver TRANSFORMER =
         *     JsonTransformer.root()
         *       .include(OBJECT_A)
         *       .include(OBJECT_B)
         *       .freeze();
         *
         *   public static void transform(final JsonObject json) {
         *     TRANSFORMER.updateAll(json);
         *   }
         * }</pre>
         *
         * @param transformer The nested transformer to include
         * @return This, for method chaining.
         */
        public final ObjectResolver include(final ObjectResolver transformer) {
            updates.add(new NestedTransformer(transformer));
            return this;
        }

        /**
         * Bundles an additional transformer at the given path. This is intended for nesting
         * additional transformations while preserving reuse of the original transformations
         * and allowing them to be applied directly to the nested objects.
         * <p>
         *   The following code demonstrates the expected use case for this:
         * </p>
         * <pre>{@code
         *   static final ObjectResolver OBJECT_A =
         *     JsonTransformer.root()
         *       .history("x", "y", "z")
         *       .freeze();
         *
         *   static final ObjectResolver OBJECT_B =
         *     JsonTransformer.root()
         *       .history("1", "2", "3")
         *       .freeze();
         *
         *   static final ObjectResolver TRANSFORMER =
         *     JsonTransformer.root()
         *       .include("a", OBJECT_A)
         *       .include("b", OBJECT_B)
         *       .freeze();
         *
         *   public static void transform(final JsonObject json) {
         *     TRANSFORMER.updateAll(json);
         *   }
         * }</pre>
         *
         * @param path The path to the object being transformed.
         * @param transformer The nested transformer to include
         * @return This, for method chaining.
         */
        public final ObjectResolver include(final String path, final ObjectResolver transformer) {
            updates.add(new NestedTransformer(withPath(path).include(transformer)));
            return this;
        }

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
         * Collapses the fields from a nested object into its parent.
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
         *   JsonTransformer.root()
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
         * @param transformation A functional interface for updating the field programmatically.
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
         *   For example, when given the following JSON data:
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
         * @return This, for method chaining.
         */
        public final ObjectResolver ifPresent(final String key, final BiConsumer<JsonObject, JsonValue> f) {
            updates.add(new MemberPredicateHelper(this, key, f));
            return this;
        }

        /**
         * Variant of {@link #ifPresent(String, BiConsumer)} which ignores the present value.
         * <p>
         *   For example, when given the following JSON data:
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
         *   For example, when given the following JSON data:
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
         * @return This, for method chaining.
         */
        public final ObjectResolver moveArray(final String from, final String to) {
            updates.add(new ArrayCopyHelper(this, from, to));
            return this;
        }

        /**
         * Relocates fields to entirely different object paths. This transformer is <b>only valid for
         * regular object paths.</b> If any arrays are present in the given path, the first object in
         * the flattened array path will be used and <b>any other objects will be silently ignored</b>.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>{@code
         *   {
         *     path: {
         *       to: {
         *         value: 24
         *       }
         *     }
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.root()
         *     .relocate("path.to.value", "whole.new.path")
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   {
         *     whole: {
         *       new: {
         *         path: 24
         *       }
         *     }
         *   }
         * }</pre>
         * <p>
         *   Note that when any object arrays are present on the <code>to</code> path, only the
         *   first object in the array will copied into. <b>The remaining data will be ignored and
         *   the following transformation will occur</b>:
         * </p>
         * <pre>{@code
         *   {
         *     path: {
         *       to: {
         *         value: 24
         *       }
         *     }
         *     whole: {
         *       new: [
         *         {
         *           other: 48
         *         }
         *         {
         *           other: 49
         *         }
         *       ]
         *     }
         *   }
         * }</pre>
         * <p>
         *   Will be transformed into:
         * </p>
         * <pre>{@code
         *   {
         *     whole: {
         *       new: [
         *         {
         *           other: 48
         *           path: 24
         *         }
         *         {
         *           other: 49
         *         }
         *       ]
         *     }
         *   }
         * }</pre>
         * <p>
         *   Note that when any object arrays are present on the <code>from</code> path, only the
         *   first object in the array will be copied out of. <b>This is considered an error condition
         *   and the remaining data will be left behind</b>.
         * </p>
         *
         * @param from The fully-qualified, dotted path to the original value.
         * @param to The fully-qualified, dotted path to the new value.
         * @return This, for method, chaining.
         */
        public final ObjectResolver relocate(final String from, final String to) {
            return relocate(from, to, true);
        }

        /**
         * Variant of {@link #relocate(String, String)} which is designed to cover the condition where
         * an object or array is being moved and another container already exists at the destination.
         * Setting the third parameter (<code>merge</code>) to <code>true</code> will merge containers
         * instead of overwriting them.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>{@code
         *   {
         *     simple: {
         *       path: {
         *         a: 1
         *         b: 2
         *       }
         *     }
         *     other: {
         *       path: {
         *         c: 3
         *       }
         *     }
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.root()
         *     .relocate("simple.path", "other.path", true)
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   {
         *     other: {
         *       path: {
         *         a: 1
         *         b: 2
         *         c: 3
         *       }
         *     }
         *   }
         * }</pre>
         *
         * @param from The fully-qualified, dotted path to the original value.
         * @param to The fully-qualified, dotted path to the new value.
         * @param merge Whether to write into any existing containers at the end of the path.
         * @return This, for method, chaining.
         */
        public final ObjectResolver relocate(final String from, final String to, final boolean merge) {
            updates.add(new FieldRelocator(this, from, to, merge));
            return this;
        }

        /**
         * Places the fields in every matching object into a specific order. Any fields
         * not matched by the set will simply be rendered at the end.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>{@code
         *   {
         *     a: 1
         *     first: 9
         *     b: 2
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.root()
         *     .reorder(singleton("first")
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   {
         *     first: 9
         *     a: 1
         *     b: 2
         *   }
         * }</pre>
         *
         * @param keys The exact order in which to place the fields.
         * @return This, for method chaining.
         */
        public final ObjectResolver reorder(final Collection<String> keys) {
            return this.reorder(keys, Collections.emptyList());
        }

        /**
         * Places the fields in a strict, specific order. This method accepts two sets of
         * keys, where the first places any matching fields at the top and the second
         * places any matching fields at the bottom.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>{@code
         *   {
         *     a: 1
         *     first: 8
         *     b: 2
         *     last: 9
         *     c: 3
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.root()
         *     .reorder(singleton("first"), singleton("last"))
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   {
         *     first: 8
         *     a: 1
         *     b: 2
         *     c: 3
         *     last: 9
         *   }
         * }</pre>
         *
         * @param first The keys to display at the beginning of the object.
         * @param last The keys to display at the end of the object.
         * @return This, for method chaining.
         */
        public final ObjectResolver reorder(final Collection<String> first, final Collection<String> last) {
            updates.add(new StrictFieldOrganizer(this, first, last));
            return this;
        }

        /**
         * Sorts every field in the matching objects in alphabetical order.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>{@code
         *   {
         *     b: 2
         *     c: 3
         *     a: 1
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.root()
         *     .sort()
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   {
         *     a: 1
         *     b: 2
         *     c: 3
         *   }
         * }</pre>
         *
         * @return This, for method chaining.
         */
        public final ObjectResolver sort() {
            return this.sort(Comparator.comparing(JsonObject.Member::getKey));
        }

        /**
         * Sorts every field in the matching objects when given a specific comparator
         * to determine their order.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>{@code
         *   {
         *     a: 1
         *     b: 2
         *     c: 3
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.root()
         *     .sort((m1, m2) -> m2.getName().compareTo(m1.getName()))
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   {
         *     c: 3
         *     b: 2
         *     a: 1
         *   }
         * }</pre>
         *
         * @param comparator A comparator to determine field order.
         * @return This, for method chaining.
         */
        public final ObjectResolver sort(final Comparator<JsonObject.Member> comparator) {
            updates.add(new FieldSorter(this, comparator));
            return this;
        }

        /**
         * Recursively provides default values in the matching objects. Existing values will
         * not be replaced by this transformer.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>{@code
         *   {
         *     a: 0
         *     c: 4
         *     n: {}
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.root()
         *     .setDefaults(parse("a:1,b:2,c:3,n:{k:9}"))
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   {
         *     a: 0
         *     b: 2
         *     c: 4
         *     n: {
         *       k: 9
         *     }
         *   }
         * }</pre>
         *
         * @param defaults An object containing the default values.
         * @return This, for method chaining.
         */
        public final ObjectResolver setDefaults(final JsonObject defaults) {
            updates.add(new DefaultFieldProvider(this, defaults));
            return this;
        }

        /**
         * Removes any single key from the matching objects.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>{@code
         *   {
         *     a: 1
         *     b: 2
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.root()
         *     .remove("a")
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   {
         *     b: 2
         *   }
         * }</pre>
         *
         * @param key The name of the field being removed.
         * @return This, for method chaining.
         */
        public final ObjectResolver remove(final String key) {
            return this.remove(key, null);
        }

        /**
         * Removes a single key-value pair from the matching objects. For this transformer
         * to remove a field, the value of the field must match <code>value</code>.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>{@code
         *   {
         *     a: 1
         *     b: 2
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.root()
         *     .remove("a", 1)
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   {
         *     b: 2
         *   }
         * }</pre>
         * <p>
         *   However, <b>the field will not be removed if the value does not match</b>.
         * </p>
         *
         * @param key The name of the field being removed.
         * @param value The expected value being removed.
         * @return This, for method chaining.
         */
        public final ObjectResolver remove(final String key, final Object value) {
            return this.remove(key, Json.any(value));
        }

        /**
         * Removes a set of key-value pairs from the matching object. An ideal use case for this
         * transformer would be whenever a default JSON object is available. The default object
         * could be used as a mask to remove unnecessary fields.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>{@code
         *   {
         *     a: 1
         *     b: 2
         *     c: 3
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.root()
         *     .remove(parse("a:1,b:1,c:1"))
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   {
         *     b: 2
         *     c: 3
         *   }
         * }</pre>
         *
         * @param json A set of key-value pairs being removed.
         * @return This, for method chaining.
         */
        public final ObjectResolver remove(final JsonObject json) {
            for (final JsonObject.Member m : json) {
                this.remove(m.getKey(), m.getValue());
            }
            return this;
        }

        /**
         * Removes a single key-value pair from the matching objects. This method is a variant
         * of {@link #remove(String, Object)} accepting a {@link JsonValue} directly.
         * <p>
         *   For example, when given the following JSON data:
         * </p>
         * <pre>{@code
         *   {
         *     a: 1
         *     b: 2
         *   }
         * }</pre>
         * <p>
         *   And the following history:
         * </p>
         * <pre>{@code
         *   JsonTransformer.root()
         *     .remove("a", JsonValue.valueOf(1))
         *     .updateAll(json)
         * }</pre>
         * <p>
         *   The object will be transformed as follows:
         * </p>
         * <pre>{@code
         *   {
         *     b: 2
         *   }
         * }</pre>
         * <p>
         *   However, <b>the field will not be removed if the value does not match</b>.
         * </p>
         *
         * @param key The name of the field being removed.
         * @param value The expected value being removed.
         * @return This, for method chaining.
         */
        public final ObjectResolver remove(final String key, @Nullable final JsonValue value) {
            updates.add(new FieldRemover(this, key, value));
            return this;
        }

        /**
         * Runs the given consumer for each matching object, when available.
         *
         * @param fn The function to execute for each matching object.
         * @return This, for method chaining.
         */
        public final ObjectResolver run(final Consumer<JsonObject> fn) {
            updates.add(new SimpleFunctionRunner(this, fn));
            return this;
        }

        /**
         * Prevents any further changes to this resolver. Use this to clearly indicate the
         * purpose of any static json transformer and guarantee thread safety.
         *
         * @return A new object resolver which cannot be mutated.
         */
        public final ObjectResolver freeze() {
            return new FrozenObjectResolver(this);
        }

        /**
         * Runs every transformation defined on the given JSON object.
         *
         * @param json The JSON object target for these transformations.
         */
        public final void updateAll(final JsonObject json) {
            for (final Updater update : updates) {
                update.update(json);
            }
        }

        /**
         * Runs every transformation on multiple JSON objects in order.
         *
         * @param objects The JSON object targets for these transformations.
         */
        public final void updateAll(final JsonObject... objects) {
            for (final JsonObject json : objects) {
                this.updateAll(json);
            }
        }

        /**
         * Runs every transformation without modifying the given object. Instead, a new value
         * will be returned.
         *
         * @param json The JSON object source for this operation.
         * @return A <em>new</em> JSON object with the provided transforms applied.
         */
        public final JsonObject getUpdated(final JsonObject json) {
            final JsonObject clone = json.deepCopy().asObject();
            this.updateAll(clone);
            return clone;
        }

        /**
         * Runs every transformation on multiple JSON objects without modifying <b>any</b>
         * source object.
         *
         * @param objects The JSON object sources for this operation.
         * @return An array of <em>new</em> JSON objects with the provided transforms applied.
         */
        public final JsonObject[] getUpdated(final JsonObject... objects) {
            final JsonObject[] clones = new JsonObject[objects.length];
            for (int i = 0; i < objects.length; i++) {
                clones[i] = this.getUpdated(objects[i]);
            }
            return clones;
        }

        /**
         * Executes an operation for each last container when given a path. Each path element
         * is treated as <em>either</em> an object <em>or</em> an array.
         *
         * @param json The parent JSON file being operated on.
         * @param fn What to do for each element at <code>path[path.length - 1]</code>
         */
        public abstract void forEach(JsonObject json, Consumer<JsonObject> fn);

        /**
         * Collects every matching object within the given root into an array.
         *
         * @param json The root JSON object containing the expected data.
         * @return A list of every matching object.
         */
        public final List<JsonObject> collect(final JsonObject json) {
            return this.collect(json, new ArrayList<>());
        }

        /**
         * Collects every matching object within the given root into a collection of any type.
         *
         * @param json The root JSON object containing the expected data.
         * @param collection The collection being written into.
         * @param <T> The type of collection being written into.
         * @return A collection of every matching object.
         */
        public final <T extends Collection<JsonObject>> T collect(final JsonObject json, final T collection) {
            this.forEach(json, collection::add);
            return collection;
        }
    }

    public static class RootObjectResolver extends ObjectResolver {

        private RootObjectResolver() {}

        @Override
        public void forEach(final JsonObject json, final Consumer<JsonObject> fn) {
            fn.accept(json);
        }
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
                for (JsonObject o : XjsUtils.getRegularObjects(container, path[index])) {
                    forEachContainer(o, index + 1, fn);
                }
            } else if (index == path.length) {
                fn.accept(container);
            }
        }
    }

    public interface ObjectMemberPredicate {
        boolean test(final @Nullable String key, final JsonObject json);
    }

    public static class MatchingObjectResolver extends ObjectResolver {
        private final @Nullable String defaultKey;
        private final ObjectMemberPredicate predicate;

        private MatchingObjectResolver(final @Nullable String defaultKey, final ObjectMemberPredicate predicate) {
            this.defaultKey = defaultKey;
            this.predicate = predicate;
        }

        @Override
        public void forEach(final JsonObject json, final Consumer<JsonObject> fn) {
            this.forEachInObject(this.defaultKey, json, fn);
        }

        private void forEachInObject(final String key, final JsonObject json, final Consumer<JsonObject> fn) {
            if (this.predicate.test(key, json)) {
                fn.accept(json);
            }
            for (final JsonObject.Member member : json) {
                final String name = member.getKey();
                final JsonValue value = member.getValue();
                if (value.isObject()) {
                    forEachInObject(name, value.asObject(), fn);
                } else if (value.isArray()) {
                    forEachInArray(name, value.asArray(), fn);
                }
            }
        }

        private void forEachInArray(final String key, final JsonArray array, final Consumer<JsonObject> fn) {
            for (final JsonValue value : array) {
                if (value.isObject()) {
                    forEachInObject(key, value.asObject(), fn);
                } else if (value.isArray()) {
                    forEachInArray(key, value.asArray(), fn);
                }
            }
        }
    }

    public static class FrozenObjectResolver extends ObjectResolver {
        private final ObjectResolver wrapped;

        private FrozenObjectResolver(final ObjectResolver wrapped) {
            super(Collections.unmodifiableList(wrapped.updates));
            this.wrapped = wrapped;
        }

        @Override
        public void forEach(final JsonObject json, final Consumer<JsonObject> fn) {
            wrapped.forEach(json, fn);
        }
    }

    public interface Updater {
        void update(final JsonObject json);
    }

    public static class NestedTransformer implements Updater {
        private final ObjectResolver resolver;

        private NestedTransformer(final ObjectResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.updateAll(json);
        }
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
                    final float min = json.getOptional(minKey, JsonValue::asFloat).orElse(minDefault.floatValue());
                    final float max = json.getOptional(maxKey, JsonValue::asFloat).orElse(maxDefault.floatValue());
                    json.set(newKey, getRange(min, max));
                } else {
                    final int min = json.getOptional(minKey, JsonValue::asInt).orElse(minDefault.intValue());
                    final int max = json.getOptional(maxKey, JsonValue::asInt).orElse(maxDefault.intValue());
                    json.set(newKey, getRange(min, max));
                }
                json.remove(minKey);
                json.remove(maxKey);
            }
        }

        private JsonValue getRange(final float min, final float max) {
            if (min == max) {
                return Json.value(min);
            }
            return new JsonArray().add(min).add(max).condense();
        }

        private JsonValue getRange(final int min, final int max) {
            if (min == max) {
                return Json.value(min);
            }
            return new JsonArray().add(min).add(max).condense();
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
                json.setLineLength(1);
                value.setComment(CommentType.EOL, f("Removed in {}. You can delete this field.", version));
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
                final JsonArray array = XjsUtils.getOrCreateArray(json, to);
                for (final JsonValue value : source.intoArray()) {
                    array.add(value);
                }
                json.remove(from);
            }
        }
    }

    public static class FieldRelocator implements Updater {

        private final ObjectResolver resolver;
        private final String[] from;
        private final String[] to;
        private final boolean merge;

        private FieldRelocator(final ObjectResolver resolver, final String from, final String to, final boolean merge) {
            this.resolver = resolver;
            this.from = from.split("\\.");
            this.to = to.split("\\.");
            this.merge = merge;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.forEach(json, this::relocate);
        }

        private void relocate(final JsonObject json) {
            final JsonValue toMove = this.resolve(json);
            if (toMove == null) return;

            for (final JsonObject container : this.getContainers(json)) {
                final String key = to[to.length - 1];
                final JsonValue get = container.get(key);
                if (get == null) {
                    container.add(key, toMove);
                } else if (merge) {
                    if (get.isObject() && toMove.isObject()) {
                        toMove.asObject().forEach(m -> get.asObject().set(m.getKey(), m.getValue()));
                    } else if (get.isArray() && toMove.isArray()) {
                        toMove.asArray().forEach(v -> get.asArray().add(v));
                    } else {
                        container.set(key, toMove);
                    }
                } else {
                    container.set(key, toMove);
                }
            }
        }

        @Nullable
        private JsonValue resolve(final JsonObject json) {
            JsonObject parent = json;
            for (int i = 0; i < from.length - 1; i++) {
                // Only use the first object. All others are ambiguous.
                final JsonValue next = parent.get(from[i]);
                if (next == null) return null;
                final JsonObject object = this.getFirstObject(next);
                if (object == null) return null;
                parent = object;
            }
            final String key = from[from.length - 1];
            final JsonValue get = parent.get(key);
            if (get != null) parent.remove(key);
            return get;
        }

        @Nullable
        private JsonObject getFirstObject(final JsonValue value) {
            if (value.isObject()) {
                return value.asObject();
            } else if (value.isArray()) {
                final JsonArray array = value.asArray();
                if (!array.isEmpty()) {
                    return getFirstObject(array.get(0));
                }
            }
            return null;
        }

        private List<JsonObject> getContainers(final JsonObject json) {
            final List<JsonObject> containers = new ArrayList<>();
            this.addContainers(containers, json, 0);
            return containers;
        }

        private void addContainers(final List<JsonObject> containers, final JsonObject root, final int index) {
            if (index < to.length - 1) {
                final String key = to[index];
                final JsonValue value = root.get(key);

                if (value != null && value.isObject()) {
                    this.addContainers(containers, value.asObject(), index + 1);
                } else if (value != null && value.isArray()) {
                    this.addToArray(containers, value.asArray(), index);
                } else {
                    final JsonObject object = new JsonObject();
                    root.add(key, object);
                    this.addContainers(containers, object, index + 1);
                }
            } else if (index == to.length - 1) {
                containers.add(root);
            }
        }

        private void addToArray(final List<JsonObject> containers, final JsonArray array, final int index) {
            if (array.isEmpty()) {
                final JsonObject object = new JsonObject();
                array.add(object);
                this.addContainers(containers, object, index + 1);
            } else {
                for (final JsonValue value : array) {
                    if (value != null && value.isObject()) {
                        this.addContainers(containers, value.asObject(), index + 1);
                    } else if (value != null && value.isArray()) {
                        this.addToArray(containers, value.asArray(), index);
                    }
                }
            }
        }
    }

    public static class StrictFieldOrganizer implements Updater {

        final ObjectResolver resolver;
        final Collection<String> first;
        final Collection<String> last;

        private StrictFieldOrganizer(final ObjectResolver resolver, final Collection<String> first, final Collection<String> last) {
            this.resolver = resolver;
            this.first = first;
            this.last = last;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.forEach(json, this::reorder);
        }

        private void reorder(final JsonObject json) {
            final JsonObject clone = new JsonObject().addAll(json);
            final JsonObject firstValues = drain(clone, first);
            final JsonObject lastValues = drain(clone, last);
            json.clear();

            json.addAll(firstValues);
            json.addAll(clone);
            json.addAll(lastValues);
        }

        private static JsonObject drain(final JsonObject source, final Collection<String> keys) {
            final JsonObject drain = new JsonObject();
            for (final String key : keys) {
                final JsonValue value = source.get(key);
                if (value != null) {
                    drain.add(key, value);
                    source.remove(key);
                }
            }
            return drain;
        }
    }

    public static class FieldSorter implements Updater {

        final ObjectResolver resolver;
        final Comparator<JsonObject.Member> comparator;

        private FieldSorter(final ObjectResolver resolver, final Comparator<JsonObject.Member> comparator) {
            this.resolver = resolver;
            this.comparator = comparator;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.forEach(json, this::sort);
        }

        private void sort(final JsonObject json) {
            final JsonObject sorted = json.stream().sorted(comparator).collect(JsonCollectors.member());
            json.clear().addAll(sorted);
        }
    }

    public static class DefaultFieldProvider implements Updater {

        final ObjectResolver resolver;
        final JsonObject defaults;

        private DefaultFieldProvider(final ObjectResolver resolver, final JsonObject defaults) {
            this.resolver = resolver;
            this.defaults = defaults;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.forEach(json, this::setDefaults);
        }

        private void setDefaults(final JsonObject json) {
            json.setDefaults(this.defaults);
        }
    }

    public static class FieldRemover implements Updater {

        final ObjectResolver resolver;
        final String key;
        @Nullable final JsonValue value;

        private FieldRemover(final ObjectResolver resolver, final String key, @Nullable final JsonValue value) {
            this.resolver = resolver;
            this.key = key;
            this.value = value;
        }

        @Override
        public void update(final JsonObject json) {
            resolver.forEach(json, this::remove);
        }

        private void remove(final JsonObject json) {
            if (this.value == null || this.value.equals(json.get(this.key))) {
                json.remove(this.key);
            }
        }
    }

    public static class SimpleFunctionRunner implements Updater {

        final ObjectResolver resolver;
        final Consumer<JsonObject> fn;

        private SimpleFunctionRunner(final ObjectResolver resolver, final Consumer<JsonObject> fn) {
            this.resolver = resolver;
            this.fn = fn;
        }

        @Override
        public void update (final JsonObject json) {
            resolver.forEach(json, fn);
        }
    }

}

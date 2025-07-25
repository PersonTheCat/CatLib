package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.RegistryHandle;

import java.util.List;
import java.util.Set;

/**
 * Matches any number of {@link ResourceLocation ids} when given a {@link RegistryHandle
 * registry handle}. This Type is designed to be expressible in one of 2 ways:
 *
 * <ul>
 *   <li>
 *     A named list of {@link ResourceLocation ids}; for example:
 *      <pre>{@code
 *        names: [ "dirt", "quark:crafter" ]
 *      }</pre>
 *   </li>
 *   <li>
 *     A heterogeneous list of prefixed matchers; for example:
 *     <pre>{@code
 *       [ "#is_forest", "plains", "@byg" ]
 *     }</pre>
 *   </li>
 * </ul>
 */
public interface IdMatcher<T> {

    /**
     * A list of all matcher types which should appear in every list.
     */
    List<Info<?>> DEFAULT_TYPES = List.of(Id.INFO, Tag.INFO, Mod.INFO);

    /**
     * Appends any possible ids to {@code out}.
     *
     * @param handle The source registry providing IDs and values
     * @param out    The set of locations being appended to
     */
    void add(final RegistryHandle<T> handle, final Set<ResourceKey<T>> out);

    /**
     * Gets a description of this matcher, providing details for serialization.
     *
     * @return The description
     */
    Info<? extends IdMatcher<?>> info();

    /**
     * Convenience method to create invertible entries from matchers.
     *
     * @param invert Whether the result is an inverted entry
     * @return @ new {@link InvertibleEntry}
     */
    default InvertibleEntry<T> entry(final boolean invert) {
        return new InvertibleEntry<>(invert, this);
    }

    /**
     * Convenience method for constructing a new {@link Id} entry.
     *
     * @param invert Whether to invert this matcher
     * @param id     The id being matched
     * @return A new {@link InvertibleEntry}
     */
    static <T> InvertibleEntry<T> id(final boolean invert, final ResourceKey<T> id) {
        return new Id<>(id).entry(invert);
    }

    /**
     * Convenience method for constructing a new {@link Mod} entry.
     *
     * @param invert Whether to invert this matcher
     * @param name   The mod being matched
     * @return A new {@link InvertibleEntry}
     */
    static <T> InvertibleEntry<T> mod(final boolean invert, final String name) {
        return new Mod<T>(name).entry(invert);
    }

    /**
     * Convenience method for constructing a new {@link Tag} entry.
     *
     * @param invert Whether to invert this matcher
     * @param tag    The tag being matched
     * @return A new {@link InvertibleEntry}
     */
    static <T> InvertibleEntry<T> tag(final boolean invert, final TagKey<T> tag) {
        return new Tag<>(tag).entry(invert);
    }

    /**
     * Describes the expected use of this matcher, as well as means for creating it
     * statically.
     *
     * @param <M> The corresponding type of {@link IdMatcher matcher}
     */
    interface Info<M extends IdMatcher<?>> {

        /**
         * Gets the field name expected to be used in the object format
         *
         * @return The name
         */
        String fieldName();

        /**
         * Gets the codec for an {@link InvertibleEntry} containing this type of matcher.
         *
         * @return The codec
         */
        <T> Codec<InvertibleEntry<T>> codec(final ResourceKey<Registry<T>> key);

        /**
         * Gets an optional codec for an {@link InvertibleEntry} which uses a string prefix
         * to create the value, if applicable.
         *
         * @return The codec
         */
        default <T> @Nullable Codec<InvertibleEntry<T>> prefixedCodec(final ResourceKey<Registry<T>> key) {
            return null;
        }

        /**
         * Indicates whether this type of matcher can appear in a list.
         *
         * @return <code>true</code>, if it may appear in a list
         */
        default boolean canBeListed() {
            return false;
        }
    }

    /**
     * String representable variant of {@link Info} providing convenient codecs.
     *
     * @param <M> The corresponding type of {@link IdMatcher matcher}
     */
    interface StringRepresentable<M extends IdMatcher<?>> extends Info<M> {

        /**
         * Gets a textual representation of this value with no prefix or other
         * affixed symbols.
         *
         * @param m The instance being serialized
         * @return The textual representation
         */
        String valueOf(final M m);

        /**
         * Constructs a new matcher with no prefix or other affixed symbols.
         *
         * @param s The textual representation of this value.
         * @return The matcher
         */
        <T> DataResult<InvertibleEntry<T>> newFromString(
                final ResourceKey<Registry<T>> key, final boolean invert, final String s);

        /**
         * If applicable, gets the prefix to use in the list format.
         *
         * @return The prefix, or else <code>null</code>.
         */
        default @Nullable String prefix() {
            return null;
        }

        @Override
        default <T> Codec<InvertibleEntry<T>> codec(final ResourceKey<Registry<T>> key) {
            return Codec.STRING.comapFlatMap(
                s -> InvertibleEntry.fromString(key, s, this),
                e -> e.stringify(this, false));
        }

        @Override
        default <T> Codec<InvertibleEntry<T>> prefixedCodec(final ResourceKey<Registry<T>> key) {
            if (this.prefix() == null) return null;
            return Codec.STRING.comapFlatMap(
                s -> InvertibleEntry.fromString(key, s, this),
                e -> e.stringify(this, true));
        }

        @Override
        default boolean canBeListed() {
            return this.prefix() != null;
        }
    }

    /**
     * An invertible type of {@link IdMatcher matcher}. This entry selectively
     * writes to either a white or blacklist.
     */
    record InvertibleEntry<T>(boolean invert, IdMatcher<T> matcher) {

        /**
         * Variant of {@link IdMatcher#add} which may either write to a white or blacklist.
         *
         * @param handle The source registry providing IDs and values
         * @param white  The white set of locations being appended to
         * @param black  The black set of locations being appended to
         */
        void add(RegistryHandle<T> handle, Set<ResourceKey<T>> white, Set<ResourceKey<T>> black) {
            this.matcher.add(handle, this.invert ? black : white);
        }

        static <T> DataResult<InvertibleEntry<T>> fromString(
                ResourceKey<Registry<T>> key, String s, final StringRepresentable<?> info) {
            boolean invert = false;
            if (s.startsWith("!")) {
                invert = true;
                s = s.substring(1);
            }
            final String prefix = info.prefix();
            if (prefix != null && s.startsWith(prefix)) {
                s = s.substring(prefix.length());
            }
            return info.newFromString(key, invert, s);
        }

        @SuppressWarnings("unchecked")
        <M extends IdMatcher<?>> String stringify(final StringRepresentable<M> info, final boolean needsPrefix) {
            final StringBuilder sb = new StringBuilder();
            if (this.invert) {
                sb.append('!');
            }
            if (needsPrefix) {
                final String prefix = info.prefix();
                if (prefix == null) throw new IllegalArgumentException("prefix == null");
                sb.append(prefix);
            }
            sb.append(info.valueOf((M) this.matcher));
            return sb.toString();
        }

        static <T> Codec<InvertibleEntry<T>> nonInvertibleCodec(final Codec<IdMatcher<T>> codec) {
            return codec.xmap(m -> m.entry(false), e -> e.matcher);
        }
    }

    record Id<T>(ResourceKey<T> id) implements IdMatcher<T> {
        public static final Info<Id<?>> INFO = new StringRepresentable<>() {
            @Override
            public String fieldName() {
                return "names";
            }

            @Override
            public String valueOf(final Id<?> id) {
                return id.id.location().toString();
            }

            @Override
            public <U> DataResult<InvertibleEntry<U>> newFromString(
                    final ResourceKey<Registry<U>> key, final boolean invert, final String s) {
                return ResourceLocation.read(s).map(id -> IdMatcher.id(invert, ResourceKey.create(key, id)));
            }

            @Override
            public String prefix() {
                return "";
            }

            @Override
            public String toString() {
                return "ID";
            }
        };

        @Override
        public void add(final RegistryHandle<T> handle, final Set<ResourceKey<T>> out) {
            if (handle.isRegistered(this.id)) {
                out.add(this.id);
            }
        }

        @Override
        public Info<Id<?>> info() {
            return INFO;
        }
    }

    record Tag<T>(TagKey<T> id) implements IdMatcher<T> {
        public static final Info<Tag<?>> INFO = new StringRepresentable<>() {
            @Override
            public String fieldName() {
                return "tags";
            }

            @Override
            public String valueOf(final Tag<?> tag) {
                return tag.id.toString();
            }

            @Override
            public <U> DataResult<InvertibleEntry<U>> newFromString(
                    final ResourceKey<Registry<U>> key, final boolean invert, final String s) {
                return ResourceLocation.read(s).map(id -> IdMatcher.tag(invert, TagKey.create(key, id)));
            }

            @Override
            public String prefix() {
                return "#";
            }

            @Override
            public String toString() {
                return "TAG";
            }
        };

        @Override
        public void add(final RegistryHandle<T> handle, final Set<ResourceKey<T>> out) {
            final HolderSet.Named<T> tag = handle.getNamed(this.id);
            if (tag != null) {
                tag.forEach(holder -> out.add(handle.keyOf(holder)));
            }
        }

        @Override
        public Info<Tag<?>> info() {
            return INFO;
        }
    }

    record Mod<T>(String id) implements IdMatcher<T> {
        public static final Info<Mod<?>> INFO = new StringRepresentable<>() {
            @Override
            public String fieldName() {
                return "mods";
            }

            @Override
            public String valueOf(final Mod<?> mod) {
                return mod.id;
            }

            @Override
            public <U> DataResult<InvertibleEntry<U>> newFromString(
                    final ResourceKey<Registry<U>> key, final boolean invert, final String s) {
                return DataResult.success(IdMatcher.mod(invert, s));
            }

            @Override
            public String prefix() {
                return "@";
            }

            @Override
            public String toString() {
                return "MOD";
            }
        };

        @Override
        public void add(final RegistryHandle<T> handle, final Set<ResourceKey<T>> out) {
            handle.streamEntries()
                .filter(e -> this.id.equals(e.getKey().location().getNamespace()))
                .forEach(e -> out.add(e.getKey()));
        }

        @Override
        public Info<Mod<?>> info() {
            return INFO;
        }
    }
}
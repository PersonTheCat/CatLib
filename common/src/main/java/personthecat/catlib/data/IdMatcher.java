package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
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
public interface IdMatcher {

    /**
     * A list of all matcher types which should appear in every list.
     */
    List<Info<?>> DEFAULT_TYPES = List.of(Id.INFO, Tag.INFO, Mod.INFO);

    /**
     * Appends any possible ids to {@code out}.
     *
     * @param handle The source registry providing IDs and values
     * @param out    The set of locations being appended to
     * @param <T>    The type of value in the registry
     */
    <T> void add(final RegistryHandle<T> handle, final Set<ResourceLocation> out);

    /**
     * Gets a description of this matcher, providing details for serialization.
     *
     * @return The description
     */
    Info<? extends IdMatcher> info();

    /**
     * Convenience method for constructing a new {@link Id} entry.
     *
     * @param invert Whether to invert this matcher
     * @param id     The id being matched
     * @return A new {@link InvertibleEntry}
     */
    static InvertibleEntry id(final boolean invert, final ResourceLocation id) {
        return new InvertibleEntry(invert, new Id(id));
    }

    /**
     * Convenience method for constructing a new {@link Mod} entry.
     *
     * @param invert Whether to invert this matcher
     * @param name   The mod being matched
     * @return A new {@link InvertibleEntry}
     */
    static InvertibleEntry mod(final boolean invert, final String name) {
        return new InvertibleEntry(invert, new Mod(name));
    }

    /**
     * Convenience method for constructing a new {@link Tag} entry.
     *
     * @param invert Whether to invert this matcher
     * @param tag    The tag being matched
     * @return A new {@link InvertibleEntry}
     */
    static InvertibleEntry tag(final boolean invert, final ResourceLocation tag) {
        return new InvertibleEntry(invert, new Tag(tag));
    }

    /**
     * Describes the expected use of this matcher, as well as means for creating it
     * statically.
     *
     * @param <M> The corresponding type of {@link IdMatcher matcher}
     */
    interface Info<M extends IdMatcher> {

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
        Codec<InvertibleEntry> codec();

        /**
         * Gets an optional codec for an {@link InvertibleEntry} which uses a string prefix
         * to create the value, if applicable.
         *
         * @return The codec
         */
        default @Nullable Codec<InvertibleEntry> prefixedCodec() {
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

        /**
         * Generates a nonnull variant of the given codec, printing this for info.
         *
         * @param codec The codec being wrapped
         * @param <T>   The type of data being serialized
         * @return A nonnull version of the given codec
         */
        default <T> Codec<T> nonNullCodec(final Codec<T> codec) {
            return codec.flatXmap(this::nonNullValue, this::nonNullValue);
        }

        /**
         * Filters the value based on whether it is null.
         *
         * @param t   The value being filtered
         * @param <T> The type of value being filtered
         * @return Success if nonnull, error if null
         */
        default <T> DataResult<T> nonNullValue(final T t) {
            return t != null ? DataResult.success(t) : DataResult.error(() -> this + " not found");
        }
    }

    /**
     * String representable variant of {@link Info} providing convenient codecs.
     *
     * @param <M> The corresponding type of {@link IdMatcher matcher}
     */
    interface StringRepresentable<M extends IdMatcher> extends Info<M> {

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
        @Nullable M newFromString(final String s);

        /**
         * If applicable, gets the prefix to use in the list format.
         *
         * @return The prefix, or else <code>null</code>.
         */
        default @Nullable String prefix() {
            return null;
        }

        @Override
        default Codec<InvertibleEntry> codec() {
            return this.nonNullCodec(Codec.STRING.xmap(
                s -> InvertibleEntry.fromString(s, this),
                e -> e.stringify(this, false)));
        }

        @Override
        default Codec<InvertibleEntry> prefixedCodec() {
            if (this.prefix() == null) return null;
            return this.nonNullCodec(Codec.STRING.xmap(
                s -> InvertibleEntry.fromString(s, this),
                e -> e.stringify(this, true)));
        }

        @Override
        default boolean canBeListed() {
            return this.prefix() != null;
        }
    }

    /**
     * A version of {@link IdMatcher} which only tolerates one specific type of registry.
     *
     * @param <T> The type of registry compatible with this matcher.
     */
    interface Typed<T> extends IdMatcher {

        /**
         * Gets a key representing the type of registry compatible with this matcher.
         *
         * @return The key
         */
        ResourceKey<? extends Registry<T>> type();

        /**
         * Override ensuring that the correct type of registry is provided.
         *
         * @param handle The source registry providing IDs and values
         * @param out    The set of locations being appended to
         * @param <U>    The type of value in the registry
         */
        @Override
        @SuppressWarnings("unchecked")
        default <U> void add(final RegistryHandle<U> handle, final Set<ResourceLocation> out) {
            if (!this.type().equals(handle.key())) {
                throw new UnsupportedOperationException("Illegal handle for matcher: " + handle.key());
            }
            this.addTyped((RegistryHandle<T>) handle, out);
        }

        /**
         * Checked variant of {@link #add}.
         *
         * @param handle The source registry providing IDs and values
         * @param out    The set of locations being appended to
         */
        void addTyped(final RegistryHandle<T> handle, final Set<ResourceLocation> out);
    }

    /**
     * An invertible type of {@link IdMatcher matcher}. This entry selectively
     * writes to either a white or blacklist.
     */
    record InvertibleEntry(boolean invert, IdMatcher matcher) {

        /**
         * Variant of {@link IdMatcher#add} which may either write to a white or blacklist.
         *
         * @param handle The source registry providing IDs and values
         * @param white  The white set of locations being appended to
         * @param black  The black set of locations being appended to
         * @param <T>    The type of value in the registry
         */
        <T> void add(RegistryHandle<T> handle, Set<ResourceLocation> white, Set<ResourceLocation> black) {
            this.matcher.add(handle, this.invert ? black : white);
        }

        static @Nullable InvertibleEntry fromString(String s, final StringRepresentable<?> info) {
            boolean invert = false;
            if (s.startsWith("!")) {
                invert = true;
                s = s.substring(1);
            }
            final String prefix = info.prefix();
            if (prefix != null && s.startsWith(prefix)) {
                s = s.substring(prefix.length());
            }
            final IdMatcher fromString = info.newFromString(s);
            if (fromString == null) return null;
            return new InvertibleEntry(invert, fromString);
        }

        @SuppressWarnings("unchecked")
        <M extends IdMatcher> String stringify(final StringRepresentable<M> info, final boolean needsPrefix) {
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

        static Codec<InvertibleEntry> nonInvertibleCodec(final Codec<IdMatcher> codec) {
            return codec.xmap(m -> new InvertibleEntry(false, m), e -> e.matcher);
        }
    }

    record Id(ResourceLocation id) implements IdMatcher {
        public static final Info<Id> INFO = new StringRepresentable<>() {
            @Override
            public String fieldName() {
                return "names";
            }

            @Override
            public String valueOf(final Id id) {
                return id.id.toString();
            }

            @Override
            public Id newFromString(final String s) {
                return new Id(new ResourceLocation(s));
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
        public <T> void add(final RegistryHandle<T> handle, final Set<ResourceLocation> out) {
            if (handle.isRegistered(this.id)) {
                out.add(this.id);
            }
        }

        @Override
        public Info<Id> info() {
            return INFO;
        }
    }

    record Tag(ResourceLocation id) implements IdMatcher {
        public static final Info<Tag> INFO = new StringRepresentable<>() {
            @Override
            public String fieldName() {
                return "tags";
            }

            @Override
            public String valueOf(final Tag tag) {
                return tag.id.toString();
            }

            @Override
            public Tag newFromString(final String s) {
                return new Tag(new ResourceLocation(s));
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
        public <T> void add(final RegistryHandle<T> handle, final Set<ResourceLocation> out) {
            final ResourceKey<? extends Registry<T>> key = handle.key();
            if (key == null) return;
            final HolderSet.Named<T> tag = handle.getNamed(TagKey.create(key, this.id));
            if (tag == null) return;
            tag.forEach(holder -> out.add(idOf(handle, holder)));
        }

        private static <T> ResourceLocation idOf(final RegistryHandle<T> handle, final Holder<T> holder) {
            return holder.unwrap().map(ResourceKey::location, handle::getKey);
        }

        @Override
        public Info<Tag> info() {
            return INFO;
        }
    }

    record Mod(String id) implements IdMatcher {
        public static final Info<Mod> INFO = new StringRepresentable<>() {
            @Override
            public String fieldName() {
                return "mods";
            }

            @Override
            public String valueOf(final Mod mod) {
                return mod.id;
            }

            @Override
            public Mod newFromString(final String s) {
                return new Mod(s);
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
        public <T> void add(final RegistryHandle<T> handle, final Set<ResourceLocation> out) {
            handle.streamKeys().filter(id -> this.id.equals(id.getNamespace())).forEach(out::add);
        }

        @Override
        public Info<Mod> info() {
            return INFO;
        }
    }
}
package personthecat.catlib.serialization.codec.capture;

import org.jetbrains.annotations.Nullable;

public record Key<T>(@Nullable Key<?> qualifier, String name, Class<T> type) {
    static final String ANY = "$__any__";

    public static <T> Key<T> of(String key, T t) {
        return of(key, inferType(t));
    }

    @SafeVarargs
    public static <T> Key<T> of(String key, T... implicitType) {
        return of(key, inferType(implicitType));
    }

    public static <T> Key<T> of(String key, Class<T> type) {
        return of(null, key, type);
    }

    public static <T> Key<T> of(@Nullable Key<?> qualifier, String key, Class<T> type) {
        return new Key<>(qualifier, key, type);
    }

    public Key<T> qualified(Key<?> qualifier) {
        return of(qualifier, this.name, this.type);
    }

    public Key<T> unqualified() {
        return of(this.name, this.type);
    }

    public <R> Key<R> as(Class<R> type) {
        return of(this.qualifier, this.name, type);
    }

    Key<T> asAny() {
        return of(ANY, this.type);
    }

    public boolean isQualified() {
        return this.qualifier != null;
    }

    public String qualifiedName() {
        return this.qualifier != null ? this.qualifier.qualifiedName() + "." + this.name : this.name;
    }

    @Override
    public String toString() {
        return this.type.getSimpleName() + " " + this.qualifiedName();
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> inferType(T t) {
        return (Class<T>) t.getClass();
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> Class<T> inferType(T... implicitType) {
        return (Class<T>) implicitType.getClass().getComponentType();
    }
}

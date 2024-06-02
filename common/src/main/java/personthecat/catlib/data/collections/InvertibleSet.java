package personthecat.catlib.data.collections;

import java.util.Set;

public record InvertibleSet<T>(Set<T> entries, boolean blacklist) {}

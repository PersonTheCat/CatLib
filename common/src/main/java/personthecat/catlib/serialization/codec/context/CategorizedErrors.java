package personthecat.catlib.serialization.codec.context;

import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.collections.MultiValueHashMap;
import personthecat.catlib.data.collections.MultiValueMap;

import java.util.List;

public record CategorizedErrors(@Nullable String category, MultiValueMap<List<Either<String, Integer>>, String> erredLeaves) {
    public CategorizedErrors(@Nullable String category) {
        this(category, new MultiValueHashMap<>());
    }
}

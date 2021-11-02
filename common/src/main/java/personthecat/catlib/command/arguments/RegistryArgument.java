package personthecat.catlib.command.arguments;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.data.Lazy;
import personthecat.catlib.event.registry.CommonRegistries;
import personthecat.catlib.event.registry.RegistryHandle;
import personthecat.catlib.util.LibReference;
import personthecat.catlib.util.RegistryUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static personthecat.catlib.exception.Exceptions.cmdSyntax;

public class RegistryArgument<T> implements ArgumentType<T> {

    public static void register() {
        ArgumentTypes.register(LibReference.MOD_ID + ":registry_argument", RegistryArgument.class,
            new EmptyArgumentSerializer<>(() -> new RegistryArgument<>(CommonRegistries.BLOCKS)));
    }

    private static final Map<Class<?>, RegistryArgument<?>> ARGUMENTS_BY_TYPE = new ConcurrentHashMap<>();

    private final RegistryHandle<T> handle;
    private final Lazy<List<String>> suggestions;

    public RegistryArgument(final RegistryHandle<T> handle) {
        this.handle = handle;
        this.suggestions = Lazy.of(() -> computeSuggestions(handle));
    }

    @SuppressWarnings("unchecked")
    public static <T> RegistryArgument<T> getOrThrow(final Class<T> clazz) {
        return (RegistryArgument<T>) ARGUMENTS_BY_TYPE.computeIfAbsent(clazz, c ->
            new RegistryArgument<>(RegistryUtils.getByType(c)));
    }

    @Override
    public T parse(final StringReader reader) throws CommandSyntaxException {
        final ResourceLocation id = new ResourceLocation(reader.readString());
        if (!this.handle.isRegistered(id)) {
            throw cmdSyntax(reader, "Feature not found");
        }
        return this.handle.lookup(new ResourceLocation(reader.readString()));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(this.suggestions.get(), builder);
    }

    private static List<String> computeSuggestions(final RegistryHandle<?> handle) {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        handle.forEach((id, value) -> {
            if ("minecraft".equals(id.getNamespace())) builder.add(id.getPath());
            builder.add(id.toString());
        });
        return builder.build();
    }
}

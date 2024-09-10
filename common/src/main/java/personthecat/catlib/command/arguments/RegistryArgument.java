package personthecat.catlib.command.arguments;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ResettableLazy;
import personthecat.catlib.exception.MissingElementException;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.registry.RegistryHandle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryArgument<T> implements ArgumentType<T> {
    private static final Map<Class<?>, RegistryArgument<?>> ARGUMENTS_BY_TYPE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, RegistryArgument<?>> ARGUMENTS_BY_ID = new ConcurrentHashMap<>();
    public static final ArgumentTypeInfo<RegistryArgument<?>, Info.Template> INFO = new Info();
    private static final Dynamic2CommandExceptionType NOT_IN_REGISTRY =
        new Dynamic2CommandExceptionType((id, r) -> Component.translatable("catlib.errorText.notInRegistry", id, r));

    private final RegistryHandle<T> handle;
    private final ResettableLazy<List<String>> suggestions;

    private RegistryArgument(final RegistryHandle<T> handle) {
        this.handle = handle;
        this.suggestions = ResettableLazy.of(() -> computeSuggestions(handle));
        DynamicRegistries.listen(this.handle, this).accept(updated -> this.suggestions.reset());
    }

    public static <T> @Nullable RegistryArgument<T> lookup(final Class<T> type) {
        final RegistryArgument<?> ra = ARGUMENTS_BY_TYPE.get(type);
        if (ra != null) return cast(ra);
        final RegistryHandle<T> handle = DynamicRegistries.lookup(type);
        if (handle == null) return null;
        return constructAndRegister(handle, type);
    }

    public static <T> RegistryArgument<T> getOrThrow(final Class<T> type) {
        final RegistryArgument<?> ra = ARGUMENTS_BY_TYPE.get(type);
        return ra != null ? cast(ra) : constructAndRegister(DynamicRegistries.getByType(type), type);
    }

    public static <T> RegistryArgument<T> getOrThrow(final ResourceKey<? extends Registry<T>> key) {
        final RegistryArgument<?> ra = ARGUMENTS_BY_ID.get(key.location());
        return ra != null ? cast(ra) : constructAndRegister(DynamicRegistries.getOrThrow(key), null);
    }

    private static <T> RegistryArgument<T> constructAndRegister(final RegistryHandle<T> handle, @Nullable Class<T> type) {
        if (type == null) {
            if (handle.isEmpty()) {
                throw new MissingElementException("Cannot construct registry argument (element type unknown): " + handle.key());
            }
            type = cast(handle.iterator().next().getClass());
        }
        final ResourceKey<? extends Registry<T>> key = handle.key();
        if (key == null) {
            throw new MissingElementException("Cannot construct registry argument (key unknown): " + type.getSimpleName());
        }
        final RegistryArgument<T> ra = new RegistryArgument<>(handle);
        ARGUMENTS_BY_ID.put(key.location(), ra);
        ARGUMENTS_BY_TYPE.put(type, ra);
        return ra;
    }

    @SuppressWarnings("unchecked")
    private static <T> RegistryArgument<T> cast(final RegistryArgument<?> ra) {
        return (RegistryArgument<T>) ra;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> cast(final Class<?> type) {
        return (Class<T>) type;
    }

    @Override
    public T parse(final StringReader reader) throws CommandSyntaxException {
        final ResourceLocation id = ResourceLocation.read(reader);
        if (!this.handle.isRegistered(id)) {
            final ResourceKey<?> key = this.handle.key();
            final ResourceLocation registryId = key != null ? key.location() : null;
            throw NOT_IN_REGISTRY.createWithContext(reader, id, registryId);
        }
        return this.handle.lookup(id);
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

    private static class Info implements ArgumentTypeInfo<RegistryArgument<?>, Info.Template> {

        @Override
        public void serializeToNetwork(final Template template, final FriendlyByteBuf buf) {
            buf.writeResourceLocation(template.id);
        }

        @Override
        public @NotNull Template deserializeFromNetwork(final FriendlyByteBuf buf) {
            return new Template(buf.readResourceLocation());
        }

        @Override
        public @NotNull Template unpack(final RegistryArgument ra) {
            return new Template(Objects.requireNonNull(ra.handle.key()).location());
        }

        @Override
        public void serializeToJson(final Template template, final JsonObject json) {
            json.addProperty("registry", template.id.toString());
        }

        public class Template implements ArgumentTypeInfo.Template<RegistryArgument<?>> {
            private final ResourceLocation id;

            private Template(final ResourceLocation id) {
                this.id = id;
            }

            @Override
            public @NotNull RegistryArgument<?> instantiate(final CommandBuildContext ctx) {
                return getOrThrow(ResourceKey.createRegistryKey(this.id));
            }

            @Override
            public @NotNull Info type() {
                return Info.this;
            }
        }
    }
}

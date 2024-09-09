package personthecat.catlib.command.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.Exceptions;
import personthecat.catlib.util.LibUtil;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class EnumArgument<E extends Enum<E>> implements ArgumentType<E> {
    public static final ArgumentTypeInfo<EnumArgument<?>, Info.Template> INFO = new Info();
    private final Class<E> enumClass;

    private EnumArgument(final Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    public static <E extends Enum<E>> EnumArgument<?> of(final Class<E> enumClass) {
        return new EnumArgument<>(enumClass);
    }

    @Override
    public E parse(final StringReader reader) throws CommandSyntaxException {
        return LibUtil.getEnumConstant(reader.readUnquotedString(), this.enumClass)
            .orElseThrow(() -> Exceptions.cmdSyntax(reader, "No such value"));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder sb) {
        return SharedSuggestionProvider.suggest(
            Stream.of(this.enumClass.getEnumConstants()).map(e -> e.toString().toLowerCase()), sb);
    }

    private static class Info implements ArgumentTypeInfo<EnumArgument<?>, Info.Template> {

        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buf) {
            buf.writeUtf(template.qualifiedPath);
        }

        @Override
        public @NotNull Template deserializeFromNetwork(FriendlyByteBuf buf) {
            return new Template(buf.readUtf());
        }

        @Override
        public void serializeToJson(Template template, JsonObject json) {
            json.addProperty("enumClass", template.qualifiedPath);
        }

        @Override
        public @NotNull Template unpack(EnumArgument<?> ea) {
            return new Template(ea.enumClass.getName());
        }

        private class Template implements ArgumentTypeInfo.Template<EnumArgument<?>> {
            private final String qualifiedPath;

            private Template(final String qualifiedPath) {
                this.qualifiedPath = qualifiedPath;
            }

            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public @NotNull EnumArgument<?> instantiate(CommandBuildContext ctx) {
                return new EnumArgument(getEnumClass(this.qualifiedPath));
            }

            @SuppressWarnings("unchecked")
            private static Class<? extends Enum<?>> getEnumClass(final String qualifiedPath) {
                try {
                    final Class<?> clazz = Class.forName(qualifiedPath);
                    if (!clazz.isEnum()) {
                        throw new IllegalArgumentException("Not an enum: " + qualifiedPath);
                    }
                    return (Class<? extends Enum<?>>) clazz;
                } catch (final ClassNotFoundException e) {
                    throw new IllegalArgumentException("Class not found: " + qualifiedPath, e);
                }
            }

            @Override
            public @NotNull ArgumentTypeInfo<EnumArgument<?>, ?> type() {
                return Info.this;
            }
        }
    }
}

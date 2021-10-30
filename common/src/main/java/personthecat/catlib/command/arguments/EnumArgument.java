package personthecat.catlib.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import personthecat.catlib.exception.Exceptions;
import personthecat.catlib.util.LibReference;
import personthecat.catlib.util.McUtils;
import personthecat.catlib.util.Shorthand;
import personthecat.fresult.Void;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class EnumArgument<E extends Enum<E>> implements ArgumentType<E> {

    public static void register() {
        ArgumentTypes.register(LibReference.MOD_ID + ":enum_argument", EnumArgument.class,
            new EmptyArgumentSerializer<>(() -> new EnumArgument<>(Void.class)));
    }

    private final Class<E> enumClass;

    private EnumArgument(final Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    public static <E extends Enum<E>> EnumArgument<?> of(final Class<E> enumClass) {
        return new EnumArgument<>(enumClass);
    }

    @Override
    public E parse(final StringReader reader) throws CommandSyntaxException {
        return Shorthand.getEnumConstant(reader.readUnquotedString(), this.enumClass)
            .orElseThrow(() -> Exceptions.cmdSyntax(reader, "No such value"));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder sb) {
        return SharedSuggestionProvider.suggest(
            Stream.of(this.enumClass.getEnumConstants()).map(e -> e.toString().toLowerCase()), sb);
    }
}

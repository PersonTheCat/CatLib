package personthecat.catlib.command.arguments;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import lombok.experimental.UtilityClass;
import personthecat.catlib.command.CommandRegistrationContext;
import personthecat.catlib.command.CommandSuggestions;

@UtilityClass
@SuppressWarnings("unused")
public class ArgumentSuppliers {

    public static class File implements ArgumentSupplier<java.io.File> {
        public ArgumentDescriptor<java.io.File> get() {
            return new ArgumentDescriptor<>(new FileArgument(getModConfigFolderOrThrow()));
        }
    }

    public static class HjsonFile implements ArgumentSupplier<HjsonArgument.Result> {
        public ArgumentDescriptor<HjsonArgument.Result> get() {
            return new ArgumentDescriptor<>(new HjsonArgument(getModConfigFolderOrThrow()));
        }
    }

    public static class AnyValue implements ArgumentSupplier<String> {
        public ArgumentDescriptor<String> get() {
            return new ArgumentDescriptor<>(StringArgumentType.greedyString(), CommandSuggestions.ANY_VALUE);
        }
    }

    public static class AnyInt implements ArgumentSupplier<Integer> {
        public ArgumentDescriptor<Integer> get() {
            return new ArgumentDescriptor<>(IntegerArgumentType.integer(), CommandSuggestions.ANY_INT);
        }
    }

    public static class AnyDouble implements ArgumentSupplier<Double> {
        public ArgumentDescriptor<Double> get() {
            return new ArgumentDescriptor<>(DoubleArgumentType.doubleArg(), CommandSuggestions.ANY_DECIMAL);
        }
    }

    private static java.io.File getModConfigFolderOrThrow() {
        return CommandRegistrationContext.getActiveModOrThrow().getConfigFolder();
    }
}

package personthecat.catlib.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.util.SyntaxLinter;

public class LinterTestException extends FormattedException {

    private static final String TEST =
        "{\n" +
        "  goodString: OK\n" +
        "  goodBool: true\n" +
        "  goodNull: null\n" +
        "  goodArray: [ 1, 2, 3 ]\n" +
        "  badArray: [ text ]\n" +
        "  goodArray2: [ true, false, null ]\n" +
        "  goodArray3: [ 1, 2, 3 ]\n" +
        "  goodArray4: [ \"Hello, world!\" ]\n" +
        "  badArray2: [ true, false, banana ]\n" +
        "  badArray3: [ \"good?\" ] ]\n" +
        "  goodObject: {\n" +
        "    ok: true\n" +
        "  }\n" +
        "}";

    public LinterTestException() {
        super("Linter Test");
    }

    @Override
    public @NotNull String getCategory() {
        return "Syntax Errors";
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return new TextComponent("Linter Test");
    }

    @Override
    public @NotNull Component getDetailMessage() {
        return SyntaxLinter.DEFAULT_LINTER.lint(TEST);
    }
}

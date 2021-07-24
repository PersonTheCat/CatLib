package personthecat.catlib.command.annotations;

import com.mojang.brigadier.arguments.ArgumentType;
import personthecat.catlib.command.arguments.ListArgumentBuilder;
import personthecat.catlib.command.arguments.ArgumentSupplier;

public @interface Node {
    String name();
    boolean executes() default false;

    Class<? extends ArgumentType<?>>[] type() default {};
    Class<? extends ArgumentSupplier<?>>[] descriptor() default {};
    IntRange[] intRange() default {};
    DoubleRange[] doubleRange() default {};
    boolean isBoolean() default false;
    StringValue[] stringVal() default {};

    ListInfo intoList() default @ListInfo(useList = false);

    @interface IntRange {
        int min();
        int max();
    }

    @interface DoubleRange {
        double min();
        double max();
    }

    @interface StringValue {
        Type value() default Type.WORD;
        enum Type { STRING, GREEDY, WORD }
    }

    @interface ListInfo {
        boolean useList() default true;
        int size() default ListArgumentBuilder.MAX_LIST_DEPTH;
    }
}

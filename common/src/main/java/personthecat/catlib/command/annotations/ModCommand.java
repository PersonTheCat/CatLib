package personthecat.catlib.command.annotations;

import personthecat.catlib.command.CommandSide;
import personthecat.catlib.command.CommandType;
import personthecat.catlib.util.SyntaxLinter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModCommand {
    String name();
    String arguments() default "";
    String[] description() default {};
    Class<? extends SyntaxLinter>[] linter() default {};
    Node[] branch() default {};
    CommandType type() default CommandType.MOD;
    CommandSide side() default CommandSide.EITHER;
}

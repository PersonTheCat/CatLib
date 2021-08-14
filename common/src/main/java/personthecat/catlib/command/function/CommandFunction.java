package personthecat.catlib.command.function;

import personthecat.catlib.command.CommandContextWrapper;

public interface CommandFunction {
    void execute(final CommandContextWrapper wrapper) throws Throwable;
}

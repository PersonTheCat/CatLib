package personthecat.catlib.command;

public enum CommandType {

    /**
     * Indicates that a command should be applied to the root
     * command node.
     */
    GLOBAL,

    /**
     * Indicates that A command should be applied to the given
     * mod's main command.
     */
    MOD
}

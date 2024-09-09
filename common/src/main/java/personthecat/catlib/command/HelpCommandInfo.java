package personthecat.catlib.command;

public record HelpCommandInfo(String name, String arguments, String description, CommandType type) {
    public boolean isGlobal() {
        return this.type == CommandType.GLOBAL;
    }
}

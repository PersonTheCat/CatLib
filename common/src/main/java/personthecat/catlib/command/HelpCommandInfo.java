package personthecat.catlib.command;

import lombok.Value;

@Value
public class HelpCommandInfo {
    String name;
    String arguments;
    String description;
    CommandType type;

    public boolean isGlobal() {
        return this.type == CommandType.GLOBAL;
    }
}

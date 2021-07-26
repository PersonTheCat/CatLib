package personthecat.catlib.command;

public enum CommandSide {

    /**
     * Indicates that a command is valid for and can only run
     * on the dedicated server side.
     */
    DEDICATED {
        @Override
        public boolean canRegister(final boolean dedicated) {
            return dedicated;
        }
    },

    /**
     * Indicates that a command is valid for and can only run
     * on the integrated, client server side.
     */
    CLIENT {
        @Override
        public boolean canRegister(final boolean dedicated) {
            return !dedicated;
        }
    },

    /**
     * Indicates that a command is valid for and can run on
     * either the dedicated or integrated server side.
     */
    EITHER {
        @Override
        public boolean canRegister(final boolean dedicated) {
            return true;
        }
    };

    public abstract boolean canRegister(final boolean dedicated);
}


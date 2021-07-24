package personthecat.catlib.command;

public enum CommandSide {
    DEDICATED {
        @Override
        public boolean canRegister(final boolean dedicated) {
            return dedicated;
        }
    },
    CLIENT {
        @Override
        public boolean canRegister(final boolean dedicated) {
            return !dedicated;
        }
    },
    EITHER {
        @Override
        public boolean canRegister(final boolean dedicated) {
            return true;
        }
    };

    public abstract boolean canRegister(final boolean dedicated);
}


package personthecat.catlib.data.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class NonRecursiveObserverSet<O> extends SimpleObserverSet<O> {

    public NonRecursiveObserverSet() {
        super();
    }

    public NonRecursiveObserverSet(final Collection<O> entries) {
        super(entries);
    }

    @Override
    public boolean hasActiveEntries() {
        for (final SimpleTrackedEntry<O> entry : this.tracked) {
            if (entry.isActive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void forEach(final Consumer<O> fn) {
        if (this.tracked.isEmpty()) {
            return;
        }
        for (final SimpleTrackedEntry<O> entry : new ArrayList<>(this.tracked)) {
            if (!(entry.isRemoved() || entry.isActive())) {
                entry.setActive(true);
                try {
                    fn.accept(entry.getObserver());
                } finally {
                    entry.setActive(false);
                }
            }
        }
    }
}

package personthecat.catlib.data;

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
    public void forEach(final Consumer<O> fn) {
        for (final SimpleTrackedEntry<O> entry : new ArrayList<>(this.tracked)) {
            if (!(entry.isRemoved() || entry.isActive())) {
                entry.setActive(true);
                fn.accept(entry.getObserver());
                entry.setActive(false);
            }
        }
    }
}

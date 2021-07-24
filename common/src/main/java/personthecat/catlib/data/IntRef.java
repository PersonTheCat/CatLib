package personthecat.catlib.data;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class IntRef {
    public int value;

    public IntRef(final int value) {
        this.value = value;
    }

    public int get() {
        return this.value;
    }

    public int set(final int value) {
        return this.value = value;
    }

    public int add(final int rhs) {
        return this.value += rhs;
    }

    public int sub(final int rhs) {
        return this.value -= rhs;
    }

    public int increment() {
        return this.add(1);
    }

    public int decrement() {
        return this.sub(1);
    }
}
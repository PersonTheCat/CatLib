package personthecat.catlib.serialization.codec;

import com.mojang.serialization.JavaOps;
import net.minecraft.resources.DelegatingOps;

public final class LiteralOps extends DelegatingOps<Object> {
    public static final LiteralOps INSTANCE = new LiteralOps();

    private LiteralOps() {
        super(JavaOps.INSTANCE);
    }

    @Override
    public String toString() {
        return "Literal";
    }
}

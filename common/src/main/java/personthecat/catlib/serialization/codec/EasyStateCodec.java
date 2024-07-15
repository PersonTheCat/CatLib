package personthecat.catlib.serialization.codec;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.registry.CommonRegistries;

public class EasyStateCodec implements Codec<BlockState> {

    private final HolderLookup<Block> blocks = CommonRegistries.BLOCK.asLookup();

    @Override
    public <T> DataResult<T> encode(final BlockState input, final DynamicOps<T> ops, final T prefix) {
        return DataResult.success(ops.createString(BlockStateParser.serialize(input)));
    }

    @Override
    public <T> DataResult<Pair<BlockState, T>> decode(final DynamicOps<T> ops, final T input) {
        return ops.getStringValue(input).flatMap(id -> {
            try {
                final BlockState parsed = BlockStateParser.parseForBlock(this.blocks, id, false).blockState();
                return DataResult.success(Pair.of(parsed, input));
            } catch (final CommandSyntaxException e) {
                return DataResult.error(e::getMessage);
            }
        });
    }

    @Override
    public String toString() {
        return "EasyStateCodec";
    }
}

package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import personthecat.catlib.registry.CommonRegistries;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.serialization.codec.XjsOps;
import personthecat.catlib.test.McBootstrapExtension;
import xjs.data.Json;
import xjs.data.JsonValue;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(McBootstrapExtension.class)
public class IdListTest {
    private static final ResourceKey<Block> SAND_KEY = blockKey("sand");
    private static final ResourceKey<Block> DIRT_KEY = blockKey("dirt");
    private static final ResourceKey<Block> CLAY_KEY = blockKey("clay");
    private static final ResourceKey<Block> GRAVEL_KEY = blockKey("gravel");
    private static final ResourceKey<Block> OAK_LOG_KEY = blockKey("oak_log");
    private static final ResourceKey<Block> BIRCH_LOG_KEY = blockKey("birch_log");
    private static final ResourceKey<Block> GOLD_ORE_KEY = blockKey("gold_ore");
    private static final ResourceKey<Block> IRON_ORE_KEY = blockKey("iron_ore");
    private static final ResourceKey<Block> COPPER_ORE_KEY = blockKey("copper_ore");

    private static final ResourceKey<ArgumentTypeInfo<?, ?>> DOUBLE_KEY = argKey("brigadier:double");
    private static final ResourceKey<ArgumentTypeInfo<?, ?>> STRING_KEY = argKey("brigadier:string");
    private static final ResourceKey<ArgumentTypeInfo<?, ?>> ENTITY_KEY = argKey("minecraft:entity");
    private static final ResourceKey<ArgumentTypeInfo<?, ?>> FLOAT_KEY = argKey("brigadier:float");

    private static final ResourceKey<Item> CROSSBOW_KEY = itemKey("crossbow");
    private static final ResourceKey<Item> GOLD_ORE_ITEM_KEY = itemKey("gold_ore");
    private static final ResourceKey<Item> DIAMOND_KEY = itemKey("diamond");

    @Test
    public void newInstance_isPossibleToTest() {
        assertDoesNotThrow(() -> new IdList.Builder<>(Registries.BLOCK).build());
    }

    @Test
    public void idList_matchesRegularIds() {
        final RegistryHandle<Block> blocks = CommonRegistries.BLOCK;
        final IdList<Block> list =
            IdList.builder(Registries.BLOCK)
                .addEntry(IdMatcher.id(false, SAND_KEY))
                .addEntry(IdMatcher.id(false, DIRT_KEY))
                .build();
        assertTrue(list.test(blocks.getHolder(SAND_KEY)));
        assertTrue(list.test(blocks.getHolder(DIRT_KEY)));
        assertFalse(list.test(blocks.getHolder(CLAY_KEY)));
    }

    @Test
    public void idList_matchesByMod() {
        final RegistryHandle<ArgumentTypeInfo<?, ?>> argumentTypes =
            CommonRegistries.get(Registries.COMMAND_ARGUMENT_TYPE);
        final IdList<ArgumentTypeInfo<?, ?>> list =
            IdList.builder(Registries.COMMAND_ARGUMENT_TYPE)
                .addEntry(IdMatcher.mod(false, "brigadier"))
                .build();
        assertTrue(list.test(argumentTypes.getHolder(DOUBLE_KEY)));
        assertTrue(list.test(argumentTypes.getHolder(STRING_KEY)));
        assertFalse(list.test(argumentTypes.getHolder(ENTITY_KEY)));
    }

    @Test
    public void idList_matchesByTag() {
        final Registry<Block> blocks = BuiltInRegistries.BLOCK;
        final TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, new ResourceLocation("test"));
        blocks.getOrCreateTag(tagKey);
        blocks.bindTags(Map.of(tagKey, List.of(
            blocks.getHolder(GRAVEL_KEY).orElseThrow(),
            blocks.getHolder(OAK_LOG_KEY).orElseThrow())));
        final IdList<Block> list =
            IdList.builder(Registries.BLOCK)
                .addEntry(IdMatcher.tag(false, tagKey))
                .build();
        assertTrue(list.test(blocks.getHolder(GRAVEL_KEY).orElseThrow()));
        assertTrue(list.test(blocks.getHolder(OAK_LOG_KEY).orElseThrow()));
        assertFalse(list.test(blocks.getHolder(BIRCH_LOG_KEY).orElseThrow()));
    }

    @Test
    public void idList_canInvertAll() {
        final RegistryHandle<Block> blocks = CommonRegistries.BLOCK;
        final IdList<Block> list =
            IdList.builder(Registries.BLOCK)
                .addEntry(IdMatcher.id(false, SAND_KEY))
                .addEntry(IdMatcher.id(false, DIRT_KEY))
                .blacklist(true)
                .build();
        assertFalse(list.test(blocks.getHolder(SAND_KEY)));
        assertFalse(list.test(blocks.getHolder(DIRT_KEY)));
        assertTrue(list.test(blocks.getHolder(CLAY_KEY)));
    }

    @Test
    public void idList_canInvertSingle() {
        final RegistryHandle<ArgumentTypeInfo<?, ?>> argumentTypes =
            CommonRegistries.get(Registries.COMMAND_ARGUMENT_TYPE);
        final IdList<ArgumentTypeInfo<?, ?>> list =
            IdList.builder(Registries.COMMAND_ARGUMENT_TYPE)
                .addEntry(IdMatcher.mod(false, "brigadier"))
                .addEntry(IdMatcher.id(true, STRING_KEY))
                .build();
        assertTrue(list.test(argumentTypes.getHolder(FLOAT_KEY)));
        assertTrue(list.test(argumentTypes.getHolder(DOUBLE_KEY)));
        assertFalse(list.test(argumentTypes.getHolder(STRING_KEY)));
    }

    @Test
    public void idList_supportsAllInverted() {
        final RegistryHandle<Block> blocks = CommonRegistries.BLOCK;
        final IdList<Block> list =
            IdList.builder(Registries.BLOCK)
                .addEntry(IdMatcher.id(true, SAND_KEY))
                .addEntry(IdMatcher.id(true, DIRT_KEY))
                .build();
        assertFalse(list.test(blocks.getHolder(SAND_KEY)));
        assertFalse(list.test(blocks.getHolder(DIRT_KEY)));
        assertTrue(list.test(blocks.getHolder(CLAY_KEY)));
    }

    @Test
    public void idList_emptyList_matchesNone() {
        final RegistryHandle<Item> items = CommonRegistries.ITEM;
        final IdList<Item> list = IdList.builder(Registries.ITEM).build();
        assertFalse(list.test(items.getHolder(CROSSBOW_KEY)));
        assertFalse(list.test(items.getHolder(GOLD_ORE_ITEM_KEY)));
        assertFalse(list.test(items.getHolder(DIAMOND_KEY)));
    }

    @Test
    public void idList_emptyBlacklist_matchesAll() {
        final RegistryHandle<Item> items = CommonRegistries.ITEM;
        final IdList<Item> list = IdList.builder(Registries.ITEM).blacklist(true).build();
        assertTrue(list.test(items.getHolder(CROSSBOW_KEY)));
        assertTrue(list.test(items.getHolder(GOLD_ORE_ITEM_KEY)));
        assertTrue(list.test(items.getHolder(DIAMOND_KEY)));
    }

    @Test
    public void idList_canBeParsed_fromList() {
        final IdList<Block> parsed = parseBlockList("""
            [ 'iron_ore', 'copper_ore' ]
            """);
        final IdList<Block> expected = IdList.builder(Registries.BLOCK)
            .addEntry(IdMatcher.id(false, IRON_ORE_KEY))
            .addEntry(IdMatcher.id(false, COPPER_ORE_KEY))
            .format(IdList.Format.LIST)
            .build();
        assertEquals(expected, parsed);
    }

    @Test
    public void idList_canBeParsed_fromSingleEntry() {
        final IdList<Block> parsed = parseBlockList("""
            'iron_ore'
            """);
        final IdList<Block> expected = IdList.builder(Registries.BLOCK)
            .addEntry(IdMatcher.id(false, IRON_ORE_KEY))
            .format(IdList.Format.LIST)
            .build();
        assertEquals(expected, parsed);
    }

    @Test
    public void idList_canBeParsed_fromObject() {
        final IdList<Block> parsed = parseBlockList("""
            {
              names: [ 'iron_ore', 'copper_ore' ],
            }""");
        final IdList<Block> expected = IdList.builder(Registries.BLOCK)
            .addEntry(IdMatcher.id(false, IRON_ORE_KEY))
            .addEntry(IdMatcher.id(false, COPPER_ORE_KEY))
            .format(IdList.Format.OBJECT)
            .build();
        assertEquals(expected, parsed);
    }

    @Test
    public void idList_fromList_supportsPrefixedEntries() {
        final IdList<Block> parsed = parseBlockList("""
            [ 'iron_ore', '#mineable/axe', '@quark' ]
            """);
        final IdList<Block> expected = IdList.builder(Registries.BLOCK)
            .addEntry(IdMatcher.id(false, IRON_ORE_KEY))
            .addEntry(IdMatcher.tag(false, BlockTags.MINEABLE_WITH_AXE))
            .addEntry(IdMatcher.mod(false, "quark"))
            .format(IdList.Format.LIST)
            .build();
        assertEquals(expected, parsed);
    }

    @Test
    public void idList_fromList_supportsInvertedEntries() {
        final IdList<Block> parsed = parseBlockList("""
            [ '!iron_ore', 'gold_ore', '!copper_ore' ]
            """);
        final IdList<Block> expected = IdList.builder(Registries.BLOCK)
            .addEntry(IdMatcher.id(true, IRON_ORE_KEY))
            .addEntry(IdMatcher.id(false, GOLD_ORE_KEY))
            .addEntry(IdMatcher.id(true, COPPER_ORE_KEY))
            .format(IdList.Format.LIST)
            .build();
        assertEquals(expected, parsed);
    }

    @Test
    public void idList_fromList_canInvertPrefixedEntries() {
        final IdList<Block> parsed = parseBlockList("""
            [ '!#iron_ores', '!@quark' ]
            """);
        final IdList<Block> expected = IdList.builder(Registries.BLOCK)
            .addEntry(IdMatcher.tag(true, BlockTags.IRON_ORES))
            .addEntry(IdMatcher.mod(true, "quark"))
            .format(IdList.Format.LIST)
            .build();
        assertEquals(expected, parsed);
    }

    @Test
    public void idList_fromObject_supportsAdditionalKeys() {
        final IdList<Block> parsed = parseBlockList("""
            {
              names: [ 'iron_ore', 'copper_ore' ],
              mods: [ 'quark' ],
            }""");
        final IdList<Block> expected = IdList.builder(Registries.BLOCK)
            .addEntry(IdMatcher.id(false, IRON_ORE_KEY))
            .addEntry(IdMatcher.id(false, COPPER_ORE_KEY))
            .addEntry(IdMatcher.mod(false, "quark"))
            .format(IdList.Format.OBJECT)
            .build();
        assertEquals(expected, parsed);
    }

    @Test
    public void idList_fromObject_supportsBlacklistMode() {
        final IdList<Block> parsed = parseBlockList("""
            {
              names: [ 'iron_ore', 'copper_ore' ],
              blacklist: true,
            }""");
        final IdList<Block> expected = IdList.builder(Registries.BLOCK)
            .addEntry(IdMatcher.id(false, IRON_ORE_KEY))
            .addEntry(IdMatcher.id(false, COPPER_ORE_KEY))
            .format(IdList.Format.OBJECT)
            .blacklist(true)
            .build();
        assertEquals(expected, parsed);
    }

    @Test
    public void idList_fromList_isSerializedAsList() {
        final IdList<Block> list = IdList.builder(Registries.BLOCK)
            .addEntry(IdMatcher.id(false, IRON_ORE_KEY))
            .addEntry(IdMatcher.id(false, COPPER_ORE_KEY))
            .format(IdList.Format.LIST)
            .build();
        final JsonValue expected =
            Json.array("minecraft:iron_ore", "minecraft:copper_ore");
        assertMatches(expected, writeBlockList(list));
    }

    @Test
    public void idList_fromObject_isSerializedAsObject() {
        final IdList<Block> list = IdList.builder(Registries.BLOCK)
            .addEntry(IdMatcher.id(false, IRON_ORE_KEY))
            .addEntry(IdMatcher.id(false, COPPER_ORE_KEY))
            .format(IdList.Format.OBJECT)
            .build();
        final JsonValue expected =
            Json.object().add("names", Json.array("minecraft:iron_ore", "minecraft:copper_ore"));
        assertMatches(expected, writeBlockList(list));
    }

    @Test
    public void idList_inBlacklistMode_isNeverSerializedAsList() {
        final IdList<Block> list = IdList.builder(Registries.BLOCK)
            .addEntry(IdMatcher.id(false, IRON_ORE_KEY))
            .addEntry(IdMatcher.id(false, COPPER_ORE_KEY))
            .blacklist(true)
            .format(IdList.Format.OBJECT)
            .build();
        final JsonValue expected =
            Json.object()
                .add("blacklist", true)
                .add("names", Json.array("minecraft:iron_ore", "minecraft:copper_ore"));
        assertMatches(expected, writeBlockList(list));
    }

    private static IdList<Block> parseBlockList(final String djs) {
        return blockCodec().parse(XjsOps.INSTANCE, Json.parse(djs)).getOrThrow();
    }

    private static JsonValue writeBlockList(final IdList<Block> list) {
        return blockCodec().encodeStart(XjsOps.INSTANCE, list).getOrThrow();
    }

    private static Codec<IdList<Block>> blockCodec() {
        return IdList.codecOf(Registries.BLOCK);
    }

    private static void assertMatches(final JsonValue expected, final JsonValue actual) {
        if (!expected.matches(actual)) {
            throw new AssertionError("Expected: " + expected + "\nbut was: " + actual + "\n");
        }
    }

    private static ResourceKey<Block> blockKey(String id) {
        return ResourceKey.create(Registries.BLOCK, new ResourceLocation(id));
    }

    private static ResourceKey<ArgumentTypeInfo<?, ?>> argKey(String id) {
        return ResourceKey.create(Registries.COMMAND_ARGUMENT_TYPE, new ResourceLocation(id));
    }

    private static ResourceKey<Item> itemKey(String id) {
        return ResourceKey.create(Registries.ITEM, new ResourceLocation(id));
    }
}

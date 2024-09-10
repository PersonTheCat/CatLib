package personthecat.catlib.data;

import com.mojang.serialization.Codec;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

    @Test
    public void newInstance_isPossibleToTest() {
        assertDoesNotThrow(() -> new IdList.Builder<>(Registries.BLOCK).build());
    }

    @Test
    public void idList_matchesRegularIds() {
        final RegistryHandle<Block> blocks = CommonRegistries.BLOCK;
        final IdList<Block> list =
            IdList.builder(Registries.BLOCK)
                .addEntries(IdMatcher.id(false, new ResourceLocation("sand")))
                .addEntries(IdMatcher.id(false, new ResourceLocation("dirt")))
                .build();
        assertTrue(list.test(blocks.getHolder(new ResourceLocation("sand"))));
        assertTrue(list.test(blocks.getHolder(new ResourceLocation("dirt"))));
        assertFalse(list.test(blocks.getHolder(new ResourceLocation("clay"))));
    }

    @Test
    public void idList_matchesByMod() {
        final RegistryHandle<ArgumentTypeInfo<?, ?>> argumentTypes =
            CommonRegistries.get(Registries.COMMAND_ARGUMENT_TYPE);
        final IdList<ArgumentTypeInfo<?, ?>> list =
            IdList.builder(Registries.COMMAND_ARGUMENT_TYPE)
                .addEntries(IdMatcher.mod(false, "brigadier"))
                .build();
        assertTrue(list.test(argumentTypes.getHolder(new ResourceLocation("brigadier:double"))));
        assertTrue(list.test(argumentTypes.getHolder(new ResourceLocation("brigadier:string"))));
        assertFalse(list.test(argumentTypes.getHolder(new ResourceLocation("minecraft:entity"))));
    }

    @Test
    public void idList_matchesByTag() {
        final Registry<Block> blocks = BuiltInRegistries.BLOCK;
        final ResourceLocation tagId = new ResourceLocation("test");
        final TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
        blocks.getOrCreateTag(tagKey);
        blocks.bindTags(Map.of(tagKey, List.of(
            blocks.getHolder(ResourceKey.create(Registries.BLOCK, new ResourceLocation("gravel"))).orElseThrow(),
            blocks.getHolder(ResourceKey.create(Registries.BLOCK, new ResourceLocation("oak_log"))).orElseThrow())));
        final IdList<Block> list =
            IdList.builder(Registries.BLOCK)
                .addEntries(IdMatcher.tag(false, tagId))
                .build();
        assertTrue(list.test(blocks.getHolder(ResourceKey.create(Registries.BLOCK, new ResourceLocation("gravel"))).orElseThrow()));
        assertTrue(list.test(blocks.getHolder(ResourceKey.create(Registries.BLOCK, new ResourceLocation("oak_log"))).orElseThrow()));
        assertFalse(list.test(blocks.getHolder(ResourceKey.create(Registries.BLOCK, new ResourceLocation("birch_log"))).orElseThrow()));
    }

    @Test
    public void idList_canInvertAll() {
        final RegistryHandle<Block> blocks = CommonRegistries.BLOCK;
        final IdList<Block> list =
            IdList.builder(Registries.BLOCK)
                .addEntries(IdMatcher.id(false, new ResourceLocation("sand")))
                .addEntries(IdMatcher.id(false, new ResourceLocation("dirt")))
                .blacklist(true)
                .build();
        assertFalse(list.test(blocks.getHolder(new ResourceLocation("sand"))));
        assertFalse(list.test(blocks.getHolder(new ResourceLocation("dirt"))));
        assertTrue(list.test(blocks.getHolder(new ResourceLocation("clay"))));
    }

    @Test
    public void idList_canInvertSingle() {
        final RegistryHandle<ArgumentTypeInfo<?, ?>> argumentTypes =
            CommonRegistries.get(Registries.COMMAND_ARGUMENT_TYPE);
        final IdList<ArgumentTypeInfo<?, ?>> list =
            IdList.builder(Registries.COMMAND_ARGUMENT_TYPE)
                .addEntries(IdMatcher.mod(false, "brigadier"))
                .addEntries(IdMatcher.id(true, new ResourceLocation("brigadier:string")))
                .build();
        assertTrue(list.test(argumentTypes.getHolder(new ResourceLocation("brigadier:float"))));
        assertTrue(list.test(argumentTypes.getHolder(new ResourceLocation("brigadier:double"))));
        assertFalse(list.test(argumentTypes.getHolder(new ResourceLocation("brigadier:string"))));
    }

    @Test
    public void idList_emptyList_matchesNone() {
        final RegistryHandle<Item> items = CommonRegistries.ITEM;
        final IdList<Item> list = IdList.builder(Registries.ITEM).build();
        assertFalse(list.test(items.getHolder(new ResourceLocation("crossbow"))));
        assertFalse(list.test(items.getHolder(new ResourceLocation("gold_ore"))));
        assertFalse(list.test(items.getHolder(new ResourceLocation("diamond"))));
    }

    @Test
    public void idList_emptyBlacklist_matchesAll() {
        final RegistryHandle<Item> items = CommonRegistries.ITEM;
        final IdList<Item> list = IdList.builder(Registries.ITEM).blacklist(true).build();
        assertTrue(list.test(items.getHolder(new ResourceLocation("crossbow"))));
        assertTrue(list.test(items.getHolder(new ResourceLocation("gold_ore"))));
        assertTrue(list.test(items.getHolder(new ResourceLocation("diamond"))));
    }

    @Test
    public void idList_canBeParsed_fromList() {
        final IdList<Block> parsed = parseBlockList("""
            [ 'iron_ore', 'copper_ore' ]
            """);
        final IdList<Block> expected = IdList.builder(Registries.BLOCK)
            .addEntries(
                IdMatcher.id(false, new ResourceLocation("iron_ore")),
                IdMatcher.id(false, new ResourceLocation("copper_ore")))
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
            .addEntries(
                IdMatcher.id(false, new ResourceLocation("iron_ore")))
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
            .addEntries(
                IdMatcher.id(false, new ResourceLocation("iron_ore")),
                IdMatcher.id(false, new ResourceLocation("copper_ore")))
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
            .addEntries(
                IdMatcher.id(false, new ResourceLocation("iron_ore")),
                IdMatcher.tag(false, new ResourceLocation("mineable/axe")),
                IdMatcher.mod(false, "quark"))
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
            .addEntries(
                IdMatcher.id(true, new ResourceLocation("iron_ore")),
                IdMatcher.id(false, new ResourceLocation("gold_ore")),
                IdMatcher.id(true, new ResourceLocation("copper_ore")))
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
            .addEntries(
                IdMatcher.id(false, new ResourceLocation("iron_ore")),
                IdMatcher.id(false, new ResourceLocation("copper_ore")),
                IdMatcher.mod(false, "quark"))
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
            .addEntries(
                IdMatcher.id(false, new ResourceLocation("iron_ore")),
                IdMatcher.id(false, new ResourceLocation("copper_ore")))
            .format(IdList.Format.OBJECT)
            .blacklist(true)
            .build();
        assertEquals(expected, parsed);
    }

    @Test
    public void idList_fromList_isSerializedAsList() {
        final IdList<Block> list = IdList.builder(Registries.BLOCK)
            .addEntries(
                IdMatcher.id(false, new ResourceLocation("iron_ore")),
                IdMatcher.id(false, new ResourceLocation("copper_ore")))
            .format(IdList.Format.LIST)
            .build();
        final JsonValue expected =
            Json.array("minecraft:iron_ore", "minecraft:copper_ore");
        assertMatches(expected, writeBlockList(list));
    }

    @Test
    public void idList_fromObject_isSerializedAsObject() {
        final IdList<Block> list = IdList.builder(Registries.BLOCK)
            .addEntries(
                IdMatcher.id(false, new ResourceLocation("iron_ore")),
                IdMatcher.id(false, new ResourceLocation("copper_ore")))
            .format(IdList.Format.OBJECT)
            .build();
        final JsonValue expected =
            Json.object().add("names", Json.array("minecraft:iron_ore", "minecraft:copper_ore"));
        assertMatches(expected, writeBlockList(list));
    }

    @Test
    public void idList_inBlacklistMode_isNeverSerializedAsList() {
        final IdList<Block> list = IdList.builder(Registries.BLOCK)
            .addEntries(
                IdMatcher.id(false, new ResourceLocation("iron_ore")),
                IdMatcher.id(false, new ResourceLocation("copper_ore")))
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
}

package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import xjs.data.Json;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class OptionalCodecTest {
    private final Codec<Optional<ResourceLocation>> testSubject =
        new OptionalCodec<>(ResourceLocation.CODEC);

    @Test
    public void parse_whenInputIsPresent_returnsOptionalOf() {
        final var optional = this.testSubject.parse(XjsOps.INSTANCE, Json.parse("'catlib:test'")).getOrThrow();
        assertEquals(Optional.of(new ResourceLocation("catlib", "test")), optional);
    }

    @Test
    public void parse_whenInputIsEmpty_returnsOptionalEmpty() {
        final var optional = this.testSubject.parse(XjsOps.INSTANCE, Json.parse("null")).getOrThrow();
        assertEquals(Optional.empty(), optional);
    }

    @Test
    public void parse_whenValueIsPresent_butInvalid_returnsError() {
        final var result = this.testSubject.parse(XjsOps.INSTANCE, Json.parse("1234"));
        assertInstanceOf(DataResult.Error.class, result);
    }

    @Test
    public void encode_whenInputIsPresent_writesValue() {
        final var id = new ResourceLocation("catlib", "test");
        final var json = this.testSubject.encodeStart(XjsOps.INSTANCE, Optional.of(id)).getOrThrow();
        assertEquals(Json.value("catlib:test"), json);
    }

    @Test
    public void encode_whenValueIsEmpty_writesNull() {
        final var json = this.testSubject.encodeStart(XjsOps.INSTANCE, Optional.empty()).getOrThrow();
        assertEquals(Json.value(null), json);
    }

    @Test
    public void parseList_whenElementIsPresent_containsOptionalOf() {
        final var list = this.testSubject.listOf().parse(XjsOps.INSTANCE, Json.parse("['catlib:test']")).getOrThrow();
        assertEquals(List.of(Optional.of(new ResourceLocation("catlib", "test"))), list);
    }

    @Test
    public void encodeList_whenElementIsEmpty_containsNull() {
        final var json = this.testSubject.listOf().encodeStart(XjsOps.INSTANCE, List.of(Optional.empty())).getOrThrow();
        assertEquals(Json.array((String) null), json);
    }
}

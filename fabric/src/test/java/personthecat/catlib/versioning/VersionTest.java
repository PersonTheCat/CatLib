package personthecat.catlib.versioning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class VersionTest {

    @Test
    public void equals_versionNumbers_returnsExpectedValue() {
        assertEquals(Version.create(1, 0), Version.create(1, 0));
        assertEquals(Version.create(65536, 65536), Version.create(65536, 65536));
    }

    @Test
    public void equals_tagValue_returnsExpectedValue() {
        assertEquals(Version.create(0, 0, "TAG"), Version.create(0, 0, "tag"));
        assertEquals(Version.create(0, 0, "SNAPSHOT"), Version.create(0, 0, "snapshot"));
    }

    @Test
    public void parse_returnsExpectedValue() {
        assertEquals(Version.parse("1.0"), Version.create(1, 0));
        assertEquals(Version.parse("1.999.999-tag"), Version.create(1, 999, 999, "tag"));
        assertEquals(Version.parse("3.0-tag+1.20.6"), Version.create(3, 0, 0, "tag", "1.20.6"));
        assertEquals(Version.parse("65536"), Version.create(65536, 0));
    }

    @Test
    public void parse_numberTooHigh() {
        assertThrows(Version.TooHigh.class, () -> Version.parse("99999"));
        assertThrows(Version.TooHigh.class, () -> Version.parse("1.0.99999"));
    }

    @Test
    public void parse_doesNotMatch() {
        assertThrows(Version.DoesNotMatch.class, () -> Version.parse(""));
        assertThrows(Version.DoesNotMatch.class, () -> Version.parse("100000"));
        assertThrows(Version.DoesNotMatch.class, () -> Version.parse("1.0.0.0"));
        assertThrows(Version.DoesNotMatch.class, () -> Version.parse("1.0.0-"));
    }

    @Test
    public void create_tooLow() {
        assertThrows(Version.TooLow.class, () -> Version.create(-1, 0));
        assertThrows(Version.TooLow.class, () -> Version.create(1, -1));
    }

    @Test
    public void compareTo_versionNumbers_returnsExpectedValue() {
        final Version v1_0 = Version.create(1, 0, 0, "");
        final Version v2_0 = Version.create(2, 0, 0, "");
        final Version v1_9 = Version.create(1, 65536, 65536, "");
        final Version vMin = Version.create(0, 0, 0, "");
        final Version vMax = Version.create(65536, 65536, 65536, "");
        final Version vPen = Version.create(65536, 65536, 65535, "");

        assertGreater(v2_0, v1_0);
        assertGreater(v1_9, v1_0);
        assertGreater(v2_0, v1_9);
        assertGreater(vMax, vMin);
        assertGreater(vMax, vPen);
    }

    @Test
    public void compareTo_tagValue_returnsExpectedValue() {
        final Version b1 = Version.create(1, 0, "b1");
        final Version b2 = Version.create(1, 0, "b2");
        final Version z3 = Version.create(1, 0, "z3");
        final Version snap = Version.create(1, 0, "SNAPSHOT");

        assertGreater(b2, b1);
        assertGreater(z3, b2);
        assertGreater(snap, z3);
    }

    @Test
    public void toString_returnsExpectedValue() {
        assertEquals("1.0", Version.create(1, 0, 0, "").toString());
        assertEquals("1.0-tag", Version.create(1, 0, "tag").toString());
        assertEquals("1.0.1", Version.create(1, 0, 1, "").toString());
    }

    private static <T extends Comparable<T>> void assertGreater(final T lhs, final T rhs) {
        assertTrue(lhs.compareTo(rhs) > 0, lhs + " > " + rhs);
    }
}

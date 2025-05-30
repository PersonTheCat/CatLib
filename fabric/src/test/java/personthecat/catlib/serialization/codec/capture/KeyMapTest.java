package personthecat.catlib.serialization.codec.capture;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeyMapTest {

    @Test
    public void put_updatesSize() {
        final int size = new Random().nextInt(1234);
        final var map = new KeyMap<Integer>();
        for (int i = 0; i < size; i++) {
            map.put(Key.of(String.valueOf(i), Integer.class), i);
        }
        assertEquals(size, map.size());
    }

    @Test
    public void get_byExactKey_returnsValue() {
        final var map = new KeyMap<String>();
        map.put(Key.of("key", String.class), "value");
        assertNotNull(map.get(Key.of("key", String.class)));
    }

    @Test
    public void get_whenLookupIsParent_andOriginalIsChild_returnsValue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Bunny.class), new Bunny());
        assertNotNull(map.get(Key.of("animal", Animal.class)));
    }

    @Test
    public void get_whenLookupIsChild_andOriginalIsParent_returnsNull() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Bunny());
        assertNull(map.get(Key.of("animal", Bunny.class)));
    }

    @Test
    public void get_whenOriginalIsNameless_andLookupIsNamed_returnsValue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of(Key.ANY, Animal.class), new Gerbil());
        assertNotNull(map.get(Key.of("animal", Animal.class)));
    }

    @Test
    public void get_whenOriginalIsNamed_andLookupIsNameless_returnsNull() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Gerbil());
        assertNull(map.get(Key.of(Key.ANY, Animal.class)));
    }

    @Test
    public void get_whenOriginalIsUnqualified_andLookupIsQualified_returnsValue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Bunny());
        assertNotNull(map.get(Key.of(Key.of("parent"), "animal", Animal.class)));
    }

    @Test
    public void get_whenOriginalIsQualified_andLookupIsUnqualified_returnsNull() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of(Key.of("parent"), "animal", Animal.class), new Bunny());
        assertNull(map.get(Key.of("animal", Animal.class)));
    }

    @Test
    public void getOrDefault_byExactKey_returnsValue() {
        final var map = new KeyMap<String>();
        map.put(Key.of("key", String.class), "value");
        assertEquals("value", map.getOrDefault(Key.of("key", String.class), "default"));
    }

    @Test
    public void getOrDefault_whenLookupIsParent_andOriginalIsChild_returnsValue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Bunny.class), new Bunny());
        assertEquals(new Bunny(), map.getOrDefault(Key.of("animal", Animal.class), new Gerbil()));
    }

    @Test
    public void getOrDefault_whenLookupIsChild_andOriginalIsParent_returnsDefault() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Bunny());
        assertEquals(new Gerbil(), map.getOrDefault(Key.of("animal", Bunny.class), new Gerbil()));
    }

    @Test
    public void getOrDefault_whenOriginalIsNameless_andLookupIsNamed_returnsValue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of(Key.ANY, Animal.class), new Bunny());
        assertEquals(new Bunny(), map.getOrDefault(Key.of("animal", Animal.class), new Gerbil()));
    }

    @Test
    public void getOrDefault_whenOriginalIsNamed_andLookupIsNameless_returnsDefault() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Bunny());
        assertEquals(new Gerbil(), map.getOrDefault(Key.of(Key.ANY, Animal.class), new Gerbil()));
    }

    @Test
    public void getOrDefault_whenOriginalIsUnqualified_andLookupIsQualified_returnsValue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Bunny());
        assertEquals(new Bunny(), map.getOrDefault(Key.of(Key.of("parent"), "animal", Animal.class), new Gerbil()));
    }

    @Test
    public void getOrDefault_whenOriginalIsQualified_andLookupIsUnqualified_returnsDefault() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of(Key.of("parent"), "animal", Animal.class), new Bunny());
        assertEquals(new Gerbil(), map.getOrDefault(Key.of("animal", Animal.class), new Gerbil()));
    }

    @Test
    public void containsKey_byExactKey_returnsTrue() {
        final var map = new KeyMap<String>();
        map.put(Key.of("key", String.class), "value");
        assertTrue(map.containsKey(Key.of("key", String.class)));
    }

    @Test
    public void containsKey_whenLookupIsParent_andOriginalIsChild_returnsTrue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Bunny.class), new Bunny());
        assertTrue(map.containsKey(Key.of("animal", Animal.class)));
    }

    @Test
    public void containsKey_whenLookupIsChild_andOriginalIsParent_returnsFalse() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Bunny());
        assertFalse(map.containsKey(Key.of("animal", Bunny.class)));
    }

    @Test
    public void containsKey_whenOriginalIsNameless_andLookupIsNamed_returnsTrue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of(Key.ANY, Animal.class), new Gerbil());
        assertTrue(map.containsKey(Key.of("animal", Animal.class)));
    }

    @Test
    public void containsKey_whenOriginalIsNamed_andLookupIsNameless_returnsFalse() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Gerbil());
        assertFalse(map.containsKey(Key.of(Key.ANY, Animal.class)));
    }

    @Test
    public void containsKey_whenOriginalIsUnqualified_andLookupIsQualified_returnsTrue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Bunny());
        assertTrue(map.containsKey(Key.of(Key.of("parent"), "animal", Animal.class)));
    }

    @Test
    public void containsKey_whenOriginalIsQualified_andLookupIsUnqualified_returnsFalse() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of(Key.of("parent"), "animal", Animal.class), new Bunny());
        assertFalse(map.containsKey(Key.of("animal", Animal.class)));
    }

    @Test
    public void remove_byExactKey_returnsValue() {
        final var map = new KeyMap<String>();
        map.put(Key.of("key", String.class), "value");
        assertNotNull(map.remove(Key.of("key", String.class)));
    }

    @Test
    public void remove_whenLookupIsParent_andOriginalIsChild_returnsValue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Bunny.class), new Bunny());
        assertNotNull(map.remove(Key.of("animal", Animal.class)));
    }

    @Test
    public void remove_whenLookupIsChild_andOriginalIsParent_returnsNull() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Bunny());
        assertNull(map.remove(Key.of("animal", Bunny.class)));
    }

    @Test
    public void remove_whenOriginalIsNameless_andLookupIsNamed_returnsValue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of(Key.ANY, Animal.class), new Gerbil());
        assertNotNull(map.remove(Key.of("animal", Animal.class)));
    }

    @Test
    public void remove_whenOriginalIsNamed_andLookupIsNameless_returnsNull() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Gerbil());
        assertNull(map.remove(Key.of(Key.ANY, Animal.class)));
    }

    @Test
    public void remove_whenOriginalIsUnqualified_andLookupIsQualified_returnsValue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Bunny());
        assertNotNull(map.remove(Key.of(Key.of("parent"), "animal", Animal.class)));
    }

    @Test
    public void remove_whenOriginalIsQualified_andLookupIsUnqualified_returnsNull() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of(Key.of("parent"), "animal", Animal.class), new Bunny());
        assertNull(map.remove(Key.of("animal", Animal.class)));
    }

    @Test
    public void remove_thenGet_returnsNull() {
        final var map = new KeyMap<String>();
        final var key = Key.of("key", String.class);
        map.put(key, "value");
        map.remove(key);
        assertNull(map.get(key));
    }

    @Test
    public void remove_thenGet_updatesSize() {
        final var map = new KeyMap<String>();
        final var key = Key.of("key", String.class);
        map.put(key, "value");
        map.remove(key);
        assertTrue(map.isEmpty());
    }

    @Test
    public void removeEntry_byExactKey_returnsTrue() {
        final var map = new KeyMap<String>();
        map.put(Key.of("key", String.class), "value");
        assertTrue(map.remove(Key.of("key", String.class), "value"));
    }

    @Test
    public void removeEntry_whenLookupIsParent_andOriginalIsChild_returnsTrue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Bunny.class), new Bunny());
        assertTrue(map.remove(Key.of("animal", Animal.class), new Bunny()));
    }

    @Test
    public void removeEntry_whenLookupIsChild_andOriginalIsParent_returnsFalse() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Bunny());
        assertFalse(map.remove(Key.of("animal", Bunny.class), new Bunny()));
    }

    @Test
    public void removeEntry_whenOriginalIsNameless_andLookupIsNamed_returnsTrue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of(Key.ANY, Animal.class), new Gerbil());
        assertTrue(map.remove(Key.of("animal", Animal.class), new Gerbil()));
    }

    @Test
    public void removeEntry_whenOriginalIsNamed_andLookupIsNameless_returnsFalse() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Gerbil());
        assertFalse(map.remove(Key.of(Key.ANY, Animal.class), new Gerbil()));
    }

    @Test
    public void removeEntry_whenOriginalIsUnqualified_andLookupIsQualified_returnsTrue() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of("animal", Animal.class), new Bunny());
        assertTrue(map.remove(Key.of(Key.of("parent"), "animal", Animal.class), new Bunny()));
    }

    @Test
    public void removeEntry_whenOriginalIsQualified_andLookupIsUnqualified_returnsFalse() {
        final var map = new KeyMap<Animal>();
        map.put(Key.of(Key.of("parent"), "animal", Animal.class), new Bunny());
        assertFalse(map.remove(Key.of("animal", Animal.class), new Bunny()));
    }

    @Test
    public void removeEntry_thenGet_returnsNull() {
        final var map = new KeyMap<String>();
        final var key = Key.of("key", String.class);
        map.put(key, "value");
        map.remove(key, "value");
        assertNull(map.get(key));
    }

    @Test
    public void removeEntry_thenGet_updatesSize() {
        final var map = new KeyMap<String>();
        final var key = Key.of("key", String.class);
        map.put(key, "value");
        map.remove(key, "value");
        assertTrue(map.isEmpty());
    }

    @Test
    public void removeEntry_whenValueDoesNotMatch_doesNotRemoveEntry() {
        final var map = new KeyMap<Animal>();
        final var parent = Key.of("animal", Animal.class);
        final var child = Key.of("animal", Bunny.class);
        map.put(child, new Bunny());
        map.remove(parent, new Gerbil());
        assertNotNull(map.get(child));
    }

    interface Animal {}
    record Bunny() implements Animal {}
    record Gerbil() implements Animal {}
}

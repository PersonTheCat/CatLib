package personthecat.catlib.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class LibStringUtilsTest {

    @Test
    public void wrapLines_picksClosetFit() {
        final String text = "one two three\nfour five six\nseven eight nine";

        final List<String> lines1 = LibStringUtils.wrapLines(text, 6);
        assertEquals(asList("one", "two", "three", "four", "five", "six", "seven", "eight", "nine"), lines1);

        final List<String> lines2 = LibStringUtils.wrapLines(text, 12);
        assertEquals(asList("one two", "three four", "five six", "seven eight", "nine"), lines2);
    }

    @Test
    public void wrapLines_ignoresLongWords() {
        final String text = "onomatopoeia pulchritudinous psychotomimetic";

        final List<String> lines = LibStringUtils.wrapLines(text, 3);
        assertEquals(asList("onomatopoeia", "pulchritudinous", "psychotomimetic"), lines);
    }

    @Test
    public void wrapLines_trimsLines() {
        final String text = "one   two   three   four   five   six";

        // Don't necessarily want to test if we're losing any other whitespace.
        for (final String line : LibStringUtils.wrapLines(text, 10)) {
            assertEquals(line.trim(), line);
        }
    }

    @Test
    public void toTitleCase_supportsSnakeCase() {
        assertEquals("One Two Three", LibStringUtils.toTitleCase("one_two_three"));
        assertEquals("Four Five Six", LibStringUtils.toTitleCase("four_five_six"));
        assertEquals("Seven Eight Nine", LibStringUtils.toTitleCase("seven_eight_nine"));
    }

    @Test
    public void toTitleCase_supportsChainedSnakeCase() {
        assertEquals("One Two Three", LibStringUtils.toTitleCase("one-two-three"));
        assertEquals("Four Five Six", LibStringUtils.toTitleCase("four-five-six"));
        assertEquals("Seven Eight Nine", LibStringUtils.toTitleCase("seven-eight-nine"));
    }

    @Test
    public void capitalize_returnsCapitalWord() {
        assertEquals("One two three", LibStringUtils.capitalize("one two three"));
        assertEquals("Four five six", LibStringUtils.capitalize("four five six"));
        assertEquals("Seven eight nine", LibStringUtils.capitalize("seven eight nine"));
    }

    @Test
    public void tokenize_supportsCamelCase() {
        assertEquals(asList("one", "Two", "Three"), LibStringUtils.tokenize("oneTwoThree"));
        assertEquals(asList("four", "Five", "Six"), LibStringUtils.tokenize("fourFiveSix"));
        assertEquals(asList("seven", "Eight", "Nine"), LibStringUtils.tokenize("sevenEightNine"));
    }

    @Test
    public void tokenize_supportsPascalCase() {
        assertEquals(asList("One", "Two", "Three"), LibStringUtils.tokenize("OneTwoThree"));
        assertEquals(asList("Four", "Five", "Six"), LibStringUtils.tokenize("FourFiveSix"));
        assertEquals(asList("Seven", "Eight", "Nine"), LibStringUtils.tokenize("SevenEightNine"));
    }

    @Test
    public void tokenize_supportsCapitalizedAbbreviations() {
        assertEquals(asList("CG", "Is", "A", "Mod"), LibStringUtils.tokenize("CGIsAMod"));
        assertEquals(asList("OSV", "Is", "Too"), LibStringUtils.tokenize("OSVIsToo"));
    }
}

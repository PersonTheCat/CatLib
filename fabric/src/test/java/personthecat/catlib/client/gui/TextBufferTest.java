package personthecat.catlib.client.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextBufferTest {

    @Test
    public void getLines_getsLineStrings() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        final var expected = List.of(
            "line 1",
            "line 2",
            "line 3"
        );
        assertEquals(expected, buffer.getLines());
    }

    @Test
    public void getLine_getsLineString() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        assertEquals("line 2", buffer.getLine(1));
    }

    @Test
    public void lineCount_getsNumLines() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        assertEquals(3, buffer.lineCount());
    }

    @Test
    public void getText_getsFullText() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        assertEquals(text, buffer.getText());
    }

    @Test
    public void moveLeft_whenCursorGt0_movesCursorLeft() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.moveCursorTo(1, 2);
        buffer.moveLeft();

        assertEquals(1, buffer.getCursorCol());
        assertEquals(1, buffer.getCursorRow());
    }

    @Test
    public void moveLeft_whenCursorEq0_movesCursorToEndOfPrevious() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.moveCursorTo(1, 0);
        buffer.moveLeft();

        assertEquals("line 1".length(), buffer.getCursorCol());
        assertEquals(0, buffer.getCursorRow());
    }

    @Test
    public void moveLeft_whenCursorEq0_andRowEq0_doesNotMoveCursor() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.moveCursorTo(0, 0);
        buffer.moveLeft();

        assertEquals(0, buffer.getCursorCol());
        assertEquals(0, buffer.getCursorRow());
    }

    @Test
    public void moveRight_whenCursorLtLen_movesCursorRight() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.moveCursorTo(2, 1);
        buffer.moveRight();

        assertEquals(2, buffer.getCursorCol());
        assertEquals(2, buffer.getCursorRow());
    }

    @Test
    public void moveRight_whenCursorEqLen_movesCursorToBeginningOfNext() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.moveCursorTo(1, "line 2".length());
        buffer.moveRight();

        assertEquals(2, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());
    }

    @Test
    public void moveRight_whenCursorEqLen_andRowEqLen_doesNotMoveCursor() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.moveCursorTo(2, "line 3".length());
        buffer.moveRight();

        assertEquals("line 3".length(), buffer.getCursorCol());
        assertEquals(2, buffer.getCursorRow());
    }
// todo:
//  moveUp when row is 0 should move col to 0
//  moveDown when row is last should move col to last

    @Test
    public void moveUp_whenCursorGt0_movesCursorUp() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.moveCursorTo(1, 0);
        buffer.moveUp();

        assertEquals(0, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());
    }

    @Test
    public void moveUp_whenCursorGt0_andColGtPreviousRowLen_movesCursorToEndOfPrevious() {
        final var text = """
            line 1
            line 2...
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.moveCursorTo(1, "line 2...".length());
        buffer.moveUp();

        assertEquals(0, buffer.getCursorRow());
        assertEquals("line 1".length(), buffer.getCursorCol());
    }

    @Test
    public void moveUp_whenCursorEq0_movesColTo0() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 4);

        buffer.moveUp();

        assertEquals(0, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());
    }

    @Test
    public void moveDown_whenCursorLtLen_movesCursorDown() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.moveCursorTo(1, 0);
        buffer.moveDown();

        assertEquals(2, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());
    }

    @Test
    public void moveDown_whenCursorLtLen_andColGtNextRowLen_movesCursorToEndOfNext() {
        final var text = """
            line 1
            line 2...
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.moveCursorTo(1, "line 2...".length());
        buffer.moveDown();

        assertEquals(2, buffer.getCursorRow());
        assertEquals("line 3".length(), buffer.getCursorCol());
    }

    @Test
    public void moveDown_whenCursorEqLen_movesColToLen() {

    }

    @Test
    public void getSection_whenRangeIsSingleLine_getsLine() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        assertEquals("line 2", buffer.getSection(1, 1, 0, "line 2".length()));
    }

    @Test
    public void getSection_whenRangeIsMultipleLines_getsLines() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        assertEquals("line 1\nline 2", buffer.getSection(0, 1, 0, "line 2".length()));
    }

    @Test
    public void getSection_whenRangeIsBetweenMultipleLines_getsFullTextOfRegion() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        assertEquals(" 1\nline", buffer.getSection(0, 1, "line".length(), "line".length()));
    }

    @Test
    public void deleteSection_whenRangeIsSingleLine_deletesLine() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.deleteSection(1, 1, 0, "line 2".length());

        final var expected = """
            line 1
            
            line 3""";

        assertEquals(expected, buffer.getText());
    }

    @Test
    public void deleteSection_whenRangeIsMultipleLines_deletesMultipleLines() {
        final var text = """
            line 1
            line 2
            line 3
            line 4""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.deleteSection(1, 2, 0, "line 3".length());

        final var expected = """
            line 1
            
            line 4""";

        assertEquals(expected, buffer.getText());
    }

    @Test
    public void deleteSection_whenRangeIsBetweenLines_deletesFullTextOfRegion() {
        final var text = """
            a 1
            b 2
            c 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.deleteSection(0, 2, "a ".length(), "c ".length());

        assertEquals("a 3", buffer.getText());
    }

    @Test
    public void deleteSection_movesCursorToStartOfRegion() {
        final var text = """
            abc 123
            def 456
            ghi 789""";
        final var buffer = new TextBuffer();
        buffer.setText(text);

        buffer.deleteSection(1, 2, 3, 4);

        assertEquals(1, buffer.getCursorRow());
        assertEquals(3, buffer.getCursorCol());
    }

    @Test
    public void insertText_whenTextIsOneLine_updatesCurrentLine() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 4);

        buffer.insertText("no newlines for this test");

        assertEquals("0123no newlines for this test456789", buffer.getText());
    }

    @Test
    public void insertText_whenTextIsMultipleLines_doesAppendAllLines() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 4);

        buffer.insertText("line 1\nline 2");

        assertEquals("0123line 1\nline 2456789", buffer.getText());
    }

    @Test
    public void insertText_whenIsOneLine_movesCursorToEndOfInput() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 4);

        buffer.insertText("no newlines for this test");

        // cursor goes here ----------------------->
        // expected = "0123no newlines for this test456789"

        assertEquals(0, buffer.getCursorRow());
        assertEquals("0123no newlines for this test".length(), buffer.getCursorCol());
    }

    @Test
    public void insertText_whenTextIsMultipleLines_movesCursorToEndOfInput() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 4);

        buffer.insertText("line 1\nline 2");

        // cursor goes here ------------>
        // expected = "0123line 1\nline 2456789"

        assertEquals(1, buffer.getCursorRow());
        assertEquals("line 2".length(), buffer.getCursorCol());
    }

    @Test
    public void insertChar_insertsCharacterAtCursor() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 4);

        buffer.insertChar('c');

        assertEquals("0123c456789", buffer.getText());
    }

    @Test
    public void insertChar_movesCursorForward() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 4);

        buffer.insertChar('c');

        assertEquals(0, buffer.getCursorRow());
        assertEquals(5, buffer.getCursorCol());
    }

    @Test
    public void insertNewline_whenCursorIsAtEndOfLine_addsNewLine() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, "line 1".length());

        buffer.insertNewline();

        final var expected = """
            line 1
            
            line 2
            line 3""";

        assertEquals(expected, buffer.getText());
    }

    @Test
    public void insertNewline_whenCursorIsAtEndOfLine_movesCursorToNextLine() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, "line 1".length());

        buffer.insertNewline();

        assertEquals(1, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());
    }

    @Test
    public void insertNewline_whenCursorIsMidLine_movesRestOfLineToNext() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, "line".length());

        buffer.insertNewline();

        final var expected = """
            line
             1
            line 2
            line 3""";

        assertEquals(expected, buffer.getText());
    }

    @Test
    public void insertNewline_whenCursorIsMidLine_movesCursorToNextLine() {
        final var text = """
            line 1
            line 2
            line 3""";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, "line".length());

        buffer.insertNewline();

        assertEquals(1, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());
    }

    @Test
    public void backspace_whenColGt0_deletesLastCharacter_beforeCursor() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 4);

        buffer.backspace();

        assertEquals("012456789", buffer.getText());
    }

    @Test
    public void backspace_whenColGt0_movesCursorBackOnce() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 4);

        buffer.backspace();

        assertEquals(0, buffer.getCursorRow());
        assertEquals(3, buffer.getCursorCol());
    }

    @Test
    public void backspace_whenColEq0_movesLineToPreviousRow() {
        final var text = """
            line 1
            line 2""";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(1, 0);

        buffer.backspace();

        assertEquals("line 1line 2", buffer.getText());
    }

    @Test
    public void backspace_whenColEq0_movesCursorToEndOfPreviousLine() {
        final var text = """
            line 1
            line 2""";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(1, 0);

        buffer.backspace();

        assertEquals(0, buffer.getCursorRow());
        assertEquals("line 1".length(), buffer.getCursorCol());
    }

    @Test
    public void backspace_atBeginningOfText_doesNotUpdateText() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 0);

        buffer.backspace();

        assertEquals(text, buffer.getText());
    }

    @Test
    public void backspace_atBeginningOfText_doesNotUpdateCursor() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 0);

        buffer.backspace();

        assertEquals(0, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());
    }

    @Test
    public void delete_beforeEndOfLine_removesNextCharacter() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, 4);

        buffer.delete();

        assertEquals("012356789", buffer.getText());
    }

    @Test
    public void delete_atEndOfLine_movesNextLineToCurrent() {
        final var text = """
            line 1
            line 2""";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, "line 1".length());

        buffer.delete();

        assertEquals("line 1line 2", buffer.getText());
    }

    @Test
    public void delete_atEndOfText_doesNotUpdateText() {
        final var text = "0123456789";
        final var buffer = new TextBuffer();
        buffer.setText(text);
        buffer.moveCursorTo(0, text.length());

        buffer.delete();

        assertEquals(text, buffer.getText());
    }
}

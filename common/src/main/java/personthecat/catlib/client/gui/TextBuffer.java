package personthecat.catlib.client.gui;

import java.util.ArrayList;
import java.util.List;

public class TextBuffer {
    private final List<StringBuilder> lines;
    private int cursorRow;
    private int cursorCol;

    public TextBuffer() {
        this.lines = new ArrayList<>();
        this.cursorRow = 0;
        this.cursorCol = 0;
    }

    public List<String> getLines() {
        return this.lines.stream().map(StringBuilder::toString).toList();
    }

    public String getLine(int idx) {
        return this.lines.get(idx).toString();
    }

    public int lineCount() {
        return this.lines.size();
    }

    public int maxCharacterWidth() {
        return this.lines.stream().mapToInt(StringBuilder::length).max().orElse(0);
    }

    public String getText() {
        return String.join("\n", this.getLines());
    }

    public void setText(final String text) {
        this.lines.clear();
        for (final var line : text.split("\r?\n")) {
            this.lines.add(new StringBuilder(line));
        }
        if (this.lines.isEmpty()) this.lines.add(new StringBuilder());
    }

    public void moveLeft() {
        if (this.cursorCol > 0) {
            this.cursorCol--;
        } else if (this.cursorRow > 0) {
            this.cursorRow--;
            this.cursorCol = this.lines.get(this.cursorRow).length();
        }
    }

    public void moveRight() {
        final var line = this.lines.get(this.cursorRow);
        if (this.cursorCol < line.length()) {
            this.cursorCol++;
        } else if (this.cursorRow < this.lines.size() - 1) {
            this.cursorRow++;
            this.cursorCol = 0;
        }
    }

    public void moveUp() {
        if (this.cursorRow > 0) {
            this.cursorRow--;
            this.cursorCol = Math.min(this.cursorCol, this.lines.get(this.cursorRow).length());
        } else {
            this.cursorCol = 0;
        }
    }

    public void moveDown() {
        if (this.cursorRow < this.lines.size() - 1) {
            this.cursorRow++;
            this.cursorCol = Math.min(this.cursorCol, this.lines.get(this.cursorRow).length());
        } else {
            this.cursorCol = this.lines.get(this.cursorRow).length();
        }
    }

    public String getSection(int startRow, int endRow, int startCol, int endCol) {
        if (startRow == endRow) {
            return this.lines.get(startRow).substring(startCol, endCol);
        }
        final var sb = new StringBuilder();
        final var first = this.lines.get(startRow);
        sb.append(first, startCol, first.length()).append('\n');
        for (int i = startRow + 1; i < endRow; i++) {
            sb.append(this.lines.get(i)).append('\n');
        }
        final var last = this.lines.get(endRow);
        sb.append(last, 0, endCol);
        return sb.toString();
    }

    public void deleteSection(int startRow, int endRow, int startCol, int endCol) {
        if (startRow == endRow) {
            this.lines.get(startRow).delete(startCol, endCol);
            return;
        }
        final var first = this.lines.get(startRow);
        first.delete(startCol, first.length());
        final var last = this.lines.get(endRow);
        if (endCol < last.length()) {
            first.append(last, endCol, last.length());
        } else {
            last.delete(0, endCol);
        }
        for (int i = startRow; i < endRow; i++) {
            this.lines.remove(startRow + 1);
        }
        this.cursorRow = startRow;
        this.cursorCol = startCol;
    }

    public void insertText(String text) {
        final var split = text.split("\r?\n");
        final var first = this.lines.get(this.cursorRow);
        final var after = first.substring(this.cursorCol);
        first.setLength(this.cursorCol);
        first.append(split[0]);
        for (int i = split.length - 1; i > 0; i--) {
            this.lines.add(this.cursorRow + 1, new StringBuilder(split[i]));
        }
        this.lines.get(this.cursorRow + split.length - 1).append(after);
        if (split.length == 1) {
            this.cursorCol += split[0].length();
        } else {
            this.cursorCol = split[split.length - 1].length();
        }
        this.cursorRow += split.length - 1;
    }

    public void insertChar(char c) {
        this.lines.get(this.cursorRow).insert(this.cursorCol, c);
        this.cursorCol++;
    }

    public void insertNewline() {
        final var current = this.lines.get(this.cursorRow);
        final var remainder = current.substring(this.cursorCol);
        current.delete(this.cursorCol, current.length());
        this.lines.add(this.cursorRow + 1, new StringBuilder(remainder));
        this.cursorRow++;
        this.cursorCol = 0;
    }

    public void backspace() {
        if (this.cursorCol > 0) {
            this.lines.get(this.cursorRow).deleteCharAt(this.cursorCol - 1);
            this.cursorCol--;
        } else if (this.cursorRow > 0) {
            final var prev = this.lines.get(this.cursorRow - 1);
            final var current = this.lines.remove(this.cursorRow);
            this.cursorRow--;
            this.cursorCol = prev.length();
            prev.append(current);
        }
    }

    public void delete() {
        final var line = this.lines.get(this.cursorRow);
        if (this.cursorCol < line.length()) {
            line.deleteCharAt(this.cursorCol);
        } else if (this.cursorRow < this.lines.size() - 1) {
            final var next = this.lines.remove(this.cursorRow + 1);
            line.append(next);
        }
    }

    public int getCursorRow() {
        return cursorRow;
    }

    public int getCursorCol() {
        return cursorCol;
    }

    public void moveCursorTo(int row, int col) {
        this.cursorRow = row;
        this.cursorCol = col;
    }
}

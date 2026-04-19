package ch.ti.gagi.xlseditor.ui;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Wave 0 stub — enable after OccurrenceHighlighter is implemented in Wave 1")
class OccurrenceHighlighterTest {

    @Test
    void findOccurrencesReturnsEmptyForBlankToken() {
        List<int[]> result = OccurrenceHighlighter.findOccurrences("some text $var", "");
        assertTrue(result.isEmpty(), "Blank token must return empty list, not throw");
    }

    @Test
    void findOccurrencesReturnsEmptyForSingleCharToken() {
        // Guard: tokens shorter than 2 chars are too noisy; skip highlighting
        List<int[]> result = OccurrenceHighlighter.findOccurrences("$x is here", "$x");
        assertTrue(result.isEmpty(), "Single-char token must return empty (length < 2 guard)");
    }

    @Test
    void findOccurrencesLocatesAllMatches() {
        String text = "$var foo $var";
        List<int[]> hits = OccurrenceHighlighter.findOccurrences(text, "$var");
        assertEquals(2, hits.size(), "Must find exactly 2 occurrences of '$var'");
        assertArrayEquals(new int[]{0, 4}, hits.get(0), "First hit: [0, 4]");
        assertArrayEquals(new int[]{9, 13}, hits.get(1), "Second hit: [9, 13]");
    }
}

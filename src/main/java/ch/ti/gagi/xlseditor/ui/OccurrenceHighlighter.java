package ch.ti.gagi.xlseditor.ui;

import org.fxmisc.richtext.CodeArea;

import java.util.List;

/**
 * Finds and highlights all occurrences of a token within a CodeArea's text.
 * All methods are static; this class is not instantiable.
 * Implementation will be added in Wave 1.
 */
public final class OccurrenceHighlighter {

    private OccurrenceHighlighter() {}

    /**
     * Returns a list of [start, end] index pairs for every occurrence of {@code token}
     * in {@code text}. Returns an empty list if token is blank or shorter than 2 characters.
     * Wave 1: implement with regex or String.indexOf scan.
     */
    public static List<int[]> findOccurrences(String text, String token) {
        // Skeleton: returns empty list until Wave 1 implements the scan
        return List.of();
    }

    /**
     * Applies occurrence highlight style spans to the given CodeArea for the selected token.
     * Wave 1: implement overlay highlighting.
     */
    public static void applyTo(CodeArea codeArea, String token) {
        // Skeleton: no-op until Wave 1
    }
}

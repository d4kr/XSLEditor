package ch.ti.gagi.xlseditor.ui;

import org.fxmisc.richtext.CodeArea;

import java.util.List;

/**
 * Provides static XSL/XSL-FO keyword autocomplete support for a RichTextFX CodeArea.
 * All methods are static; this class is not instantiable.
 * Implementation will be added in Wave 1.
 */
public final class AutocompleteProvider {

    private AutocompleteProvider() {}

    /**
     * Returns the list of keyword completions whose prefix matches the given string.
     * Empty prefix returns the full keyword list.
     * Wave 1: replace with real keyword list and filter logic.
     */
    public static List<String> getMatches(String prefix) {
        // Skeleton: returns empty list until Wave 1 implements the keyword table
        return List.of();
    }

    /**
     * Triggers the autocomplete popup at the current caret position in the given CodeArea.
     * Wave 1: implement popup display.
     */
    public static void triggerAt(CodeArea codeArea) {
        // Skeleton: no-op until Wave 1
    }
}

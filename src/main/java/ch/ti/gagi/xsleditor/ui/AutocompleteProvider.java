package ch.ti.gagi.xsleditor.ui;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.fxmisc.richtext.CodeArea;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pure static utility: static XSL/XSL-FO keyword list + ContextMenu trigger.
 * getMatches() is pure Java (testable without JavaFX toolkit).
 * triggerAt() is JavaFX UI code called from EditorController.buildTab().
 */
public final class AutocompleteProvider {

    private AutocompleteProvider() {}

    /** Static XSL/XSL-FO keyword list — hardcoded per RESEARCH.md Pattern 3. */
    private static final List<String> KEYWORDS = List.of(
        // XSLT core
        "xsl:template",
        "xsl:apply-templates",
        "xsl:call-template",
        "xsl:value-of",
        "xsl:for-each",
        "xsl:if",
        "xsl:choose",
        "xsl:when",
        "xsl:otherwise",
        "xsl:variable",
        "xsl:param",
        "xsl:with-param",
        "xsl:include",
        "xsl:import",
        "xsl:output",
        "xsl:strip-space",
        "xsl:preserve-space",
        "xsl:text",
        "xsl:copy",
        "xsl:copy-of",
        "xsl:number",
        "xsl:sort",
        "xsl:element",
        "xsl:attribute",
        "xsl:comment",
        "xsl:processing-instruction",
        "xsl:message",
        // XSL-FO
        "fo:root",
        "fo:layout-master-set",
        "fo:simple-page-master",
        "fo:page-sequence",
        "fo:flow",
        "fo:static-content",
        "fo:block",
        "fo:inline",
        "fo:table",
        "fo:table-body",
        "fo:table-row",
        "fo:table-cell",
        "fo:list-block",
        "fo:list-item",
        "fo:list-item-label",
        "fo:list-item-body",
        "fo:external-graphic",
        "fo:basic-link"
    );

    /**
     * Returns keywords that start with the given prefix (case-sensitive).
     * Returns the full list for an empty prefix.
     * Never returns null; returns an empty list for an unmatched prefix.
     *
     * @param prefix the text typed before the caret (may be empty)
     * @return filtered keyword list
     */
    public static List<String> getMatches(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return KEYWORDS;
        }
        return KEYWORDS.stream()
            .filter(k -> k.startsWith(prefix))
            .collect(Collectors.toList());
    }

    /**
     * Shows an autocomplete ContextMenu anchored to the caret position in the
     * given CodeArea. Selecting a menu item inserts the keyword (replacing the
     * typed prefix). Called from EditorController.buildTab() on the FX thread.
     *
     * @param area the CodeArea to anchor the popup to
     */
    public static void triggerAt(CodeArea area) {
        String prefix = extractPrefixBeforeCaret(area);
        List<String> matches = getMatches(prefix);
        if (matches.isEmpty()) return;

        ContextMenu menu = new ContextMenu();
        for (String keyword : matches) {
            MenuItem item = new MenuItem(keyword);
            item.setOnAction(e -> insertCompletion(area, prefix, keyword));
            menu.getItems().add(item);
        }

        // getCaretBounds() returns bounds already in screen coordinate space.
        // Calling localToScreen() would double-transform the coordinates. Use bounds directly.
        area.getCaretBounds().ifPresent(b -> {
            menu.show(area, b.getMinX(), b.getMaxY());
        });
    }

    private static String extractPrefixBeforeCaret(CodeArea area) {
        int caretPos = area.getCaretPosition();
        if (caretPos == 0) return "";
        String text = area.getText(0, caretPos);
        // Walk back to find the start of the current token (stop at whitespace or '>')
        int start = caretPos;
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (Character.isWhitespace(c) || c == '>' || c == '<') break;
            start--;
        }
        return text.substring(start, caretPos);
    }

    private static void insertCompletion(CodeArea area, String prefix, String keyword) {
        int caretPos = area.getCaretPosition();
        int replaceStart = caretPos - prefix.length();
        if (replaceStart < 0) replaceStart = 0;
        area.replaceText(replaceStart, caretPos, keyword);
    }
}

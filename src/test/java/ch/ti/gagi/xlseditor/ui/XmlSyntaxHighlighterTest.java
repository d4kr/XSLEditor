package ch.ti.gagi.xlseditor.ui;

import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class XmlSyntaxHighlighterTest {

    @Test
    void computeHighlightingHandlesEmptyInput() {
        StyleSpans<Collection<String>> result = XmlSyntaxHighlighter.computeHighlighting("");
        assertEquals(0, result.length(),
            "Empty input must produce zero-length StyleSpans, not throw");
    }

    @Test
    void totalSpanLengthEqualsInputLength() {
        String text = "<xsl:template match=\"/\"><!-- comment --></xsl:template>";
        StyleSpans<Collection<String>> spans = XmlSyntaxHighlighter.computeHighlighting(text);
        assertEquals(text.length(), spans.length(),
            "Sum of all span lengths must equal text.length() (guards StyleSpansBuilder crash)");
    }

    @Test
    void computeHighlightingReturnsCommentClass() {
        String text = "<!-- hello -->";
        StyleSpans<Collection<String>> spans = XmlSyntaxHighlighter.computeHighlighting(text);
        boolean hasComment = false;
        for (var span : spans) {
            if (span.getStyle().contains("xml-comment")) { hasComment = true; break; }
        }
        assertTrue(hasComment, "Comment text must map to 'xml-comment' CSS class");
    }

    @Test
    void computeHighlightingReturnsTagmarkForElement() {
        String text = "<xsl:template>";
        StyleSpans<Collection<String>> spans = XmlSyntaxHighlighter.computeHighlighting(text);
        boolean hasTagOrElement = false;
        for (var span : spans) {
            Collection<String> styles = span.getStyle();
            if (styles.contains("xml-tagmark") || styles.contains("xml-element")) {
                hasTagOrElement = true; break;
            }
        }
        assertTrue(hasTagOrElement, "Element tag must map to 'xml-tagmark' or 'xml-element' CSS class");
    }
}

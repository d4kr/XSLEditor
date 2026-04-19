package ch.ti.gagi.xlseditor.ui;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;

/**
 * Computes XML/XSLT syntax highlighting spans for a RichTextFX CodeArea.
 * All methods are static; this class is not instantiable.
 * Implementation will be added in Wave 1.
 */
public final class XmlSyntaxHighlighter {

    private XmlSyntaxHighlighter() {}

    /**
     * Computes syntax-highlighting StyleSpans for the given text.
     * Returns a zero-length span collection for empty input.
     * Wave 1: replace with regex-based XML highlighter.
     */
    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        if (text == null || text.isEmpty()) {
            builder.add(Collections.emptyList(), 0);
            return builder.create();
        }
        // Skeleton: single unstyled span covering full text (Wave 1 replaces this)
        builder.add(Collections.emptyList(), text.length());
        return builder.create();
    }
}

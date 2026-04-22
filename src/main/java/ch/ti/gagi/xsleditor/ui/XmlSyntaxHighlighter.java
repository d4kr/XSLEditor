package ch.ti.gagi.xsleditor.ui;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure static utility: computes RichTextFX StyleSpans for XML/XSLT source text.
 * All CSS class names must match declarations in main.css (Phase 5 section).
 *
 * Called from EditorController.buildTab() via an ExecutorService Task — never on
 * the JavaFX Application Thread directly.
 */
public final class XmlSyntaxHighlighter {

    private XmlSyntaxHighlighter() {}  // no instantiation

    // Top-level XML token pattern (order matters: COMMENT and CDATA first to avoid
    // partial matches inside those regions).
    private static final Pattern XML_TAG = Pattern.compile(
        "(?<COMMENT><!--[\\s\\S]*?-->)"
        + "|(?<CDATA><!\\[CDATA\\[[\\s\\S]*?\\]\\]>)"
        + "|(?<PI><\\?[\\s\\S]*?\\?>)"
        + "|(?<ELEMENT>(?<OPEN></?)"
            + "(?<ELEMNAME>[\\w][\\w:-]*)"
            + "(?<ATTRS>(?:[^<>\"'/]|\"[^\"]*\"|'[^']*'|/(?!>))*)"
            + "(?<CLOSE>/?>))"
    );

    // Attribute pattern applied inside the ATTRS capture group
    private static final Pattern ATTR_PATTERN = Pattern.compile(
        "(?<ATTRNAME>[\\w][\\w:-]*)\\s*(?<EQ>=)\\s*(?<AVALUE>\"[^\"]*\"|'[^']*')"
    );

    /**
     * Computes syntax-highlighting StyleSpans for the given XML/XSLT text.
     * The total length of the returned StyleSpans always equals text.length().
     * Safe to call with empty or null text — returns zero-length spans.
     *
     * @param text raw document text
     * @return StyleSpans mapping character ranges to CSS class collections
     */
    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        if (text == null || text.isEmpty()) {
            StyleSpansBuilder<Collection<String>> empty = new StyleSpansBuilder<>();
            empty.add(Collections.emptyList(), 0);
            return empty.create();
        }

        Matcher matcher = XML_TAG.matcher(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastKwEnd = 0;

        while (matcher.find()) {
            // Plain text before this match
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);

            if (matcher.group("COMMENT") != null) {
                spansBuilder.add(Collections.singleton("xml-comment"),
                    matcher.end() - matcher.start());

            } else if (matcher.group("CDATA") != null) {
                spansBuilder.add(Collections.singleton("xml-cdata"),
                    matcher.end() - matcher.start());

            } else if (matcher.group("PI") != null) {
                spansBuilder.add(Collections.singleton("xml-pi"),
                    matcher.end() - matcher.start());

            } else {
                // ELEMENT: break it into sub-spans for tagmark / element-name / attrs / close
                int elemStart = matcher.start();
                String openMark  = matcher.group("OPEN");    // "<" or "</"
                String elemName  = matcher.group("ELEMNAME");
                String attrsText = matcher.group("ATTRS");
                String closeMark = matcher.group("CLOSE");   // ">" or "/>"

                int cursor = elemStart;

                // "<" or "</"
                spansBuilder.add(Collections.singleton("xml-tagmark"), openMark.length());
                cursor += openMark.length();

                // element name (e.g. "xsl:template")
                spansBuilder.add(Collections.singleton("xml-element"), elemName.length());
                cursor += elemName.length();

                // attributes block: walk with ATTR_PATTERN
                if (attrsText != null && !attrsText.isEmpty()) {
                    Matcher attrMatcher = ATTR_PATTERN.matcher(attrsText);
                    int attrLast = 0;
                    while (attrMatcher.find()) {
                        // plain whitespace / punctuation before attribute
                        spansBuilder.add(Collections.emptyList(),
                            attrMatcher.start() - attrLast);
                        // attribute name
                        spansBuilder.add(Collections.singleton("xml-attribute"),
                            attrMatcher.group("ATTRNAME").length());
                        // "="
                        spansBuilder.add(Collections.emptyList(), 1); // the '=' character
                        // attribute value
                        spansBuilder.add(Collections.singleton("xml-avalue"),
                            attrMatcher.group("AVALUE").length());
                        attrLast = attrMatcher.end();
                    }
                    // remainder of attrs (trailing whitespace etc.)
                    spansBuilder.add(Collections.emptyList(),
                        attrsText.length() - attrLast);
                    cursor += attrsText.length();
                }

                // ">" or "/>"
                spansBuilder.add(Collections.singleton("xml-tagmark"), closeMark.length());
                // cursor not needed after last segment
            }

            lastKwEnd = matcher.end();
        }

        // Trailing plain text (Pitfall 1: always add trailing span)
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}

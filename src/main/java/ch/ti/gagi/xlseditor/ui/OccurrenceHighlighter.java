package ch.ti.gagi.xlseditor.ui;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure static utility: finds all occurrences of a selected token in document text.
 * findOccurrences() is pure Java — testable without JavaFX toolkit.
 * applyTo() is JavaFX UI code called from EditorController.buildTab().
 */
public final class OccurrenceHighlighter {

    private OccurrenceHighlighter() {}

    /**
     * Finds all occurrences of the given token in text.
     * Returns each occurrence as an int[] {start, end} (end is exclusive).
     *
     * Guards:
     * - Returns empty list for null, blank, or single-char tokens (too noisy)
     * - Uses Pattern.quote() so user-selected text with regex special chars is safe
     *
     * @param text  the full document text
     * @param token the selected text to find
     * @return list of [start, end] pairs; never null
     */
    public static List<int[]> findOccurrences(String text, String token) {
        if (text == null || token == null || token.isBlank() || token.length() < 2) {
            return List.of();
        }
        List<int[]> hits = new ArrayList<>();
        Matcher m = Pattern.compile(Pattern.quote(token)).matcher(text);
        while (m.find()) {
            hits.add(new int[]{m.start(), m.end()});
        }
        return Collections.unmodifiableList(hits);
    }

    /**
     * Applies occurrence highlights to the given CodeArea for the selected token.
     *
     * A1 RESOLVED: setStyle(start, end, style) REPLACES — does not merge CSS classes.
     * Fix: build a single merged StyleSpans that overlays "occurrence" onto the base
     * syntax spans, then apply with a single setStyleSpans() call.
     *
     * Called from EditorController.buildTab() selectedTextProperty listener on the FX thread.
     *
     * @param area  the CodeArea to apply highlights to
     * @param token the selected text (may be empty — clears highlights and returns)
     */
    public static void applyTo(CodeArea area, String token) {
        String text = area.getText();
        // Compute base syntax spans
        StyleSpans<Collection<String>> base = XmlSyntaxHighlighter.computeHighlighting(text);
        if (token == null || token.isEmpty()) {
            area.setStyleSpans(0, base);
            return;
        }
        List<int[]> hits = findOccurrences(text, token);
        if (hits.isEmpty()) {
            area.setStyleSpans(0, base);
            return;
        }
        // Merge: overlay "occurrence" class onto matching ranges in base spans
        StyleSpans<Collection<String>> merged = overlayOccurrences(base, hits);
        area.setStyleSpans(0, merged);
    }

    /**
     * Builds a merged StyleSpans that overlays the "occurrence" CSS class onto any
     * character range that falls within one of the given hit intervals.
     *
     * The base syntax classes are preserved at all positions; "occurrence" is added
     * (via Set union) only at matched ranges.
     */
    private static StyleSpans<Collection<String>> overlayOccurrences(
            StyleSpans<Collection<String>> base, List<int[]> hits) {
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int pos = 0;
        for (StyleSpan<Collection<String>> span : base) {
            int end = pos + span.getLength();
            // Split this span by any hit boundaries that fall within [pos, end)
            int cursor = pos;
            for (int[] hit : hits) {
                int hStart = Math.max(hit[0], cursor);
                int hEnd   = Math.min(hit[1], end);
                if (hStart >= hEnd) continue;
                if (cursor < hStart) {
                    builder.add(span.getStyle(), hStart - cursor);
                }
                Set<String> mergedSet = new HashSet<>(span.getStyle());
                mergedSet.add("occurrence");
                builder.add(Collections.unmodifiableSet(mergedSet), hEnd - hStart);
                cursor = hEnd;
            }
            if (cursor < end) {
                builder.add(span.getStyle(), end - cursor);
            }
            pos = end;
        }
        return builder.create();
    }
}

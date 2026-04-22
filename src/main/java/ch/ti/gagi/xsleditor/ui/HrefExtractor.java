package ch.ti.gagi.xlseditor.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure static utility: locates the href attribute value of an xsl:include or
 * xsl:import element at a given character position, then resolves and validates
 * the resulting file path.
 *
 * Security: always rejects hrefs that resolve outside the current file's parent
 * directory tree (path traversal guard per RESEARCH.md Security section).
 */
public final class HrefExtractor {

    private HrefExtractor() {}

    /**
     * Pattern matches xsl:include or xsl:import with an href attribute.
     * Named group HREF captures the raw attribute value (without quotes).
     * HREF start/end positions allow checking if charIndex falls inside the value.
     */
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
        "<xsl:(?:include|import)\\b[^>]*?\\bhref=['\"](?<HREF>[^'\"]*)['\"][^>]*/?>",
        Pattern.DOTALL
    );

    /**
     * Extracts the href value at the given character index and resolves it
     * relative to currentFilePath.
     *
     * Returns Optional.empty() if:
     * - charIndex is not inside any href attribute value of xsl:include/xsl:import
     * - The resolved path does not exist on disk
     * - The resolved path escapes the parent directory (path traversal)
     *
     * @param text            full document text
     * @param charIndex       insertion index from CodeArea.hit()
     * @param currentFilePath absolute path of the file being edited
     * @return resolved absolute path wrapped in Optional, or Optional.empty()
     */
    public static Optional<Path> extractHref(String text, int charIndex, Path currentFilePath) {
        if (text == null || text.isEmpty() || currentFilePath == null) {
            return Optional.empty();
        }

        Matcher m = INCLUDE_PATTERN.matcher(text);
        while (m.find()) {
            // m.start("HREF") and m.end("HREF") give indices of the value content
            // (between the quotes).
            int hrefStart = m.start("HREF");
            int hrefEnd   = m.end("HREF");

            if (charIndex >= hrefStart && charIndex <= hrefEnd) {
                String hrefValue = m.group("HREF");
                if (hrefValue == null || hrefValue.isBlank()) return Optional.empty();

                // Resolve relative to the current file's parent directory
                Path parentDir = currentFilePath.getParent();
                if (parentDir == null) return Optional.empty();

                Path resolved;
                try {
                    resolved = parentDir.resolve(hrefValue).normalize();
                } catch (Exception e) {
                    return Optional.empty();
                }

                // Security: path traversal guard
                // Reject if resolved path does not start with parentDir.normalize()
                Path normalizedParent = parentDir.normalize();
                if (!resolved.startsWith(normalizedParent)) {
                    return Optional.empty();
                }

                // File must exist
                if (!Files.exists(resolved)) {
                    return Optional.empty();
                }

                return Optional.of(resolved);
            }
        }
        return Optional.empty();
    }
}

package ch.ti.gagi.xlseditor.ui;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Extracts and resolves href attribute values from XSL include/import elements.
 * Provides path traversal protection. All methods are static; not instantiable.
 * Implementation will be added in Wave 1.
 */
public final class HrefExtractor {

    private HrefExtractor() {}

    /**
     * Extracts the href value at the given character index in {@code text} and resolves it
     * relative to {@code currentFilePath}. Returns {@link Optional#empty()} when:
     * <ul>
     *   <li>The character at {@code charIndex} is not inside an href attribute value.</li>
     *   <li>The resolved path escapes the parent directory (path traversal).</li>
     *   <li>The resolved file does not exist on disk.</li>
     * </ul>
     * Wave 1: implement with regex href detection and Path resolution.
     */
    public static Optional<Path> extractHref(String text, int charIndex, Path currentFilePath) {
        // Skeleton: always returns empty until Wave 1 implements href parsing
        return Optional.empty();
    }
}

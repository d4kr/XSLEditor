package ch.ti.gagi.xsleditor.ui;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable data carrier for a tree node: the file path (relative to the
 * project root) plus the semantic role that drives the visual glyph and
 * accent color in {@link FileItemTreeCell}.
 *
 * Record semantics: equals compares path AND role, so the TreeView detects
 * role changes (e.g. previously REGULAR file becoming ENTRYPOINT) and
 * refreshes the cell even when the Path is unchanged.
 *
 * Phase 3 / TREE-01..03
 */
public record FileItem(Path path, FileRole role) {

    public FileItem {
        Objects.requireNonNull(path,  "path must not be null");
        Objects.requireNonNull(role,  "role must not be null");
    }

    /**
     * Role tag driving the cell's glyph and CSS class.
     *
     *   ENTRYPOINT — the project's entrypoint XSLT (TREE-02). Renders with
     *                '\u25B6 ' prefix and .entrypoint accent color.
     *   XML_INPUT  — the project's XML input (TREE-03). Renders with
     *                '\u25A0 ' prefix and .xml-input accent color.
     *   REGULAR    — any other file in the project root. Renders with
     *                '\u25A1 ' prefix and the default cell color.
     */
    public enum FileRole { ENTRYPOINT, XML_INPUT, REGULAR }
}

package ch.ti.gagi.xlseditor.ui;

import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;

/**
 * Custom TreeCell renderer for {@link FileItem} nodes.
 *
 * Rendering rules (locked by 03-UI-SPEC.md):
 *   - ENTRYPOINT: prefix '\u25B6 ' (filled right triangle), class 'entrypoint' → #66bb6a
 *   - XML_INPUT : prefix '\u25A0 ' (filled square),         class 'xml-input' → #64b5f6
 *   - REGULAR   : prefix '\u25A1 ' (empty square),          default color    → #cccccc
 *
 * Tooltips (from 03-UI-SPEC § Copywriting Contract):
 *   - ENTRYPOINT → "Entrypoint XSLT"
 *   - XML_INPUT  → "XML Input"
 *   - REGULAR    → null (no tooltip)
 *
 * CSS classes consumed here are defined in main.css under '.file-tree-view'.
 *
 * Important: JavaFX reuses TreeCell instances on scroll. updateItem MUST
 * clear the previous style classes before applying new ones, otherwise a
 * recycled cell keeps stale accent colors.
 *
 * Phase 3 / TREE-02, TREE-03
 */
public final class FileItemTreeCell extends TreeCell<FileItem> {

    // Unicode glyphs — kept as constants for grep-friendly verification
    private static final String GLYPH_ENTRYPOINT = "\u25B6 "; // ▶ filled right triangle
    private static final String GLYPH_XML_INPUT  = "\u25A0 "; // ■ filled square
    private static final String GLYPH_REGULAR    = "\u25A1 "; // □ empty square

    private static final String CSS_CLASS_ENTRYPOINT = "entrypoint";
    private static final String CSS_CLASS_XML_INPUT  = "xml-input";

    private static final String TOOLTIP_ENTRYPOINT = "Entrypoint XSLT";
    private static final String TOOLTIP_XML_INPUT  = "XML Input";

    @Override
    protected void updateItem(FileItem item, boolean empty) {
        super.updateItem(item, empty); // JavaFX contract: must be first line

        // Always clear previous role-specific classes to prevent stale styling
        // on recycled cells (TreeCell is pooled across virtualized rows).
        getStyleClass().removeAll(CSS_CLASS_ENTRYPOINT, CSS_CLASS_XML_INPUT);

        if (empty || item == null) {
            setText(null);
            setTooltip(null);
            return;
        }

        String filename = item.path().getFileName().toString();
        switch (item.role()) {
            case ENTRYPOINT -> {
                setText(GLYPH_ENTRYPOINT + filename);
                getStyleClass().add(CSS_CLASS_ENTRYPOINT);
                setTooltip(new Tooltip(TOOLTIP_ENTRYPOINT));
            }
            case XML_INPUT -> {
                setText(GLYPH_XML_INPUT + filename);
                getStyleClass().add(CSS_CLASS_XML_INPUT);
                setTooltip(new Tooltip(TOOLTIP_XML_INPUT));
            }
            case REGULAR -> {
                setText(GLYPH_REGULAR + filename);
                setTooltip(null);
            }
        }
    }
}

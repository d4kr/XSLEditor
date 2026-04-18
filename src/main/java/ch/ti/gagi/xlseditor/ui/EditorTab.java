package ch.ti.gagi.xlseditor.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.undo.UndoManager;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Lightweight model for one open editor tab.
 * Owns the {@link CodeArea} and the observable dirty flag derived from the {@link UndoManager}.
 *
 * <p>Load order: replaceText FIRST, mark() SECOND, forgetHistory() THIRD.
 * Any other order causes the tab to open as dirty immediately (RESEARCH.md Pitfall 1).
 *
 * <p>This class is a data carrier with {@code public final} fields. It is NOT an FXML
 * controller and has no setters. Instantiated once per file open by
 * {@code EditorController.openOrFocusTab}.
 *
 * <p>Thread safety: must be created and accessed on the JavaFX Application Thread.
 */
public final class EditorTab {

    /** Absolute, normalized path of the file backing this tab. Never {@code null}. */
    public final Path path;

    /** The RichTextFX code area holding the file content. Never {@code null}. */
    public final CodeArea codeArea;

    /**
     * Observable dirty flag. {@code true} when the buffer differs from the last
     * saved baseline (i.e. the {@link UndoManager} is not at the marked position).
     * Driven by {@code Bindings.not(undoManager.atMarkedPositionProperty())}.
     */
    public final BooleanBinding dirty;

    /**
     * Creates an {@code EditorTab} for the given path and initial content.
     *
     * <p>Constructor body order is non-negotiable (RESEARCH.md Pitfall 1):
     * <ol>
     *   <li>{@code replaceText} — load the file content into the editor</li>
     *   <li>{@code mark()} — record the loaded state as the "saved" baseline</li>
     *   <li>{@code forgetHistory()} — clear the undo stack so Ctrl+Z cannot undo the load</li>
     * </ol>
     *
     * @param path    absolute path of the file; must not be {@code null}
     * @param content initial text content; {@code null} is treated as empty string
     */
    public EditorTab(Path path, String content) {
        this.path     = Objects.requireNonNull(path, "path");
        this.codeArea = new CodeArea();

        // Step 1: load content FIRST — before setting the saved baseline
        this.codeArea.replaceText(Objects.requireNonNullElse(content, ""));

        UndoManager<?> um = this.codeArea.getUndoManager();

        // Step 2: mark the current position as "saved" baseline
        um.mark();

        // Step 3: clear history so Ctrl+Z cannot undo past the initial load
        um.forgetHistory();

        // dirty == "NOT at the marked position"
        this.dirty = Bindings.not(um.atMarkedPositionProperty());
    }
}

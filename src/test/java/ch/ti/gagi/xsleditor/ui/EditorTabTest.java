package ch.ti.gagi.xlseditor.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EditorTabTest {

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit was already initialised by a previous test class — OK.
        }
    }

    @Test
    void newTabIsNotDirtyAfterLoad() {
        EditorTab tab = new EditorTab(Path.of("dummy.xsl"), "<xsl:stylesheet/>");
        assertFalse(tab.dirty.get(), "tab must be clean immediately after load (EDIT-02)");
    }

    @Test
    void tabBecomesDirtyAfterEdit() {
        EditorTab tab = new EditorTab(Path.of("dummy.xsl"), "<xsl:stylesheet/>");
        tab.codeArea.appendText(" ");
        assertTrue(tab.dirty.get(), "tab must be dirty after any text change (EDIT-02)");
    }

    @Test
    void tabBecomesCleanAfterMarkCall() {
        EditorTab tab = new EditorTab(Path.of("dummy.xsl"), "<xsl:stylesheet/>");
        tab.codeArea.appendText(" ");
        assertTrue(tab.dirty.get(), "tab must be dirty after edit (EDIT-02)");
        tab.codeArea.getUndoManager().mark(); // simulates what saveTab() does
        assertFalse(tab.dirty.get(), "tab must be clean after UndoManager.mark() (EDIT-02)");
    }

    @Test
    void undoHistoryClearedAfterLoad() {
        EditorTab tab = new EditorTab(Path.of("dummy.xsl"), "<xsl:stylesheet/>");
        assertFalse(tab.codeArea.getUndoManager().isUndoAvailable(),
            "undo history must be empty immediately after load — forgetHistory() was called (EDIT-02)");
    }
}

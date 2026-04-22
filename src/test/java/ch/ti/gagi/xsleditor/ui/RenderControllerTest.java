package ch.ti.gagi.xsleditor.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RenderController pure-logic behavior.
 * Wave 0: stubs. Wave 1 implementation fills them in.
 *
 * Tests that require a running JavaFX stage use Platform.startup() (same pattern as EditorTabTest).
 * Tests for REND-01 (full pipeline) and REND-06 (performance) are manual-only per VALIDATION.md.
 */
class RenderControllerTest {

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit was already initialised by a previous test class — OK.
        }
    }

    /**
     * REND-02: handleRender() does nothing (no Task spawned) when project is null.
     * Wave 0 stub — enable and implement in Wave 1.
     */
    @Test
    @Disabled("Wave 0 stub — implement in Wave 1 (06-02-PLAN.md)")
    void handleRender_doesNothing_whenProjectIsNull() {
        // TODO Wave 1:
        // 1. Create RenderController
        // 2. Call initialize() with a mock ProjectContext where getCurrentProject() returns null
        // 3. Call handleRender()
        // 4. Assert no Task was spawned (logListView remains empty, pdfCallback not called)
        fail("Not yet implemented");
    }

    /**
     * REND-04: On success, pdfCallback is called with the byte[] from Preview.pdf().
     * Wave 0 stub — enable and implement in Wave 1.
     */
    @Test
    @Disabled("Wave 0 stub — implement in Wave 1 (06-02-PLAN.md)")
    void handleRender_callsPdfCallback_onSuccess() {
        // TODO Wave 1:
        // 1. Create RenderController with a mock PreviewManager that returns success Preview
        // 2. Capture pdfCallback calls
        // 3. Call handleRender()
        // 4. Assert pdfCallback.accept(bytes) was called with non-null byte[]
        fail("Not yet implemented");
    }

    /**
     * REND-05: On failure, pdfCallback is NOT called; errors are routed to logListView.
     * Wave 0 stub — enable and implement in Wave 1.
     */
    @Test
    @Disabled("Wave 0 stub — implement in Wave 1 (06-02-PLAN.md)")
    void handleRender_routesErrorsToLog_onFailure() {
        // TODO Wave 1:
        // 1. Create RenderController with a mock PreviewManager that returns failure Preview with errors
        // 2. Call handleRender()
        // 3. Assert pdfCallback was NOT called
        // 4. Assert logListView contains entries starting with "[ERROR] "
        fail("Not yet implemented");
    }
}

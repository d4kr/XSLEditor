package ch.ti.gagi.xsleditor.preview;

import ch.ti.gagi.xsleditor.model.Project;
import ch.ti.gagi.xsleditor.render.RenderOrchestrator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class PreviewManagerIntegrationTest {

    private static final Path FIXTURES = Paths.get("src/test/resources/fixtures");
    private static final Path IDENTITY = Path.of("identity.xsl");  // relative to rootPath
    private static final Path INVALID  = Path.of("invalid.xsl");   // relative to rootPath
    private static final Path INPUT    = Path.of("input.xml");      // relative to rootPath

    @Test
    void fullPipelineProducesNonEmptyPdfWithMagicBytes() {
        Project project = new Project(FIXTURES, IDENTITY, INPUT);
        PreviewManager manager = new PreviewManager(new RenderOrchestrator());

        Preview preview = manager.generatePreview(project, FIXTURES);

        assertTrue(preview.success(),
                   "Happy-path render should succeed. Errors: " + preview.errors());
        assertFalse(preview.outdated());
        assertNotNull(preview.pdf());
        assertTrue(preview.pdf().length > 0);
        // PDF magic bytes
        byte[] pdf = preview.pdf();
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    void invalidXsltProducesPreviewFailureWithXsltTypeError() {
        Project project = new Project(FIXTURES, INVALID, INPUT);
        PreviewManager manager = new PreviewManager(new RenderOrchestrator());

        Preview preview = manager.generatePreview(project, FIXTURES);

        assertFalse(preview.success(), "Invalid XSLT must not produce a successful Preview");
        assertTrue(preview.outdated(), "Preview.failure() sets outdated=true");
        assertNull(preview.pdf());
        assertFalse(preview.errors().isEmpty(), "At least one PreviewError expected");

        // FIXME: PreviewError.file/line may be null here because RenderOrchestrator.executePipeline
        // calls compileXslt(String) — Saxon has no systemId to report. Only type=XSLT is strictly
        // asserted. Production fix tracked in CONCERNS.md (RenderEngine constructor refactor).
        PreviewError first = preview.errors().get(0);
        assertEquals("XSLT", first.type(),
                     "Saxon compile failure must map to type='XSLT' via ErrorManager.fromException");
        assertNotNull(first.message());
        assertFalse(first.message().isBlank());
    }
}

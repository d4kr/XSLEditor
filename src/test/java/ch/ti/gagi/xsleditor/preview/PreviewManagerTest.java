package ch.ti.gagi.xsleditor.preview;

import ch.ti.gagi.xsleditor.render.RenderError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PreviewManagerTest {

    @Test
    void toPreviewErrors_decodesPercentEncodedFileUri() {
        RenderError error = new RenderError(
                "Value of variable is undefined",
                "XSLT",
                "file:///path/my%20file.xsl:10"
        );

        List<PreviewError> result = PreviewManager.toPreviewErrors(List.of(error));

        assertEquals(1, result.size());
        PreviewError pe = result.get(0);
        assertEquals("/path/my file.xsl", pe.file(),
                "Percent-encoded space in file URI must be decoded to real path");
        assertEquals(10, pe.line(),
                "Line number must be parsed from location string");
    }

    @Test
    void toPreviewErrors_decodesNonEncodedFileUri() {
        RenderError error = new RenderError(
                "Template not found",
                "XSLT",
                "file:///normal/path.xsl:42"
        );

        List<PreviewError> result = PreviewManager.toPreviewErrors(List.of(error));

        PreviewError pe = result.get(0);
        assertEquals("/normal/path.xsl", pe.file());
        assertEquals(42, pe.line());
    }

    @Test
    void resolveFilePath_malformedUriDoesNotThrow() {
        // "file://bad uri" contains a space — URI.create() throws IllegalArgumentException.
        // Fallback must strip "file://" and return the remainder without propagating.
        String result = PreviewManager.resolveFilePath("file://bad uri");
        assertNotNull(result, "Malformed URI fallback must not return null");
        assertFalse(result.startsWith("file://"), "Scheme prefix must be stripped in fallback");
    }
}

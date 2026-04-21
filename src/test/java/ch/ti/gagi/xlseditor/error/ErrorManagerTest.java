package ch.ti.gagi.xlseditor.error;

import ch.ti.gagi.xlseditor.render.RenderError;
import ch.ti.gagi.xlseditor.validation.ValidationError;
import net.sf.saxon.s9api.SaxonApiException;
import org.apache.fop.apps.FOPException;
import org.junit.jupiter.api.Test;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorManagerTest {

    @Test
    void saxonApiExceptionMapsToXsltType() {
        SaxonApiException e = new SaxonApiException("boom");
        RenderError err = ErrorManager.fromException(e);
        assertEquals("XSLT", err.type());
        assertEquals("boom", err.message());
    }

    @Test
    void transformerExceptionMapsToXsltType() {
        TransformerException e = new TransformerException("xslt failure");
        assertEquals("XSLT", ErrorManager.fromException(e).type());
    }

    @Test
    void fopExceptionMapsToFopType() {
        FOPException e = new FOPException("fo broke");
        assertEquals("FOP", ErrorManager.fromException(e).type());
    }

    @Test
    void ioExceptionMapsToIoType() {
        IOException e = new IOException("disk");
        assertEquals("IO", ErrorManager.fromException(e).type());
    }

    @Test
    void unknownExceptionMapsToUnknownType() {
        RuntimeException e = new RuntimeException("???");
        assertEquals("UNKNOWN", ErrorManager.fromException(e).type());
    }

    @Test
    void nullMessageFallsBackToSimpleClassName() {
        RuntimeException e = new RuntimeException((String) null);
        assertEquals("RuntimeException", ErrorManager.fromException(e).message());
    }

    @Test
    void blankMessageFallsBackToSimpleClassName() {
        RuntimeException e = new RuntimeException("   ");
        assertEquals("RuntimeException", ErrorManager.fromException(e).message());
    }

    @Test
    void fromValidationMapsFileAndLineToColonLocation() {
        ValidationError ve = new ValidationError("bad", Path.of("main.xsl"), 42, 5);
        List<RenderError> mapped = ErrorManager.fromValidation(List.of(ve));
        assertEquals(1, mapped.size());
        assertEquals("XSLT", mapped.get(0).type());
        assertEquals("main.xsl:42", mapped.get(0).location());
        assertEquals("bad", mapped.get(0).message());
    }

    @Test
    void fromValidationHandlesNullLine() {
        ValidationError ve = new ValidationError("bad", Path.of("main.xsl"), null, null);
        RenderError mapped = ErrorManager.fromValidation(List.of(ve)).get(0);
        assertEquals("main.xsl", mapped.location());
    }

    @Test
    void fromValidationHandlesNullFile() {
        ValidationError ve = new ValidationError("bad", null, null, null);
        RenderError mapped = ErrorManager.fromValidation(List.of(ve)).get(0);
        assertNull(mapped.location());
    }
}

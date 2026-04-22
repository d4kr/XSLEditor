package ch.ti.gagi.xsleditor.render;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltExecutable;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

class RenderEngineTest {

    private static final Path IDENTITY = Paths.get("src/test/resources/fixtures/identity.xsl");
    private static final Path INPUT    = Paths.get("src/test/resources/fixtures/input.xml");
    private static final Path INVALID  = Paths.get("src/test/resources/fixtures/invalid.xsl");

    @Test
    void compilesValidXsltFromPath() throws Exception {
        RenderEngine engine = new RenderEngine();
        XsltExecutable exe = engine.compileXslt(IDENTITY);
        assertNotNull(exe);
    }

    @Test
    void compilesValidXsltFromString() throws Exception {
        RenderEngine engine = new RenderEngine();
        String xslt = java.nio.file.Files.readString(IDENTITY);
        XsltExecutable exe = engine.compileXslt(xslt);
        assertNotNull(exe);
    }

    @Test
    void compilingMalformedXsltThrowsSaxonApiException() {
        RenderEngine engine = new RenderEngine();
        assertThrows(SaxonApiException.class, () -> engine.compileXslt(INVALID));
    }

    @Test
    void transformsXmlToFoStringContainingBlockAndItemText() throws Exception {
        RenderEngine engine = new RenderEngine();
        XsltExecutable exe = engine.compileXslt(IDENTITY);
        String fo = engine.transformToString(INPUT, exe);
        assertTrue(fo.contains("fo:block"), "FO output should contain fo:block element");
        assertTrue(fo.contains("hello"), "FO output should contain item text from input.xml");
    }

    @Test
    void rendersFoToPdfWithPdfMagicBytes() throws Exception {
        RenderEngine engine = new RenderEngine();
        XsltExecutable exe = engine.compileXslt(IDENTITY);
        String fo = engine.transformToString(INPUT, exe);
        byte[] pdf = engine.renderFoToPdf(fo);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }
}

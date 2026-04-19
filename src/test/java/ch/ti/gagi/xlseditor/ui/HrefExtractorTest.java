package ch.ti.gagi.xlseditor.ui;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Wave 0 stub — enable after HrefExtractor is implemented in Wave 1")
class HrefExtractorTest {

    @Test
    void extractHrefResolvesRelativePathFromXslInclude(@TempDir Path tempDir) throws IOException {
        Path baseFile = tempDir.resolve("template.xsl");
        Path includedFile = tempDir.resolve("common.xsl");
        Files.createFile(includedFile);
        String text = "<xsl:include href=\"common.xsl\"/>";
        int charIndex = text.indexOf("common.xsl") + 3; // inside the href value string
        Optional<Path> result = HrefExtractor.extractHref(text, charIndex, baseFile);
        assertTrue(result.isPresent(), "Must resolve the href to an Optional<Path>");
        assertEquals(includedFile.normalize(), result.get().normalize(),
            "Resolved path must match the actual file on disk");
    }

    @Test
    void extractHrefReturnsEmptyWhenNotInsideHref(@TempDir Path tempDir) throws IOException {
        Path baseFile = tempDir.resolve("template.xsl");
        String text = "<xsl:include href=\"common.xsl\"/>";
        // charIndex points to 'x' in 'xsl:include', NOT inside the href value
        int charIndex = 1;
        Optional<Path> result = HrefExtractor.extractHref(text, charIndex, baseFile);
        assertTrue(result.isEmpty(), "Cursor outside href value must return Optional.empty()");
    }

    @Test
    void pathTraversalIsRejected(@TempDir Path tempDir) throws IOException {
        Path baseFile = tempDir.resolve("template.xsl");
        String text = "<xsl:include href=\"../../etc/passwd\"/>";
        int charIndex = text.indexOf("../../etc/passwd") + 5; // inside the traversal value
        Optional<Path> result = HrefExtractor.extractHref(text, charIndex, baseFile);
        assertTrue(result.isEmpty(),
            "Path traversal href must be rejected and return Optional.empty()");
    }
}

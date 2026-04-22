package ch.ti.gagi.xsleditor.library;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LibraryPreprocessorTest {

    @Test
    void detectsSingleLibraryDirective() {
        String input = "<xsl:stylesheet><?LIBRARY common?><xsl:template/></xsl:stylesheet>";
        List<String> result = LibraryPreprocessor.detectLibraries(input);
        assertEquals(List.of("common"), result);
    }

    @Test
    void detectsMultipleLibraryDirectivesInOrder() {
        String input = "<?LIBRARY a?>X<?LIBRARY b?>Y<?LIBRARY c?>";
        assertEquals(List.of("a", "b", "c"), LibraryPreprocessor.detectLibraries(input));
    }

    @Test
    void mergesLibraryContentInPlace(@TempDir Path tempDir) throws IOException, LibraryProcessingException {
        Files.writeString(tempDir.resolve("common.xsl"), "<xsl:template name=\"lib\"/>");
        String template = "before<?LIBRARY common?>after";
        String result = LibraryPreprocessor.mergeLibraries(tempDir, template);
        assertEquals("before<xsl:template name=\"lib\"/>after", result);
    }

    @Test
    void cachesLibraryContentAcrossMultipleDirectives(@TempDir Path tempDir) throws IOException, LibraryProcessingException {
        Files.writeString(tempDir.resolve("util.xsl"), "LIB");
        String template = "<?LIBRARY util?>|<?LIBRARY util?>";
        String result = LibraryPreprocessor.mergeLibraries(tempDir, template);
        assertEquals("LIB|LIB", result);
    }

    @Test
    void throwsLibraryProcessingExceptionWhenFileMissing(@TempDir Path tempDir) {
        String template = "<?LIBRARY missing?>";
        LibraryProcessingException ex = assertThrows(LibraryProcessingException.class,
                () -> LibraryPreprocessor.mergeLibraries(tempDir, template));
        assertTrue(ex.getMessage().contains("Library file not found"));
    }
}

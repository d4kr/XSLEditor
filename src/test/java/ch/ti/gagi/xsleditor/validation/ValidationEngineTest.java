package ch.ti.gagi.xlseditor.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationEngineTest {

    @Test
    void wellFormedXmlReturnsEmptyErrorList(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("ok.xml"), "<?xml version=\"1.0\"?><root><item/></root>");
        List<ValidationError> errors = ValidationEngine.validateXml(tempDir.resolve("ok.xml"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void malformedXmlReturnsErrorWithLineInfo(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("bad.xml"), "<?xml version=\"1.0\"?>\n<root>\n  <item>\n</root>");
        List<ValidationError> errors = ValidationEngine.validateXml(tempDir.resolve("bad.xml"));
        assertEquals(1, errors.size());
        ValidationError err = errors.get(0);
        assertNotNull(err.message());
        assertFalse(err.message().isBlank());
        assertEquals(tempDir.resolve("bad.xml"), err.file());
        assertNotNull(err.line());
        assertTrue(err.line() >= 1);
    }

    @Test
    void missingFileReturnsSingleErrorWithNullLine(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist.xml");
        List<ValidationError> errors = ValidationEngine.validateXml(missing);
        assertEquals(1, errors.size());
        assertNull(errors.get(0).line());
    }

    @Test
    void validateAllAggregatesErrorsAcrossFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("good.xml"), "<?xml version=\"1.0\"?><root><item/></root>");
        Files.writeString(tempDir.resolve("bad.xml"), "<?xml version=\"1.0\"?>\n<root>\n  <item>\n</root>");
        List<ValidationError> errors = ValidationEngine.validateAll(
                List.of(tempDir.resolve("good.xml"), tempDir.resolve("bad.xml")));
        assertEquals(1, errors.size());
        assertEquals(tempDir.resolve("bad.xml"), errors.get(0).file());
    }
}

package ch.ti.gagi.xlseditor.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectConfigTest {

    @Test
    void writesJsonFile(@TempDir Path tempDir) throws IOException {
        Path configPath = tempDir.resolve(".xslfo-tool.json");
        ProjectConfig config = new ProjectConfig("template.xsl", "input.xml");
        config.write(configPath);
        assertTrue(Files.exists(configPath));
        String json = Files.readString(configPath);
        assertTrue(json.contains("\"entryPoint\""), "JSON must contain entryPoint key");
        assertTrue(json.contains("template.xsl"), "JSON must contain entryPoint value");
        assertTrue(json.contains("\"xmlInput\""));
        assertTrue(json.contains("input.xml"));
    }

    @Test
    void writesJsonFileOmittingNullFields(@TempDir Path tempDir) throws IOException {
        Path configPath = tempDir.resolve(".xslfo-tool.json");
        ProjectConfig config = new ProjectConfig("template.xsl", null);
        config.write(configPath);
        String json = Files.readString(configPath);
        assertTrue(json.contains("entryPoint"));
        assertFalse(json.contains("xmlInput"), "null xmlInput must be omitted from JSON");
    }

    @Test
    void readsPartialConfig(@TempDir Path tempDir) throws IOException {
        Path configPath = tempDir.resolve(".xslfo-tool.json");
        Files.writeString(configPath, "{\"entryPoint\":\"template.xsl\"}");
        ProjectConfig config = ProjectConfig.read(configPath);
        assertEquals("template.xsl", config.entryPoint());
        assertNull(config.xmlInput(), "missing xmlInput must read as null (D-03)");
    }

    @Test
    void readsExplicitNullFields(@TempDir Path tempDir) throws IOException {
        // Guards pitfall A2: JsonNode.asText() on NullNode returns the string "null" unless !isNull() is checked.
        Path configPath = tempDir.resolve(".xslfo-tool.json");
        Files.writeString(configPath, "{\"entryPoint\":null,\"xmlInput\":null}");
        ProjectConfig config = ProjectConfig.read(configPath);
        assertNull(config.entryPoint(), "explicit JSON null must become Java null, not the string \"null\"");
        assertNull(config.xmlInput());
    }

    @Test
    void rejectsAbsoluteEntryPoint() {
        assertThrows(IllegalArgumentException.class,
            () -> new ProjectConfig("/abs/template.xsl", null));
    }

    @Test
    void rejectsAbsoluteXmlInput() {
        assertThrows(IllegalArgumentException.class,
            () -> new ProjectConfig(null, "/abs/input.xml"));
    }

    @Test
    void allowsBothFieldsNull() {
        // D-03: partial state — both nulls must be accepted
        assertDoesNotThrow(() -> new ProjectConfig(null, null));
    }
}

package ch.ti.gagi.xlseditor.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;

public record ProjectConfig(String entryPoint, String xmlInput) {

    public ProjectConfig {
        if (entryPoint == null || entryPoint.isBlank()) {
            throw new IllegalArgumentException("entryPoint must not be blank");
        }
        if (xmlInput == null || xmlInput.isBlank()) {
            throw new IllegalArgumentException("xmlInput must not be blank");
        }
        if (Path.of(entryPoint).isAbsolute()) {
            throw new IllegalArgumentException("entryPoint must be a relative path");
        }
        if (Path.of(xmlInput).isAbsolute()) {
            throw new IllegalArgumentException("xmlInput must be a relative path");
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ProjectConfig read(Path configPath) throws IOException {
        JsonNode root = MAPPER.readTree(configPath.toFile());

        JsonNode ep = root.get("entryPoint");
        JsonNode xi = root.get("xmlInput");

        return new ProjectConfig(
                ep != null ? ep.asText() : null,
                xi != null ? xi.asText() : null
        );
    }
}

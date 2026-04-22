package ch.ti.gagi.xsleditor.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public record ProjectConfig(String entryPoint, String xmlInput) {

    public ProjectConfig {
        // D-03: nulls allowed — both fields are optional (partial config state)
        if (entryPoint != null && Path.of(entryPoint).isAbsolute()) {
            throw new IllegalArgumentException("entryPoint must be a relative path");
        }
        if (xmlInput != null && Path.of(xmlInput).isAbsolute()) {
            throw new IllegalArgumentException("xmlInput must be a relative path");
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ProjectConfig read(Path configPath) throws IOException {
        JsonNode root = MAPPER.readTree(configPath.toFile());
        JsonNode ep = root.get("entryPoint");
        JsonNode xi = root.get("xmlInput");
        return new ProjectConfig(
            ep != null && !ep.isNull() ? ep.asText() : null,
            xi != null && !xi.isNull() ? xi.asText() : null
        );
    }

    public void write(Path configPath) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        if (entryPoint != null) map.put("entryPoint", entryPoint);
        if (xmlInput   != null) map.put("xmlInput",   xmlInput);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), map);
    }
}

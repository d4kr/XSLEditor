package ch.ti.gagi.xlseditor.model;

import java.nio.file.Path;
import java.util.Objects;

public final class Project {

    private static final String CONFIG_FILENAME = ".xslfo-tool.json";

    // Paths are relative to rootPath
    private final Path rootPath;
    private final Path configPath;
    private final Path entryPoint;
    private final Path xmlInput;

    public Project(Path rootPath, Path entryPoint, Path xmlInput) {
        this.rootPath = Objects.requireNonNull(rootPath);
        this.configPath = deriveConfigPath(rootPath);
        this.entryPoint = entryPoint;
        this.xmlInput = xmlInput;
    }

    public static Path deriveConfigPath(Path rootPath) {
        return rootPath.resolve(CONFIG_FILENAME);
    }

    public Path rootPath() {
        return rootPath;
    }

    public Path configPath() {
        return configPath;
    }

    public Path entryPoint() {
        return entryPoint;
    }

    public Path xmlInput() {
        return xmlInput;
    }
}

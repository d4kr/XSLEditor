package ch.ti.gagi.xlseditor.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ProjectManager {

    public static Project loadProject(Path rootPath) throws IOException {
        Path configPath = Project.deriveConfigPath(rootPath);
        if (!Files.exists(configPath)) {
            // D-01: no config file → partial project, both fields null
            return new Project(rootPath, null, null);
        }
        ProjectConfig config = ProjectConfig.read(configPath);
        Path ep = config.entryPoint() != null ? Path.of(config.entryPoint()) : null;
        Path xi = config.xmlInput()   != null ? Path.of(config.xmlInput())   : null;
        return new Project(rootPath, ep, xi);
    }
}
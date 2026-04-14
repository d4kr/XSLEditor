package ch.ti.gagi.xlseditor.model;

import java.io.IOException;
import java.nio.file.Path;

public final class ProjectManager {

    public static Project loadProject(Path rootPath) throws IOException {
        ProjectConfig config = ProjectConfig.read(Project.deriveConfigPath(rootPath));

        return new Project(rootPath, Path.of(config.entryPoint()), Path.of(config.xmlInput()));
    }
}
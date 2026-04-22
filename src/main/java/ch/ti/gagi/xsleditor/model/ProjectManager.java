package ch.ti.gagi.xsleditor.model;

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
        Path root = rootPath.toAbsolutePath().normalize();

        Path ep = config.entryPoint() != null
            ? root.resolve(config.entryPoint()).normalize()
            : null;
        if (ep != null && !ep.startsWith(root)) {
            throw new IOException("entryPoint escapes project root: " + config.entryPoint());
        }

        Path xi = config.xmlInput() != null
            ? root.resolve(config.xmlInput()).normalize()
            : null;
        if (xi != null && !xi.startsWith(root)) {
            throw new IOException("xmlInput escapes project root: " + config.xmlInput());
        }

        // Store relative paths in Project (relative to rootPath)
        Path epRelative = ep != null ? root.relativize(ep) : null;
        Path xiRelative = xi != null ? root.relativize(xi) : null;
        return new Project(rootPath, epRelative, xiRelative);
    }
}
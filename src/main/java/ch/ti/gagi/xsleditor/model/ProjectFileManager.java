package ch.ti.gagi.xsleditor.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ProjectFileManager {

    public static ProjectFile load(Path rootPath, Path relativePath) throws IOException {
        Path absolutePath = rootPath.resolve(relativePath);
        String content = Files.readString(absolutePath, StandardCharsets.UTF_8);
        return new ProjectFile(relativePath, content);
    }

    public static void save(Path rootPath, ProjectFile file) throws IOException {
        Path absolutePath = rootPath.resolve(file.path());

        Files.createDirectories(absolutePath.getParent());
        Files.writeString(absolutePath, file.content(), StandardCharsets.UTF_8);

        file.markClean();
    }
}

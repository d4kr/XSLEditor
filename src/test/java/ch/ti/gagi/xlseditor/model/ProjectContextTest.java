package ch.ti.gagi.xlseditor.model;

import ch.ti.gagi.xlseditor.ui.ProjectContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectContextTest {

    @Test
    void openProjectWithoutConfigReturnsPartialProject(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        Project project = ctx.openProject(tempDir);
        assertEquals(tempDir, project.rootPath());
        assertNull(project.entryPoint());
        assertNull(project.xmlInput());
        assertTrue(ctx.isProjectLoaded());
        assertSame(project, ctx.getCurrentProject());
    }

    @Test
    void createFileWritesEmptyFile(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        ctx.createFile("output.xsl");
        Path expected = tempDir.resolve("output.xsl");
        assertTrue(Files.exists(expected));
        assertEquals(0L, Files.size(expected));
    }

    @Test
    void createFileRejectsParentTraversal(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        assertThrows(IllegalArgumentException.class,
            () -> ctx.createFile("../escape.xsl"),
            "filenames containing .. must be rejected");
        // Verify no file was created outside the project root
        assertFalse(Files.exists(tempDir.getParent().resolve("escape.xsl")));
    }

    @Test
    void createFileRejectsSubdirectory(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        assertThrows(IllegalArgumentException.class,
            () -> ctx.createFile("sub/nested.xsl"),
            "filenames containing path separators must be rejected");
    }

    @Test
    void createFileRejectsAbsolutePath(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        assertThrows(IllegalArgumentException.class,
            () -> ctx.createFile("/etc/passwd"));
    }

    @Test
    void createFileRejectsBlankFilename(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        assertThrows(IllegalArgumentException.class, () -> ctx.createFile(""));
        assertThrows(IllegalArgumentException.class, () -> ctx.createFile("   "));
    }

    @Test
    void createFileWithoutOpenProjectThrows() {
        ProjectContext ctx = new ProjectContext();
        assertThrows(IllegalStateException.class, () -> ctx.createFile("x.xsl"));
        assertFalse(ctx.isProjectLoaded());
    }
}

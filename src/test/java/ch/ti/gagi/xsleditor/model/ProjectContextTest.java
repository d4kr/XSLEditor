package ch.ti.gagi.xsleditor.model;

import ch.ti.gagi.xsleditor.ui.ProjectContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

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

    // --- Phase 3: projectFilesProperty and setEntrypoint/setXmlInput --------

    @Test
    void projectFilesListIsEmptyBeforeOpen() {
        ProjectContext ctx = new ProjectContext();
        ObservableList<Path> files = ctx.projectFilesProperty();
        assertNotNull(files, "projectFilesProperty must never return null");
        assertTrue(files.isEmpty());
    }

    @Test
    void openProjectPopulatesProjectFiles(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("a.xsl"));
        Files.createFile(tempDir.resolve("b.xml"));
        Files.createFile(tempDir.resolve("c.txt"));
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        ObservableList<Path> files = ctx.projectFilesProperty();
        assertEquals(3, files.size(), "expected 3 files in root");
        assertTrue(files.contains(Path.of("a.xsl")));
        assertTrue(files.contains(Path.of("b.xml")));
        assertTrue(files.contains(Path.of("c.txt")));
    }

    @Test
    void openProjectExcludesDotFiles(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("visible.xsl"));
        Files.createFile(tempDir.resolve(".xslfo-tool.json"));
        Files.writeString(tempDir.resolve(".xslfo-tool.json"), "{}");
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        ObservableList<Path> files = ctx.projectFilesProperty();
        assertEquals(1, files.size());
        assertTrue(files.contains(Path.of("visible.xsl")));
        assertFalse(files.contains(Path.of(".xslfo-tool.json")),
            "dotfiles must be excluded from projectFiles");
    }

    @Test
    void openProjectExcludesSubdirectoryContents(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("flat.xsl"));
        Files.createDirectory(tempDir.resolve("sub"));
        Files.createFile(tempDir.resolve("sub").resolve("nested.xsl"));
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        ObservableList<Path> files = ctx.projectFilesProperty();
        assertTrue(files.contains(Path.of("flat.xsl")));
        assertFalse(files.contains(Path.of("sub").resolve("nested.xsl")),
            "Phase 3 is flat: nested files must NOT appear");
        assertFalse(files.contains(Path.of("nested.xsl")),
            "sub/nested.xsl must not bleed up as a bare filename either");
    }

    @Test
    void openProjectReturnsSortedList(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("zebra.xml"));
        Files.createFile(tempDir.resolve("apple.xsl"));
        Files.createFile(tempDir.resolve("middle.xsl"));
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        ObservableList<Path> files = ctx.projectFilesProperty();
        assertEquals(Path.of("apple.xsl"),  files.get(0));
        assertEquals(Path.of("middle.xsl"), files.get(1));
        assertEquals(Path.of("zebra.xml"),  files.get(2));
    }

    @Test
    void reopenProjectRefreshesList(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("one.xsl"));
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        assertEquals(1, ctx.projectFilesProperty().size());
        Files.createFile(tempDir.resolve("two.xml"));
        ctx.openProject(tempDir); // same dir, files changed on disk
        assertEquals(2, ctx.projectFilesProperty().size(),
            "second openProject must refresh the list");
    }

    @Test
    void createFileAppendsToProjectFiles(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        assertTrue(ctx.projectFilesProperty().isEmpty());
        ctx.createFile("new.xsl");
        assertTrue(ctx.projectFilesProperty().contains(Path.of("new.xsl")),
            "createFile must append to projectFiles (D-07)");
    }

    @Test
    void createFileTriggersObservableListChange(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        AtomicBoolean fired = new AtomicBoolean(false);
        ctx.projectFilesProperty().addListener((ListChangeListener<Path>) change -> {
            while (change.next()) {
                if (change.wasAdded()) fired.set(true);
            }
        });
        ctx.createFile("listener.xsl");
        assertTrue(fired.get(), "ListChangeListener must fire wasAdded after createFile");
    }

    @Test
    void setEntrypointPersistsAndUpdatesProject(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("template.xsl"));
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        ctx.setEntrypoint(Path.of("template.xsl"));
        Path configPath = tempDir.resolve(".xslfo-tool.json");
        assertTrue(Files.exists(configPath));
        ProjectConfig read = ProjectConfig.read(configPath);
        assertEquals("template.xsl", read.entryPoint());
        assertNull(read.xmlInput());
        assertEquals(Path.of("template.xsl"), ctx.getCurrentProject().entryPoint());
        assertNull(ctx.getCurrentProject().xmlInput());
    }

    @Test
    void setXmlInputPersistsAndUpdatesProject(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("data.xml"));
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        ctx.setXmlInput(Path.of("data.xml"));
        Path configPath = tempDir.resolve(".xslfo-tool.json");
        assertTrue(Files.exists(configPath));
        ProjectConfig read = ProjectConfig.read(configPath);
        assertEquals("data.xml", read.xmlInput());
        assertNull(read.entryPoint());
        assertEquals(Path.of("data.xml"), ctx.getCurrentProject().xmlInput());
        assertNull(ctx.getCurrentProject().entryPoint());
    }

    @Test
    void setEntrypointPreservesExistingXmlInput(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("template.xsl"));
        Files.createFile(tempDir.resolve("data.xml"));
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        ctx.setXmlInput(Path.of("data.xml"));
        ctx.setEntrypoint(Path.of("template.xsl"));
        ProjectConfig read = ProjectConfig.read(tempDir.resolve(".xslfo-tool.json"));
        assertEquals("template.xsl", read.entryPoint());
        assertEquals("data.xml", read.xmlInput(),
            "setEntrypoint must preserve xmlInput (regression guard)");
        assertEquals(Path.of("template.xsl"), ctx.getCurrentProject().entryPoint());
        assertEquals(Path.of("data.xml"),     ctx.getCurrentProject().xmlInput());
    }

    @Test
    void setXmlInputPreservesExistingEntrypoint(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("template.xsl"));
        Files.createFile(tempDir.resolve("data.xml"));
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        ctx.setEntrypoint(Path.of("template.xsl"));
        ctx.setXmlInput(Path.of("data.xml"));
        ProjectConfig read = ProjectConfig.read(tempDir.resolve(".xslfo-tool.json"));
        assertEquals("template.xsl", read.entryPoint());
        assertEquals("data.xml",     read.xmlInput());
    }

    @Test
    void setEntrypointWithNullClearsField(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("template.xsl"));
        Files.createFile(tempDir.resolve("data.xml"));
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        ctx.setEntrypoint(Path.of("template.xsl"));
        ctx.setXmlInput(Path.of("data.xml"));
        ctx.setEntrypoint(null);  // clear
        Path configPath = tempDir.resolve(".xslfo-tool.json");
        String json = Files.readString(configPath);
        assertFalse(json.contains("entryPoint"),
            "cleared entrypoint must be omitted from JSON");
        assertTrue(json.contains("xmlInput"));
        assertNull(ctx.getCurrentProject().entryPoint());
        assertEquals(Path.of("data.xml"), ctx.getCurrentProject().xmlInput());
    }

    @Test
    void setEntrypointRejectsAbsolutePath(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        assertThrows(IllegalArgumentException.class,
            () -> ctx.setEntrypoint(Path.of("/abs/template.xsl")));
        assertFalse(Files.exists(tempDir.resolve(".xslfo-tool.json")),
            "config must NOT be written when validation fails");
    }

    @Test
    void setEntrypointRejectsTraversal(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        assertThrows(IllegalArgumentException.class,
            () -> ctx.setEntrypoint(Path.of("../escape.xsl")));
        assertFalse(Files.exists(tempDir.resolve(".xslfo-tool.json")));
        assertFalse(Files.exists(tempDir.getParent().resolve("escape.xsl")));
    }

    @Test
    void setXmlInputRejectsAbsolutePath(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        assertThrows(IllegalArgumentException.class,
            () -> ctx.setXmlInput(Path.of("/abs/data.xml")));
        assertFalse(Files.exists(tempDir.resolve(".xslfo-tool.json")));
    }

    @Test
    void setXmlInputRejectsTraversal(@TempDir Path tempDir) throws IOException {
        ProjectContext ctx = new ProjectContext();
        ctx.openProject(tempDir);
        assertThrows(IllegalArgumentException.class,
            () -> ctx.setXmlInput(Path.of("../escape.xml")));
        assertFalse(Files.exists(tempDir.resolve(".xslfo-tool.json")));
    }

    @Test
    void setEntrypointWithoutOpenProjectThrows() {
        ProjectContext ctx = new ProjectContext();
        assertThrows(IllegalStateException.class,
            () -> ctx.setEntrypoint(Path.of("template.xsl")));
    }

    @Test
    void setXmlInputWithoutOpenProjectThrows() {
        ProjectContext ctx = new ProjectContext();
        assertThrows(IllegalStateException.class,
            () -> ctx.setXmlInput(Path.of("data.xml")));
    }
}

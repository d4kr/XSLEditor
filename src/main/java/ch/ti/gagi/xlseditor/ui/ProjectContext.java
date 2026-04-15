package ch.ti.gagi.xlseditor.ui;

import ch.ti.gagi.xlseditor.model.Project;
import ch.ti.gagi.xlseditor.model.ProjectManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Holds the live Project reference and exposes observable "project loaded" state
 * so UI controls (menu items, toolbar buttons) can bind their disable state.
 *
 * D-06: project state lives here, NOT inline in MainController.
 * Pure state service — no reference to MainController; returns Project from
 * openProject() so the caller (MainController) can update its own title.
 */
public final class ProjectContext {

    private Project currentProject;
    private final BooleanProperty projectLoaded = new SimpleBooleanProperty(false);

    public BooleanProperty projectLoadedProperty() { return projectLoaded; }
    public boolean isProjectLoaded()               { return projectLoaded.get(); }
    public Project getCurrentProject()             { return currentProject; }

    /**
     * Loads a project from the given directory. Returns the loaded Project so
     * the caller (e.g. MainController.handleOpenProject) can update the window
     * title per D-07 without this class depending on the UI layer.
     *
     * If the directory has no .xslfo-tool.json, returns a partial project with
     * null entryPoint and null xmlInput (D-01). If the config file exists but
     * fails to parse, the underlying IOException propagates.
     */
    public Project openProject(Path rootPath) throws IOException {
        Project project = ProjectManager.loadProject(rootPath);
        this.currentProject = project;
        this.projectLoaded.set(true);
        return project;
    }

    /**
     * Creates an empty file in the project root directory (D-09).
     *
     * Security: filename is validated to prevent path traversal. The rule is
     * strict — only a simple filename is accepted, no path separators and no
     * parent-directory references. This blocks T-02-01 (path traversal via
     * TextInputDialog input).
     *
     * @throws IllegalStateException     if no project is loaded
     * @throws IllegalArgumentException  if filename is blank, contains path
     *                                   separators, is absolute, or contains ".."
     * @throws java.nio.file.FileAlreadyExistsException if the file already exists
     * @throws IOException               on any other I/O failure
     */
    public void createFile(String filename) throws IOException {
        if (currentProject == null) {
            throw new IllegalStateException("No project loaded");
        }
        validateSimpleFilename(filename);

        Path root = currentProject.rootPath().toAbsolutePath().normalize();
        Path target = root.resolve(filename).normalize();

        // Defense in depth: ensure the resolved+normalized target is still inside root
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IllegalArgumentException(
                "File name must resolve to a path inside the project root: " + filename);
        }

        Files.createFile(target); // atomic; throws FileAlreadyExistsException on conflict
    }

    /**
     * T-02-01 mitigation (path traversal, STRIDE-T/E): reject anything that is
     * not a plain filename. Rules:
     *   - non-null, non-blank
     *   - no '/' or '\' (no subdirectories)
     *   - not absolute
     *   - does not contain ".." as a path element
     */
    private static void validateSimpleFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name must not be blank");
        }
        if (filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException(
                "File name must not contain path separators: " + filename);
        }
        if (filename.contains("..")) {
            throw new IllegalArgumentException(
                "File name must not contain parent references: " + filename);
        }
        if (Path.of(filename).isAbsolute()) {
            throw new IllegalArgumentException(
                "File name must be relative: " + filename);
        }
    }
}

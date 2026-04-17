package ch.ti.gagi.xlseditor.ui;

import ch.ti.gagi.xlseditor.model.Project;
import ch.ti.gagi.xlseditor.model.ProjectConfig;
import ch.ti.gagi.xlseditor.model.ProjectManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

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

    // D-06/D-07: live observable list of files in the project root (relative paths),
    // sorted lexicographically, dotfiles excluded. The TreeView in FileTreeController
    // binds to this list; mutations here surface automatically in the UI.
    private final ObservableList<Path> projectFiles = FXCollections.observableArrayList();

    public BooleanProperty projectLoadedProperty() { return projectLoaded; }
    public boolean isProjectLoaded()               { return projectLoaded.get(); }
    public Project getCurrentProject()             { return currentProject; }
    public ObservableList<Path> projectFilesProperty() { return projectFiles; }

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
        refreshProjectFiles(rootPath);   // populate list BEFORE flipping the loaded flag
        this.projectLoaded.set(true);
        return project;
    }

    /**
     * Populates {@code projectFiles} with a sorted list of regular files in the
     * project root directory, with names relative to {@code rootPath}. Excludes:
     *  - dotfiles (name starts with "."); this hides .xslfo-tool.json, .git, etc.
     *  - subdirectories and their contents (Phase 3 is a flat tree per TREE-01)
     *
     * Idempotent: clears the list before each repopulation, so subsequent
     * openProject(sameDir) calls mirror the current filesystem state.
     */
    private void refreshProjectFiles(Path rootPath) throws IOException {
        projectFiles.clear();
        try (Stream<Path> entries = Files.list(rootPath)) {
            entries
                .filter(Files::isRegularFile)
                .filter(p -> !p.getFileName().toString().startsWith("."))
                .map(rootPath::relativize)
                .sorted(Comparator.comparing(Path::toString))
                .forEach(projectFiles::add);
        }
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

        // D-07: reflect the new file in the observable list so FileTreeController
        // re-renders without a full project reopen.
        Path relative = currentProject.rootPath().toAbsolutePath().normalize().relativize(target);
        projectFiles.add(relative);
    }

    /**
     * Sets the entrypoint XSLT file and persists the change to .xslfo-tool.json
     * immediately (D-01). The currentProject is rebuilt with the new entrypoint
     * so downstream observers see the change via a subsequent getCurrentProject()
     * call. xmlInput is preserved unchanged.
     *
     * @param relativePath path relative to the project root, or null to clear.
     *                     MUST be relative and, when resolved against the root
     *                     and normalized, remain inside the root (no traversal).
     * @throws IllegalStateException     if no project is loaded
     * @throws IllegalArgumentException  if relativePath is absolute or escapes root
     * @throws IOException               on write failure
     */
    public void setEntrypoint(Path relativePath) throws IOException {
        if (currentProject == null) {
            throw new IllegalStateException("No project loaded");
        }
        validateProjectRelativePath(relativePath);
        writeConfigAndRebuildProject(relativePath, currentProject.xmlInput());
    }

    /**
     * Mirror of {@link #setEntrypoint}. Preserves the current entrypoint.
     */
    public void setXmlInput(Path relativePath) throws IOException {
        if (currentProject == null) {
            throw new IllegalStateException("No project loaded");
        }
        validateProjectRelativePath(relativePath);
        writeConfigAndRebuildProject(currentProject.entryPoint(), relativePath);
    }

    /**
     * Shared write-back: persists a ProjectConfig with the two fields and
     * rebuilds the in-memory Project. Private because both setters funnel here.
     * ProjectConfig.write omits null fields (D-03), so passing null clears.
     */
    private void writeConfigAndRebuildProject(Path entryPoint, Path xmlInput) throws IOException {
        String ep = entryPoint != null ? entryPoint.toString() : null;
        String xi = xmlInput   != null ? xmlInput.toString()   : null;
        Path configPath = Project.deriveConfigPath(currentProject.rootPath());
        new ProjectConfig(ep, xi).write(configPath);
        this.currentProject = new Project(currentProject.rootPath(), entryPoint, xmlInput);
    }

    /**
     * Narrower than validateSimpleFilename — allows subdirectory segments but
     * blocks absolute paths and ../ traversal. Null is accepted (clears field).
     */
    private void validateProjectRelativePath(Path relativePath) {
        if (relativePath == null) return;
        if (relativePath.isAbsolute()) {
            throw new IllegalArgumentException(
                "project-relative path must not be absolute: " + relativePath);
        }
        Path root = currentProject.rootPath().toAbsolutePath().normalize();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalArgumentException(
                "project-relative path must stay inside the project root: " + relativePath);
        }
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

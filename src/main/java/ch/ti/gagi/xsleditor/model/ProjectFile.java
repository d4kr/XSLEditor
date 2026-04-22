package ch.ti.gagi.xlseditor.model;

import java.nio.file.Path;

public final class ProjectFile {

    // Path relative to project root
    private final Path path;

    private String content;
    private boolean dirty;
    private boolean open;

    public ProjectFile(Path path, String content) {
        this.path = path;
        this.content = content;
        this.dirty = false;
        this.open = false;
    }

    public Path path() {
        return path;
    }

    public String content() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
package ch.ti.gagi.xlseditor.preview;

import ch.ti.gagi.xlseditor.model.Project;
import ch.ti.gagi.xlseditor.render.RenderOrchestrator;
import ch.ti.gagi.xlseditor.render.RenderResult;

import java.nio.file.Path;

public final class PreviewManager {

    private final RenderOrchestrator orchestrator;

    public PreviewManager(RenderOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public RenderResult generatePreview(Project project, Path rootPath) {
        return orchestrator.renderSafe(project, rootPath);
    }
}

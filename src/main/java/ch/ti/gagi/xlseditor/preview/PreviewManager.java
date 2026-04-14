package ch.ti.gagi.xlseditor.preview;

import ch.ti.gagi.xlseditor.model.Project;
import ch.ti.gagi.xlseditor.render.RenderError;
import ch.ti.gagi.xlseditor.render.RenderOrchestrator;
import ch.ti.gagi.xlseditor.render.RenderResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PreviewManager {

    private final RenderOrchestrator orchestrator;

    public PreviewManager(RenderOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public Preview generatePreview(Project project, Path rootPath) {
        return toPreview(orchestrator.renderSafe(project, rootPath));
    }

    private static Preview toPreview(RenderResult result) {
        return result.success()
                ? Preview.success(result.pdf())
                : Preview.failure(toPreviewErrors(result.errors()));
    }

    private static List<PreviewError> toPreviewErrors(List<RenderError> errors) {
        List<PreviewError> result = new ArrayList<>(errors.size());
        for (RenderError e : errors) {
            String file = null;
            Integer line = null;
            String location = e.location();
            if (location != null) {
                int colon = location.lastIndexOf(':');
                if (colon > 0) {
                    String linePart = location.substring(colon + 1);
                    try {
                        line = Integer.parseInt(linePart);
                        file = location.substring(0, colon);
                    } catch (NumberFormatException ignored) {
                        file = location;
                    }
                } else {
                    file = location;
                }
            }
            result.add(new PreviewError(e.message(), e.type(), file, line));
        }
        return result;
    }
}

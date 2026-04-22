package ch.ti.gagi.xsleditor.preview;

import ch.ti.gagi.xsleditor.model.Project;
import ch.ti.gagi.xsleditor.render.RenderError;
import ch.ti.gagi.xsleditor.render.RenderOrchestrator;
import ch.ti.gagi.xsleditor.render.RenderResult;

import java.net.URI;
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

    static String resolveFilePath(String raw) {
        if (raw != null && raw.startsWith("file://")) {
            try {
                return URI.create(raw).getPath();
            } catch (IllegalArgumentException ignored) {
                // Malformed URI — strip scheme prefix manually.
                String stripped = raw.substring("file://".length());
                return stripped.startsWith("/") ? stripped : "/" + stripped;
            }
        }
        return raw;
    }

    static List<PreviewError> toPreviewErrors(List<RenderError> errors) {
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
                        file = resolveFilePath(location.substring(0, colon));
                    } catch (NumberFormatException ignored) {
                        file = resolveFilePath(location);
                    }
                } else {
                    file = resolveFilePath(location);
                }
            }
            result.add(new PreviewError(e.message(), e.type(), file, line));
        }
        return result;
    }
}

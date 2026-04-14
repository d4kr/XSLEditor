package ch.ti.gagi.xlseditor.render;

import ch.ti.gagi.xlseditor.dependency.DependencyGraph;
import ch.ti.gagi.xlseditor.dependency.DependencyResolver;
import ch.ti.gagi.xlseditor.library.LibraryPreprocessor;
import ch.ti.gagi.xlseditor.model.Project;
import ch.ti.gagi.xlseditor.validation.ValidationEngine;
import ch.ti.gagi.xlseditor.validation.ValidationError;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltExecutable;
import org.apache.fop.apps.FOPException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RenderOrchestrator {

    private final RenderEngine renderEngine = new RenderEngine();

    public byte[] render(Project project, Path rootPath) throws Exception {
        // 1. Build dependency graph
        DependencyGraph graph = DependencyResolver.buildGraph(rootPath, project.entryPoint());

        // 2. Validate all files — fail immediately on errors
        List<ValidationError> errors = ValidationEngine.validateProject(rootPath, project, graph);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Validation failed with " + errors.size() + " error(s): " + errors);
        }

        // 3. Load entry XSLT as string
        Path entryPath = rootPath.resolve(project.entryPoint()).normalize();
        String xsltContent = Files.readString(entryPath, StandardCharsets.UTF_8);

        // 4. Apply library preprocessor
        String processed = LibraryPreprocessor.mergeLibraries(rootPath, xsltContent);

        // 5. Compile XSLT from string
        XsltExecutable executable = renderEngine.compileXslt(processed);

        // 6. Transform XML to FO
        Path xmlPath = rootPath.resolve(project.xmlInput()).normalize();
        String foContent = renderEngine.transformToString(xmlPath, executable);

        // 7. Render FO to PDF
        return renderEngine.renderFoToPdf(foContent);
    }

    public RenderResult renderSafe(Project project, Path rootPath) {
        try {
            // 1. Build dependency graph
            DependencyGraph graph = DependencyResolver.buildGraph(rootPath, project.entryPoint());

            // 2. Validate
            List<ValidationError> validationErrors = ValidationEngine.validateProject(rootPath, project, graph);
            if (!validationErrors.isEmpty()) {
                return RenderResult.failure(toRenderErrors(validationErrors));
            }

            // 3. Load entry XSLT as string
            Path entryPath = rootPath.resolve(project.entryPoint()).normalize();
            String xsltContent = Files.readString(entryPath, StandardCharsets.UTF_8);

            // 4. Apply library preprocessor
            String processed = LibraryPreprocessor.mergeLibraries(rootPath, xsltContent);

            // 5. Compile XSLT from string
            XsltExecutable executable = renderEngine.compileXslt(processed);

            // 6. Transform XML to FO
            Path xmlPath = rootPath.resolve(project.xmlInput()).normalize();
            String foContent = renderEngine.transformToString(xmlPath, executable);

            // 7. Render FO to PDF
            byte[] pdf = renderEngine.renderFoToPdf(foContent);

            return RenderResult.success(pdf);

        } catch (Exception e) {
            return RenderResult.failure(List.of(mapException(e)));
        }
    }

    private static RenderError mapException(Exception e) {
        String type;
        if (e instanceof SaxonApiException || e instanceof TransformerException) {
            type = "XSLT";
        } else if (e instanceof FOPException) {
            type = "FOP";
        } else if (e instanceof IOException) {
            type = "IO";
        } else {
            type = "UNKNOWN";
        }
        String message = (e.getMessage() == null || e.getMessage().isBlank())
                ? e.getClass().getSimpleName()
                : e.getMessage();
        return new RenderError(message, type);
    }

    private static List<RenderError> toRenderErrors(List<ValidationError> errors) {
        List<RenderError> result = new ArrayList<>(errors.size());
        for (ValidationError e : errors) {
            String location = e.file() != null ? e.file() + (e.line() != null ? ":" + e.line() : "") : null;
            result.add(new RenderError(e.message(), "XSLT", location));
        }
        return result;
    }
}

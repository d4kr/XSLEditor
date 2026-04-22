package ch.ti.gagi.xsleditor.render;

import ch.ti.gagi.xsleditor.dependency.DependencyGraph;
import ch.ti.gagi.xsleditor.dependency.DependencyResolver;
import ch.ti.gagi.xsleditor.error.ErrorManager;
import ch.ti.gagi.xsleditor.library.LibraryPreprocessor;
import ch.ti.gagi.xsleditor.model.Project;
import ch.ti.gagi.xsleditor.validation.ValidationEngine;
import ch.ti.gagi.xsleditor.validation.ValidationError;
import net.sf.saxon.s9api.XsltExecutable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class RenderOrchestrator {

    private final RenderEngine renderEngine = new RenderEngine();

    public byte[] render(Project project, Path rootPath) throws Exception {
        // Phase 2 D-03: project.entryPoint() may be null for partial projects.
        // Callers must ensure entryPoint and xmlInput are set before invoking render.
        // 1. Build dependency graph
        DependencyGraph graph = DependencyResolver.buildGraph(rootPath, project.entryPoint());

        // 2. Validate all files — fail immediately on errors
        List<ValidationError> errors = ValidationEngine.validateProject(rootPath, project, graph);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Validation failed with " + errors.size() + " error(s): " + errors);
        }

        // 3-7. Execute shared rendering pipeline
        return executePipeline(project, rootPath);
    }

    public RenderResult renderSafe(Project project, Path rootPath) {
        try {
            // 1. Build dependency graph
            DependencyGraph graph = DependencyResolver.buildGraph(rootPath, project.entryPoint());

            // 2. Validate
            List<ValidationError> validationErrors = ValidationEngine.validateProject(rootPath, project, graph);
            if (!validationErrors.isEmpty()) {
                return RenderResult.failure(ErrorManager.fromValidation(validationErrors));
            }

            // 3-7. Execute shared rendering pipeline
            return RenderResult.success(executePipeline(project, rootPath));

        } catch (Exception e) {
            return RenderResult.failure(List.of(ErrorManager.fromException(e)));
        }
    }

    /**
     * Executes the core rendering pipeline (steps 3-7): null-guards project fields,
     * loads and preprocesses XSLT, compiles, transforms XML to FO, and renders to PDF.
     *
     * @throws IllegalStateException if entryPoint or xmlInput is null
     * @throws Exception             on any I/O or rendering failure
     */
    private byte[] executePipeline(Project project, Path rootPath) throws Exception {
        // 3. Load entry XSLT as string
        if (project.entryPoint() == null) {
            throw new IllegalStateException("No XSLT entrypoint configured for this project");
        }
        Path entryPath = rootPath.resolve(project.entryPoint()).normalize();
        String xsltContent = Files.readString(entryPath, StandardCharsets.UTF_8);

        // 4. Apply library preprocessor
        String processed = LibraryPreprocessor.mergeLibraries(rootPath, xsltContent);

        // 5. Compile XSLT from string
        XsltExecutable executable = renderEngine.compileXslt(processed);

        // 6. Transform XML to FO
        if (project.xmlInput() == null) {
            throw new IllegalStateException("No XML input configured for this project");
        }
        Path xmlPath = rootPath.resolve(project.xmlInput()).normalize();
        String foContent = renderEngine.transformToString(xmlPath, executable);

        // 7. Render FO to PDF
        return renderEngine.renderFoToPdf(foContent);
    }
}

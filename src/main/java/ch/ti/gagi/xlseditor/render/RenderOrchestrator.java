package ch.ti.gagi.xlseditor.render;

import ch.ti.gagi.xlseditor.dependency.DependencyGraph;
import ch.ti.gagi.xlseditor.dependency.DependencyResolver;
import ch.ti.gagi.xlseditor.library.LibraryPreprocessor;
import ch.ti.gagi.xlseditor.model.Project;
import ch.ti.gagi.xlseditor.validation.ValidationEngine;
import ch.ti.gagi.xlseditor.validation.ValidationError;
import net.sf.saxon.s9api.XsltExecutable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
}

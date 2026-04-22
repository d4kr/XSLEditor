package ch.ti.gagi.xlseditor.validation;

import ch.ti.gagi.xlseditor.dependency.DependencyGraph;
import ch.ti.gagi.xlseditor.model.Project;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ValidationEngine {

    public static List<ValidationError> validateXml(Path file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            factory.newDocumentBuilder().parse(file.toFile());
            return List.of();

        } catch (SAXParseException e) {
            return List.of(new ValidationError(
                    e.getMessage(),
                    file,
                    e.getLineNumber(),
                    e.getColumnNumber()
            ));
        } catch (Exception e) {
            return List.of(new ValidationError(
                    e.getMessage(),
                    file,
                    null,
                    null
            ));
        }
    }

    public static List<ValidationError> validateProject(Path rootPath, Project project, DependencyGraph graph) {
        Set<Path> files = new LinkedHashSet<>();

        for (Path path : graph.edges().keySet()) {
            files.add(rootPath.resolve(path).normalize());
        }

        files.add(rootPath.resolve(project.xmlInput()).normalize());

        return validateAll(new ArrayList<>(files));
    }

    public static List<ValidationError> validateAll(List<Path> files) {
        List<ValidationError> errors = new ArrayList<>();

        for (Path file : files) {
            errors.addAll(validateXml(file));
        }

        return errors;
    }
}
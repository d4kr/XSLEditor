package ch.ti.gagi.xlseditor.validation;

import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    public static List<ValidationError> validateAll(List<Path> files) {
        List<ValidationError> errors = new ArrayList<>();

        for (Path file : files) {
            errors.addAll(validateXml(file));
        }

        return errors;
    }
}
package ch.ti.gagi.xlseditor.error;

import ch.ti.gagi.xlseditor.render.RenderError;
import ch.ti.gagi.xlseditor.validation.ValidationError;
import net.sf.saxon.s9api.SaxonApiException;
import org.apache.fop.apps.FOPException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ErrorManager {

    private ErrorManager() {}

    public static RenderError fromException(Exception e) {
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

    public static List<RenderError> fromValidation(List<ValidationError> errors) {
        List<RenderError> result = new ArrayList<>(errors.size());
        for (ValidationError e : errors) {
            String location = e.file() != null ? e.file() + (e.line() != null ? ":" + e.line() : "") : null;
            result.add(new RenderError(e.message(), "XSLT", location));
        }
        return result;
    }
}

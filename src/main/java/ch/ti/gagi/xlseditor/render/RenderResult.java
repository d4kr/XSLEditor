package ch.ti.gagi.xlseditor.render;

import ch.ti.gagi.xlseditor.validation.ValidationError;

import java.util.List;

public final class RenderResult {

    private final boolean success;
    private final byte[] pdf;
    private final List<ValidationError> errors;

    private RenderResult(boolean success, byte[] pdf, List<ValidationError> errors) {
        this.success = success;
        this.pdf = pdf;
        this.errors = errors;
    }

    public static RenderResult success(byte[] pdf) {
        return new RenderResult(true, pdf, List.of());
    }

    public static RenderResult failure(List<ValidationError> errors) {
        return new RenderResult(false, null, List.copyOf(errors));
    }

    public boolean success() { return success; }
    public byte[] pdf() { return pdf; }
    public List<ValidationError> errors() { return errors; }
}

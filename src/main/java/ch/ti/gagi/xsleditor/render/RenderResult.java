package ch.ti.gagi.xlseditor.render;

import java.util.List;

public final class RenderResult {

    private final boolean success;
    private final byte[] pdf;
    private final List<RenderError> errors;

    private RenderResult(boolean success, byte[] pdf, List<RenderError> errors) {
        this.success = success;
        this.pdf = pdf;
        this.errors = errors;
    }

    public static RenderResult success(byte[] pdf) {
        return new RenderResult(true, pdf, List.of());
    }

    public static RenderResult failure(List<RenderError> errors) {
        return new RenderResult(false, null, List.copyOf(errors));
    }

    public boolean success() { return success; }
    public byte[] pdf() { return pdf; }
    public List<RenderError> errors() { return errors; }
}

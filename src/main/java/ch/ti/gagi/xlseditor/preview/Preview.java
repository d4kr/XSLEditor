package ch.ti.gagi.xlseditor.preview;

import ch.ti.gagi.xlseditor.render.RenderError;

import java.util.List;

public final class Preview {

    private final boolean success;
    private final byte[] pdf;
    private final List<RenderError> errors;

    private Preview(boolean success, byte[] pdf, List<RenderError> errors) {
        this.success = success;
        this.pdf = pdf;
        this.errors = errors;
    }

    public static Preview success(byte[] pdf) {
        return new Preview(true, pdf, List.of());
    }

    public static Preview failure(List<RenderError> errors) {
        return new Preview(false, null, List.copyOf(errors));
    }

    public boolean success() { return success; }
    public byte[] pdf() { return pdf; }
    public List<RenderError> errors() { return errors; }
}

package ch.ti.gagi.xlseditor.preview;

import ch.ti.gagi.xlseditor.render.RenderError;

import java.util.List;

public final class Preview {

    private final boolean success;
    private final boolean outdated;
    private final byte[] pdf;
    private final List<RenderError> errors;

    private Preview(boolean success, boolean outdated, byte[] pdf, List<RenderError> errors) {
        this.success = success;
        this.outdated = outdated;
        this.pdf = pdf;
        this.errors = errors;
    }

    public static Preview success(byte[] pdf) {
        return new Preview(true, false, pdf, List.of());
    }

    public static Preview failure(List<RenderError> errors) {
        return new Preview(false, true, null, List.copyOf(errors));
    }

    public boolean success() { return success; }
    public boolean outdated() { return outdated; }
    public byte[] pdf() { return pdf; }
    public List<RenderError> errors() { return errors; }
}

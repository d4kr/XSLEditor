package ch.ti.gagi.xsleditor.preview;

import java.util.List;

public final class Preview {

    private final boolean success;
    private final boolean outdated;
    private final byte[] pdf;
    private final List<PreviewError> errors;

    private Preview(boolean success, boolean outdated, byte[] pdf, List<PreviewError> errors) {
        this.success = success;
        this.outdated = outdated;
        this.pdf = pdf;
        this.errors = errors;
    }

    public static Preview success(byte[] pdf) {
        return new Preview(true, false, pdf, List.of());
    }

    public static Preview failure(List<PreviewError> errors) {
        return new Preview(false, true, null, List.copyOf(errors));
    }

    public boolean success() { return success; }
    public boolean outdated() { return outdated; }
    public byte[] pdf() { return pdf; }
    public List<PreviewError> errors() { return errors; }
}

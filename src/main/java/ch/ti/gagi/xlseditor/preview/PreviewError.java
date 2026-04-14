package ch.ti.gagi.xlseditor.preview;

public final class PreviewError {

    private final String message;
    private final String type;
    private final String file;
    private final Integer line;

    public PreviewError(String message, String type, String file, Integer line) {
        this.message = message;
        this.type = type;
        this.file = file;
        this.line = line;
    }

    public String message() { return message; }
    public String type() { return type; }
    public String file() { return file; }
    public Integer line() { return line; }
}

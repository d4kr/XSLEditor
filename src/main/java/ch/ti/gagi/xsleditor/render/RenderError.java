package ch.ti.gagi.xsleditor.render;

public record RenderError(String message, String type, String location) {

    public RenderError(String message, String type) {
        this(message, type, null);
    }
}

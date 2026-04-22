package ch.ti.gagi.xsleditor.validation;

import java.nio.file.Path;

public record ValidationError(
        String message,
        Path file,
        Integer line,
        Integer column
) {
}
package ch.ti.gagi.xlseditor.validation;

import java.nio.file.Path;

public record ValidationError(
        String message,
        Path file,
        Integer line,
        Integer column
) {
}
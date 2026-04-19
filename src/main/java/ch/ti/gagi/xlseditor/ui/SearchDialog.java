package ch.ti.gagi.xlseditor.ui;

import java.nio.file.Path;
import java.util.List;

/**
 * Dialog for multi-file text search across all project files.
 * Results are displayed in a ListView and support navigation to the matched line.
 * Implementation will be added in Wave 2.
 */
public class SearchDialog {

    /**
     * Represents a single search result hit.
     * Wave 2: this record is the data carrier for the search ListView.
     */
    public record SearchHit(Path file, int line, int column, String lineText) {
        @Override
        public String toString() {
            return file.getFileName() + ":" + (line + 1) + "  " + lineText.strip();
        }
    }

    /**
     * Searches all files under {@code projectRoot} for lines containing {@code query}.
     * Returns a list of {@link SearchHit} records, one per matching line.
     * This static method is extracted from the Task.call() lambda for testability.
     * Wave 2: implement with Files.walk + line-by-line grep.
     */
    public static List<SearchHit> search(Path projectRoot, String query) {
        // Skeleton: returns empty list until Wave 2 implements the file scanner
        return List.of();
    }
}

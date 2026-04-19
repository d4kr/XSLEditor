package ch.ti.gagi.xlseditor.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchTaskTest {

    @Test
    void findsMatchInFixtureFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("template.xsl");
        Files.writeString(file, "line zero\nhello world\nline two", StandardCharsets.UTF_8);
        // SearchDialog.search() is a static helper extracted from the Task.call() lambda
        List<SearchDialog.SearchHit> hits = SearchDialog.search(tempDir, "world");
        assertEquals(1, hits.size(), "Must find exactly one hit for 'world'");
        assertEquals(1, hits.get(0).line(), "Hit must be on line index 1 (0-based)");
        assertTrue(hits.get(0).lineText().contains("world"));
    }

    @Test
    void returnsEmptyForNoMatch(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("template.xsl");
        Files.writeString(file, "hello world", StandardCharsets.UTF_8);
        List<SearchDialog.SearchHit> hits = SearchDialog.search(tempDir, "xyz_not_found_qqqq");
        assertTrue(hits.isEmpty(), "No match must return empty list, not throw");
    }
}

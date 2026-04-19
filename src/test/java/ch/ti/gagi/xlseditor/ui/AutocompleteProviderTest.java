package ch.ti.gagi.xlseditor.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutocompleteProviderTest {

    @Test
    void getMatchesReturnsSubsetMatchingPrefix() {
        List<String> matches = AutocompleteProvider.getMatches("xsl:if");
        assertTrue(matches.contains("xsl:if"), "Prefix 'xsl:if' must return 'xsl:if'");
        assertFalse(matches.contains("fo:block"), "Prefix 'xsl:if' must not return 'fo:block'");
    }

    @Test
    void getMatchesReturnsAllOnEmptyPrefix() {
        List<String> matches = AutocompleteProvider.getMatches("");
        assertTrue(matches.size() >= 20,
            "Empty prefix must return the full keyword list (at least 20 entries)");
    }

    @Test
    void getMatchesReturnsEmptyForUnknownPrefix() {
        List<String> matches = AutocompleteProvider.getMatches("xyz:nonexistent");
        assertTrue(matches.isEmpty(), "Unknown prefix must return empty list, not throw");
    }
}

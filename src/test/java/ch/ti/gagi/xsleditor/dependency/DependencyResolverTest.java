package ch.ti.gagi.xsleditor.dependency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DependencyResolverTest {

    private static final String XSL = "xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"";

    private static String stylesheet(String body) {
        return "<?xml version=\"1.0\"?><xsl:stylesheet " + XSL + " version=\"1.0\">"
             + body + "</xsl:stylesheet>";
    }

    @Test
    void parseIncludesReturnsHrefsInOrder(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("main.xsl"),
                stylesheet("<xsl:include href=\"a.xsl\"/><xsl:include href=\"b.xsl\"/>"));
        List<Path> hrefs = DependencyResolver.parseIncludes(tempDir.resolve("main.xsl"));
        assertEquals(List.of(Path.of("a.xsl"), Path.of("b.xsl")), hrefs);
    }

    @Test
    void parseImportsReturnsHrefs(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("main.xsl"),
                stylesheet("<xsl:import href=\"common.xsl\"/>"));
        List<Path> hrefs = DependencyResolver.parseImports(tempDir.resolve("main.xsl"));
        assertEquals(List.of(Path.of("common.xsl")), hrefs);
    }

    @Test
    void buildGraphDiscoversTransitiveDependencies(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("a.xsl"), stylesheet("<xsl:include href=\"b.xsl\"/>"));
        Files.writeString(tempDir.resolve("b.xsl"), stylesheet("<xsl:import href=\"c.xsl\"/>"));
        Files.writeString(tempDir.resolve("c.xsl"), stylesheet(""));

        DependencyGraph graph = DependencyResolver.buildGraph(tempDir, Path.of("a.xsl"));

        assertTrue(graph.edges().containsKey(Path.of("a.xsl")));
        assertTrue(graph.edges().containsKey(Path.of("b.xsl")));
        assertTrue(graph.edges().containsKey(Path.of("c.xsl")));
        assertEquals(List.of(Path.of("b.xsl")), graph.dependenciesOf(Path.of("a.xsl")));
        assertEquals(List.of(Path.of("c.xsl")), graph.dependenciesOf(Path.of("b.xsl")));
        assertEquals(List.of(), graph.dependenciesOf(Path.of("c.xsl")));
    }

    @Test
    void buildGraphDetectsCircularDependency(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("a.xsl"), stylesheet("<xsl:include href=\"b.xsl\"/>"));
        Files.writeString(tempDir.resolve("b.xsl"), stylesheet("<xsl:include href=\"a.xsl\"/>"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> DependencyResolver.buildGraph(tempDir, Path.of("a.xsl")));
        assertTrue(ex.getMessage().startsWith("Circular dependency detected"));
    }

    @Test
    void buildGraphReturnsSingleEntryForLeafStylesheet(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("main.xsl"), stylesheet(""));

        DependencyGraph graph = DependencyResolver.buildGraph(tempDir, Path.of("main.xsl"));

        assertEquals(1, graph.edges().size());
        assertEquals(List.of(), graph.dependenciesOf(Path.of("main.xsl")));
    }
}

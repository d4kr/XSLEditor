package ch.ti.gagi.xlseditor.dependency;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DependencyResolver {

    private static final String XSL_NS = "http://www.w3.org/1999/XSL/Transform";

    public static List<Path> parseIncludes(Path xsltFile) throws Exception {
        return parseHrefs(xsltFile, "include");
    }

    public static List<Path> parseImports(Path xsltFile) throws Exception {
        return parseHrefs(xsltFile, "import");
    }

    /**
     * Builds a dependency graph starting from entryPoint, resolved against rootPath.
     * Keys and values are paths relative to rootPath.
     */
    public static DependencyGraph buildGraph(Path rootPath, Path entryPoint) throws Exception {
        Map<Path, List<Path>> edges = new LinkedHashMap<>();
        collect(rootPath, entryPoint, edges, new LinkedHashSet<>());
        return new DependencyGraph(edges);
    }

    private static void collect(Path rootPath, Path relPath, Map<Path, List<Path>> edges, Set<Path> stack) throws Exception {
        if (stack.contains(relPath)) {
            throw new IllegalStateException("Circular dependency detected: " + stack + " -> " + relPath);        }
        if (edges.containsKey(relPath)) {
            return;
        }

        Path absFile = rootPath.resolve(relPath);
        Path fileDir = relPath.getParent();

        List<Path> hrefs = new ArrayList<>();
        hrefs.addAll(parseIncludes(absFile));
        hrefs.addAll(parseImports(absFile));

        List<Path> deps = new ArrayList<>();
        for (Path href : hrefs) {
            Path resolved = fileDir != null ? fileDir.resolve(href).normalize() : href.normalize();
            deps.add(resolved);
        }

        stack.add(relPath);
        edges.put(relPath, deps);

        for (Path dep : deps) {
            collect(rootPath, dep, edges, stack);
        }

        stack.remove(relPath);
    }

    private static List<Path> parseHrefs(Path xsltFile, String localName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        Document doc = factory
                .newDocumentBuilder()
                .parse(xsltFile.toFile());

        NodeList nodes = doc.getElementsByTagNameNS(XSL_NS, localName);

        List<Path> paths = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            var attr = nodes.item(i).getAttributes().getNamedItem("href");
            if (attr != null) {
                paths.add(Path.of(attr.getNodeValue()));
            }
        }
        return paths;
    }
}
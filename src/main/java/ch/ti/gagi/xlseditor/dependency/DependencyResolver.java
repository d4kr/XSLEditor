package ch.ti.gagi.xlseditor.dependency;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DependencyResolver {

    private static final String XSL_NS = "http://www.w3.org/1999/XSL/Transform";

    public static List<Path> parseIncludes(Path xsltFile) throws Exception {
        return parseHrefs(xsltFile, "include");
    }

    public static List<Path> parseImports(Path xsltFile) throws Exception {
        return parseHrefs(xsltFile, "import");
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
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

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        Document doc = factory
                .newDocumentBuilder()
                .parse(xsltFile.toFile());

        NodeList nodes = doc.getElementsByTagNameNS(XSL_NS, "include");

        List<Path> includes = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            var node = nodes.item(i);
            var attr = node.getAttributes().getNamedItem("href");

            if (attr != null) {
                includes.add(Path.of(attr.getNodeValue()));
            }
        }

        return includes;
    }
}
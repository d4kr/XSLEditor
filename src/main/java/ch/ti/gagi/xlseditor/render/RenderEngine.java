package ch.ti.gagi.xlseditor.render;

import net.sf.saxon.s9api.*;

import javax.xml.transform.stream.StreamSource;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Objects;

public final class RenderEngine {

    private final Processor processor = new Processor(false);
    private final XsltCompiler compiler = processor.newXsltCompiler();

    public XsltExecutable compileXslt(Path xsltFile) throws SaxonApiException {
        Objects.requireNonNull(xsltFile);
        return compiler.compile(new StreamSource(xsltFile.toFile()));
    }

    public String transformToString(Path xmlFile, XsltExecutable executable) throws SaxonApiException {
        Objects.requireNonNull(xmlFile);
        Objects.requireNonNull(executable);

        StringWriter writer = new StringWriter();
        Serializer serializer = processor.newSerializer(writer);

        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        serializer.setOutputProperty(Serializer.Property.ENCODING, "UTF-8");

        XsltTransformer transformer = executable.load();
        transformer.setSource(new StreamSource(xmlFile.toFile()));
        transformer.setDestination(serializer);
        transformer.transform();

        return writer.toString();
    }
}
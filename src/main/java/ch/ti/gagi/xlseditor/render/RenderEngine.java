package ch.ti.gagi.xlseditor.render;

import net.sf.saxon.s9api.*;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

public final class RenderEngine {

    private final Processor processor = new Processor(false);
    private final XsltCompiler compiler = processor.newXsltCompiler();
    private final FopFactory fopFactory;

    public RenderEngine() {
        try {
            fopFactory = FopFactory.newInstance(new java.net.URI("."));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize FopFactory", e);
        }
    }

    public XsltExecutable compileXslt(Path xsltFile) throws SaxonApiException {
        Objects.requireNonNull(xsltFile);
        return compiler.compile(new StreamSource(xsltFile.toFile()));
    }

    public XsltExecutable compileXslt(String content) throws SaxonApiException {
        Objects.requireNonNull(content);
        return compiler.compile(new StreamSource(new StringReader(content)));
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

    public byte[] renderFoToPdf(String foContent) throws Exception {
        Objects.requireNonNull(foContent);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

        javax.xml.transform.Transformer transformer =
                TransformerFactory.newDefaultInstance().newTransformer();

        StreamSource source = new StreamSource(
                new ByteArrayInputStream(foContent.getBytes(StandardCharsets.UTF_8))
        );

        SAXResult result = new SAXResult(fop.getDefaultHandler());
        transformer.transform(source, result);

        return out.toByteArray();
    }
}
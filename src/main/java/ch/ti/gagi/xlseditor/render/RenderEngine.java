package ch.ti.gagi.xlseditor.render;

import net.sf.saxon.s9api.*;

import javax.xml.transform.stream.StreamSource;
import java.nio.file.Path;
import java.util.Objects;

public final class RenderEngine {

    private final Processor processor = new Processor(false);
    private final XsltCompiler compiler = processor.newXsltCompiler();

    public XsltExecutable compileXslt(Path xsltFile) throws SaxonApiException {
        Objects.requireNonNull(xsltFile);
        return compiler.compile(new StreamSource(xsltFile.toFile()));
    }
}
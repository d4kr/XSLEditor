package ch.ti.gagi.xsleditor.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XmlCharsetDetector {

    private static final Logger LOG = Logger.getLogger(XmlCharsetDetector.class.getName());
    private static final Pattern ENCODING_ATTR =
        Pattern.compile("encoding=[\"']([^\"']+)[\"']");

    private XmlCharsetDetector() {}

    /**
     * Detects the charset of a file by inspecting its BOM and XML encoding declaration.
     * Falls back to UTF-8 if neither is present or if the declared charset is unsupported.
     */
    public static Charset detect(Path path) throws IOException {
        byte[] header = readHeader(path);

        // BOM check (must run before XML declaration scan)
        if (header.length >= 3
                && header[0] == (byte) 0xEF
                && header[1] == (byte) 0xBB
                && header[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (header.length >= 2 && header[0] == (byte) 0xFF && header[1] == (byte) 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (header.length >= 2 && header[0] == (byte) 0xFE && header[1] == (byte) 0xFF) {
            return StandardCharsets.UTF_16BE;
        }

        // XML declaration — header bytes read as ISO-8859-1 (1:1 byte->char, ASCII-safe)
        String ascii = new String(header, StandardCharsets.ISO_8859_1);
        Matcher m = ENCODING_ATTR.matcher(ascii);
        if (m.find()) {
            try {
                return Charset.forName(m.group(1));
            } catch (Exception e) {
                LOG.warning("Unsupported charset declared in XML: " + m.group(1) + " -- falling back to UTF-8");
            }
        }

        return StandardCharsets.UTF_8;
    }

    private static byte[] readHeader(Path path) throws IOException {
        try (var in = Files.newInputStream(path)) {
            return in.readNBytes(200);
        }
    }
}

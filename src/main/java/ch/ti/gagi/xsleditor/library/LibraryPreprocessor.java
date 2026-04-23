package ch.ti.gagi.xsleditor.library;

import ch.ti.gagi.xsleditor.util.XmlCharsetDetector;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LibraryPreprocessor {

    private static final Pattern DIRECTIVE =
            Pattern.compile("<\\?LIBRARY\\s+(\\S+?)\\s*\\?>");

    public static List<String> detectLibraries(String content) {
        Matcher matcher = DIRECTIVE.matcher(content);
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    public static List<Path> resolveLibraries(Path rootPath, List<String> names) {
        List<Path> paths = new ArrayList<>(names.size());
        for (String name : names) {
            paths.add(rootPath.resolve(name + ".xsl").normalize());
        }
        return paths;
    }

    public static String mergeLibraries(Path rootPath, String content) throws LibraryProcessingException {
        Matcher matcher = DIRECTIVE.matcher(content);
        StringBuilder result = new StringBuilder();
        Map<String, String> cache = new HashMap<>();
        int last = 0;

        while (matcher.find()) {
            result.append(content, last, matcher.start());

            String name = matcher.group(1);
            if (!cache.containsKey(name)) {
                Path file = rootPath.resolve(name + ".xsl").normalize();
                if (!Files.exists(file)) {
                    throw new LibraryProcessingException("Library file not found: " + file);
                }
                try {
                    Charset libCs = XmlCharsetDetector.detect(file);
                    cache.put(name, Files.readString(file, libCs));
                } catch (IOException e) {
                    throw new LibraryProcessingException("Failed to read library file: " + file, e);
                }
            }

            result.append(cache.get(name));
            last = matcher.end();
        }

        result.append(content, last, content.length());
        return result.toString();
    }
}
package ch.ti.gagi.xlseditor.library;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    public static String mergeLibraries(Path rootPath, String content) throws IOException {
        Matcher matcher = DIRECTIVE.matcher(content);
        StringBuilder result = new StringBuilder();

        // Simple cache to avoid re-reading the same file
        Map<String, String> cache = new HashMap<>();

        int last = 0;

        while (matcher.find()) {
            result.append(content, last, matcher.start());

            String name = matcher.group(1);
            String libContent = cache.computeIfAbsent(name, n -> {
                try {
                    Path file = rootPath.resolve(n + ".xsl").normalize();
                    if (!Files.exists(file)) {
                        throw new RuntimeException("Library file not found: " + file);
                    }
                    return Files.readString(file, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            result.append(libContent);
            last = matcher.end();
        }

        result.append(content, last, content.length());

        return result.toString();
    }
}
package ch.ti.gagi.xlseditor.library;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LibraryPreprocessor {

    private static final Pattern DIRECTIVE = Pattern.compile("<\\?LIBRARY\\s+(\\S+?)\\s*\\?>");

    public static List<String> detectLibraries(String content) {
        Matcher matcher = DIRECTIVE.matcher(content);
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }
}

package ch.ti.gagi.xlseditor.log;

import java.util.ArrayList;
import java.util.List;

public final class LogManager {

    private final List<LogEntry> entries = new ArrayList<>();

    public void add(LogEntry entry) {
        entries.add(entry);
    }

    public void info(String message) { add(message, "INFO"); }
    public void warn(String message) { add(message, "WARN"); }
    public void error(String message) { add(message, "ERROR"); }

    private void add(String message, String level) {
        entries.add(new LogEntry(message, level, System.currentTimeMillis()));
    }

    public List<LogEntry> getAll() {
        return List.copyOf(entries);
    }

    public List<LogEntry> getByLevel(String level) {
        List<LogEntry> result = new ArrayList<>();
        for (LogEntry entry : entries) {
            if (entry.level().equals(level)) {
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }
}

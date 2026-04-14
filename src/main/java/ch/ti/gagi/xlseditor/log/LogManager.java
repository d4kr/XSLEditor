package ch.ti.gagi.xlseditor.log;

import java.util.ArrayList;
import java.util.List;

public final class LogManager {

    private final List<LogEntry> entries = new ArrayList<>();

    public void add(LogEntry entry) {
        entries.add(entry);
    }

    public List<LogEntry> getAll() {
        return List.copyOf(entries);
    }
}

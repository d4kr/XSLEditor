package ch.ti.gagi.xlseditor.log;

public final class LogEntry {

    private final String message;
    private final String level;
    private final long timestamp;

    public LogEntry(String message, String level, long timestamp) {
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
    }

    public String message() { return message; }
    public String level() { return level; }
    public long timestamp() { return timestamp; }
}

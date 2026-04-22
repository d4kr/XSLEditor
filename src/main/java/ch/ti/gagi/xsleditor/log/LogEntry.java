package ch.ti.gagi.xsleditor.log;

public final class LogEntry {

    private final String message;
    private final String level;
    private final long timestamp;
    private final String type;
    private final String file;
    private final Integer line;

    public LogEntry(String message, String level, long timestamp,
                    String type, String file, Integer line) {
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
        this.type = type;
        this.file = file;
        this.line = line;
    }

    public LogEntry(String message, String level, long timestamp) {
        this(message, level, timestamp, null, null, null);
    }

    public String message()  { return message; }
    public String level()    { return level; }
    public long timestamp()  { return timestamp; }
    public String type()     { return type; }
    public String file()     { return file; }
    public Integer line()    { return line; }
}

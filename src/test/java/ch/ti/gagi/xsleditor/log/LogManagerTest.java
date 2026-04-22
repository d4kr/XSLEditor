package ch.ti.gagi.xsleditor.log;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogManagerTest {

    @Test
    void emptyManagerReturnsEmptyList() {
        LogManager log = new LogManager();
        assertTrue(log.getAll().isEmpty());
    }

    @Test
    void addPreservesInsertionOrder() {
        LogManager log = new LogManager();
        log.add(new LogEntry("first", "INFO", 1L));
        log.add(new LogEntry("second", "WARN", 2L));
        log.add(new LogEntry("third", "ERROR", 3L));
        List<LogEntry> entries = log.getAll();
        assertEquals(3, entries.size());
        assertEquals("first", entries.get(0).message());
        assertEquals("second", entries.get(1).message());
        assertEquals("third", entries.get(2).message());
    }

    @Test
    void infoWarnErrorConvenienceMethodsSetCorrectLevels() {
        LogManager log = new LogManager();
        log.info("i");
        log.warn("w");
        log.error("e");
        List<LogEntry> entries = log.getAll();
        assertEquals("INFO", entries.get(0).level());
        assertEquals("WARN", entries.get(1).level());
        assertEquals("ERROR", entries.get(2).level());
        assertEquals("i", entries.get(0).message());
        assertEquals("w", entries.get(1).message());
        assertEquals("e", entries.get(2).message());
    }

    @Test
    void getByLevelReturnsOnlyMatchingEntries() {
        LogManager log = new LogManager();
        log.info("i1");
        log.error("e1");
        log.info("i2");
        log.warn("w1");
        List<LogEntry> infos = log.getByLevel("INFO");
        assertEquals(2, infos.size());
        assertEquals("i1", infos.get(0).message());
        assertEquals("i2", infos.get(1).message());
        List<LogEntry> errors = log.getByLevel("ERROR");
        assertEquals(1, errors.size());
        assertEquals("e1", errors.get(0).message());
    }

    @Test
    void getByLevelReturnsEmptyForUnknownLevel() {
        LogManager log = new LogManager();
        log.info("x");
        assertTrue(log.getByLevel("DEBUG").isEmpty());
    }

    @Test
    void getAllReturnsImmutableCopy() {
        LogManager log = new LogManager();
        log.info("x");
        List<LogEntry> snapshot = log.getAll();
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(new LogEntry("y", "INFO", 0L)));
    }
}

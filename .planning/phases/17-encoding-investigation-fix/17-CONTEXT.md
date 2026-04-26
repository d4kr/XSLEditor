# Phase 17: Encoding Investigation & Fix — Context

**Gathered:** 2026-04-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix the root cause of non-ASCII character garbling in the editor. Files that declare `encoding="windows-1252"` (or any non-UTF-8 charset) in their XML declaration are read with `Files.readString(path, UTF_8)`, which substitutes U+FFFD replacement chars for any byte that is not valid UTF-8. The fix is to detect the declared charset from the file header and read with the correct charset. Save must also use the detected charset to preserve round-trip encoding.

**No new UI features, no render pipeline redesign.**

</domain>

<decisions>
## Implementation Decisions

### Root Cause (ENC-01)
- **D-01:** Root cause confirmed by user inspection. Files declare `<?xml version="1.0" encoding="windows-1252" standalone="no"?>` at the top. `EditorController.openOrFocusTab()` calls `Files.readString(key, StandardCharsets.UTF_8)` which ignores this declaration. Windows-1252 bytes for `ä` (0xE4) and `ü` (0xFC) are not valid UTF-8 sequences → Java substitutes U+FFFD → displayed as `?` in the editor.

### Detection Strategy (ENC-02)
- **D-02:** New utility class `ch.ti.gagi.xsleditor.util.XmlCharsetDetector` with a single static method `detect(Path) → Charset`. Read the first 200 bytes of the file as ISO-8859-1 (guaranteed 1:1 byte→char), apply regex `encoding=["']([^"']+)["']` to extract the declared charset name, return `Charset.forName(name)` or fall back to UTF-8 on any failure.
- **D-03:** Also check for BOM before the XML declaration: UTF-8 BOM = EF BB BF → return UTF-8. UTF-16 LE = FF FE → return UTF-16LE. UTF-16 BE = FE FF → return UTF-16BE. BOM check runs before XML declaration scan.
- **D-04:** If no XML declaration is present (plain text files, non-XML files opened in the editor), default to UTF-8.

### Editor Read Path (ENC-02)
- **D-05:** `EditorController.openOrFocusTab()` — replace `Files.readString(key, StandardCharsets.UTF_8)` with two steps: `Charset cs = XmlCharsetDetector.detect(key)` then `Files.readString(key, cs)`.
- **D-06:** `EditorTab` gets a new `public final Charset charset` field. Constructor gains a `Charset charset` parameter. Stored at open time.

### Editor Save Path (round-trip integrity)
- **D-07:** `EditorController.saveTab()` — replace `StandardCharsets.UTF_8` with `editorTab.charset`.
- **D-08:** `EditorController.saveAll()` — same: replace `StandardCharsets.UTF_8` with `et.charset`.
- **D-09:** Rationale: saving a Windows-1252 file as UTF-8 would leave the XML declaration stale (`encoding="windows-1252"` but bytes are UTF-8). Round-trip preservation is the correct behavior for a file editor.

### Render Pipeline (ENC-01 scope)
- **D-10:** `RenderOrchestrator.executePipeline()` line 69 reads the XSLT entrypoint as a string with UTF-8 before passing to the LibraryPreprocessor and then to `compileXslt(String)`. If the XSLT file is in Windows-1252, string literals and comments with non-ASCII chars would also be garbled. Fix: use `XmlCharsetDetector.detect(entryPath)` here too.
- **D-11:** `LibraryPreprocessor` reads included library files with UTF-8. Fix: same — use `XmlCharsetDetector.detect(file)` when reading each library file.

### ENC-03 (Log Panel)
- **D-12:** Log panel messages are Java `String` objects from Saxon/FOP exception messages and our own constructed strings. Saxon processes XML via byte streams when given a `Path` (`compileXslt(Path xsltFile)` uses `StreamSource(file)` — Saxon handles encoding natively). Log garbling would only surface if Saxon receives a garbled string from `RenderOrchestrator`. After D-10 fixes the read path, ENC-03 is covered transitively. No log-panel-specific code changes needed.

### Claude's Discretion
- Whether to add a `SearchDialog` fix (also reads with UTF-8 via `Files.readAllLines`). Not required by ENC-01..03 but adjacent. Implement if trivial.
- Error message wording if `Charset.forName()` throws `UnsupportedCharsetException` (log a warning and fall back to UTF-8, do not crash).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Files to Modify
- `src/main/java/ch/ti/gagi/xsleditor/ui/EditorTab.java` — add `Charset charset` field and constructor parameter
- `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java` — `openOrFocusTab()` (line 136), `saveTab()` (line 301), `saveAll()` (line 319)
- `src/main/java/ch/ti/gagi/xsleditor/render/RenderOrchestrator.java` — `executePipeline()` (line 69)
- `src/main/java/ch/ti/gagi/xsleditor/library/LibraryPreprocessor.java` — file read at line 49

### New File to Create
- `src/main/java/ch/ti/gagi/xsleditor/util/XmlCharsetDetector.java` — static `detect(Path) → Charset` utility

### Read-Only Reference
- `src/main/java/ch/ti/gagi/xsleditor/render/RenderEngine.java` — `compileXslt(String)` receives a pre-read string; no change needed here

### Requirements
- `.planning/REQUIREMENTS.md` — ENC-01, ENC-02, ENC-03

</canonical_refs>

<code_context>
## Existing Code Insights

### Current broken call sites
- `EditorController:136` — `Files.readString(key, StandardCharsets.UTF_8)` — PRIMARY FIX
- `EditorController:301` — `Files.writeString(editorTab.path, ..., StandardCharsets.UTF_8)` — save must use stored charset
- `EditorController:319` — `Files.writeString(et.path, ..., StandardCharsets.UTF_8)` — saveAll must use stored charset
- `RenderOrchestrator:69` — `Files.readString(entryPath, StandardCharsets.UTF_8)` — XSLT preprocessing read
- `LibraryPreprocessor:49` — `Files.readString(file, StandardCharsets.UTF_8)` — library include read

### EditorTab constructor order (critical — do not break)
Pitfall documented in EditorTab.java: replaceText → mark() → forgetHistory(). Adding a `charset` parameter to the constructor does NOT affect this order.

### Pattern: compile xslt from Path (no change needed)
`RenderEngine.compileXslt(Path xsltFile)` uses `StreamSource(xsltFile.toFile())` — Saxon reads bytes and respects XML encoding declaration natively. No fix needed for this overload.

### No runtime platform dependency
`XmlCharsetDetector` reads raw bytes — no platform charset involved. Works identically on macOS, Windows, Linux.

</code_context>

<specifics>
## Specific Ideas

### XmlCharsetDetector implementation sketch
```java
package ch.ti.gagi.xsleditor.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XmlCharsetDetector {

    private static final Pattern ENCODING_ATTR =
        Pattern.compile("encoding=[\"']([^\"']+)[\"']");

    private XmlCharsetDetector() {}

    public static Charset detect(Path path) throws IOException {
        byte[] header = readHeader(path, 200);

        // BOM check
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

        // XML declaration
        String ascii = new String(header, StandardCharsets.ISO_8859_1);
        Matcher m = ENCODING_ATTR.matcher(ascii);
        if (m.find()) {
            try {
                return Charset.forName(m.group(1));
            } catch (Exception ignored) {
                // unsupported charset — fall through to UTF-8
            }
        }

        return StandardCharsets.UTF_8;
    }

    private static byte[] readHeader(Path path, int maxBytes) throws IOException {
        try (var in = Files.newInputStream(path)) {
            return in.readNBytes(maxBytes);
        }
    }
}
```

### EditorTab change
```java
// New field:
public final Charset charset;

// Constructor signature:
public EditorTab(Path path, String content, Charset charset) {
    this.path    = Objects.requireNonNull(path, "path");
    this.charset = Objects.requireNonNull(charset, "charset");
    // ... rest unchanged
}
```

### EditorController read call site
```java
Charset cs = XmlCharsetDetector.detect(key);
String content = Files.readString(key, cs);
EditorTab editorTab = new EditorTab(key, content, cs);
```

### EditorController save call sites
```java
// saveTab:
Files.writeString(editorTab.path, editorTab.codeArea.getText(), editorTab.charset);

// saveAll:
Files.writeString(et.path, et.codeArea.getText(), et.charset);
```

</specifics>

<deferred>
## Deferred Ideas

- SearchDialog encoding fix — uses `Files.readAllLines(file, UTF_8)`. Garbles non-ASCII in search results but not required by ENC-01..03. Defer to Phase 17 extension or Phase 19.
- Encoding indicator in tab header — show charset name next to filename. Out of scope.
- Encoding conversion on save — let user switch a file's charset. Out of scope.

</deferred>

---

*Phase: 17-encoding-investigation-fix*
*Context gathered: 2026-04-23*

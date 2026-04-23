# Phase 14: Version & Icon Housekeeping - Pattern Map

**Mapped:** 2026-04-23
**Files analyzed:** 4 (1 file move + 3 modifications)
**Analogs found:** 4 / 4

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `build.gradle` | config | transform | `build.gradle` lines 66-70 (processResources block) | self (exact) |
| `src/main/java/ch/ti/gagi/xsleditor/XSLEditorApp.java` | config/bootstrap | request-response | `AboutDialog.java` lines 130-138 (classpath resource load pattern) | role-match |
| `src/main/java/ch/ti/gagi/xsleditor/ui/AboutDialog.java` | component | request-response | `SearchDialog.java` lines 88-112 (Dialog<Void> constructor pattern) | exact |
| `src/main/resources/ch/ti/gagi/xsleditor/icon.png` | asset | file-I/O | `src/main/resources/version.properties` (classpath resource convention) | role-match |

---

## Pattern Assignments

### `build.gradle` (config, transform) — version bump only

**Analog:** `build.gradle` itself (self-referential, lines 12-13 and 66-70)

**Target field** (line 13):
```groovy
version = '0.1.0'
```
Change to:
```groovy
version = '0.3.0'
```

**processResources context** (lines 66-70) — no change needed, already injects `${version}`:
```groovy
processResources {
    filesMatching('version.properties') {
        expand(version: project.version)
    }
}
```

No other build.gradle lines change. The `processResources` task propagates the bump automatically to `version.properties` at build time, which `AboutDialog.loadVersion()` reads at runtime.

---

### `XSLEditorApp.java` (bootstrap, request-response) — icon wiring

**Analog for classpath load pattern:** `AboutDialog.java` lines 130-138

**Existing classpath resource load pattern** (`AboutDialog.java` lines 130-138):
```java
private String loadVersion() {
    try (InputStream in = getClass().getResourceAsStream("/version.properties")) {
        if (in == null) return "unknown";
        Properties p = new Properties();
        p.load(in);
        return p.getProperty("version", "unknown");
    } catch (IOException e) {
        return "unknown";
    }
}
```

**Insertion point in `XSLEditorApp.start()`** (lines 44-49) — insert icon block before `primaryStage.show()` at line 49:
```java
primaryStage.setTitle(APP_NAME);
primaryStage.setMinWidth(900);
primaryStage.setMinHeight(600);
primaryStage.setScene(scene);
hostServicesInstance = getHostServices();
// INSERT ICON BLOCK HERE (before primaryStage.show())
primaryStage.show();
```

**Icon wiring pattern to insert** — mirrors `AboutDialog.loadVersion()` with fail-silent fallback:
```java
// Load and set the application window icon
try (java.io.InputStream iconStream =
        getClass().getResourceAsStream("/ch/ti/gagi/xsleditor/icon.png")) {
    if (iconStream != null) {
        javafx.scene.image.Image icon = new javafx.scene.image.Image(iconStream);
        if (!icon.isError()) {
            primaryStage.getIcons().add(icon);
        } else {
            java.util.logging.Logger.getLogger(XSLEditorApp.class.getName())
                .warning("App icon image reported an error during loading");
        }
    } else {
        java.util.logging.Logger.getLogger(XSLEditorApp.class.getName())
            .warning("App icon not found on classpath: /ch/ti/gagi/xsleditor/icon.png");
    }
} catch (Exception e) {
    // Non-fatal: app launches without icon
}
```

**New imports required** (append to existing import block at lines 1-10):
```java
import javafx.scene.image.Image;
import java.io.InputStream;
import java.util.logging.Logger;
```

---

### `AboutDialog.java` (component, request-response) — add ImageView as first VBox element

**Analog:** `SearchDialog.java` lines 88-112 (Dialog<Void> constructor, content assembly), and `AboutDialog.loadVersion()` lines 130-138 (classpath resource load with null-check and fail-silent)

**SearchDialog Dialog<Void> constructor pattern** (lines 88-112):
```java
public SearchDialog(Stage ownerStage, ...) {
    if (ownerStage != null) initOwner(ownerStage);
    setTitle("Find in Files");
    setHeaderText(null);
    setResizable(true);
    // ... build layout nodes ...
    getDialogPane().setContent(content);
    getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
}
```

**Existing VBox assembly** in `AboutDialog.java` (lines 114-117) — this is the insertion point:
```java
// 10. Assemble root VBox
VBox content = new VBox(8,
    titleLabel, sep1, stackHeader, grid,
    sep2, authorLabel, licenseBox);
```

**ImageView construction to prepend** — follows the same fail-silent pattern as `loadVersion()`:
```java
// 10a. Optional app icon (fail-silent if resource missing)
javafx.scene.image.ImageView iconView = null;
try (InputStream iconStream =
        getClass().getResourceAsStream("/ch/ti/gagi/xsleditor/icon.png")) {
    if (iconStream != null) {
        javafx.scene.image.Image img = new javafx.scene.image.Image(iconStream);
        if (!img.isError()) {
            iconView = new javafx.scene.image.ImageView(img);
            iconView.setFitWidth(64);
            iconView.setFitHeight(64);
            iconView.setPreserveRatio(true);
        }
    }
} catch (Exception ignored) {
    // Dialog must not crash if icon is absent
}

// 10b. Assemble root VBox — prepend icon when available
VBox content;
if (iconView != null) {
    content = new VBox(8,
        iconView, titleLabel, sep1, stackHeader, grid,
        sep2, authorLabel, licenseBox);
    content.setAlignment(javafx.geometry.Pos.CENTER);
} else {
    content = new VBox(8,
        titleLabel, sep1, stackHeader, grid,
        sep2, authorLabel, licenseBox);
}
```

**New imports required** (append to existing import block at lines 1-19):
```java
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.InputStream;
```

Note: `javafx.geometry.Pos` is already imported at line 7.

---

### `src/main/resources/ch/ti/gagi/xsleditor/icon.png` (asset, file-I/O) — move only

**Analog:** `src/main/resources/version.properties` (same resource directory convention)

**Operation:** Move (git mv) from project root to resources tree:
```bash
git mv icon.png src/main/resources/ch/ti/gagi/xsleditor/icon.png
```

**Classpath access path after move:** `/ch/ti/gagi/xsleditor/icon.png`

No code change required for the move itself — the classpath path is what both `XSLEditorApp.java` and `AboutDialog.java` reference.

---

## Shared Patterns

### Classpath Resource Load (fail-silent)
**Source:** `AboutDialog.java` lines 130-138 (`loadVersion()` method)
**Apply to:** Icon loading in both `XSLEditorApp.start()` and `AboutDialog` constructor

Core structure — always null-check the stream, wrap in try-with-resources, return/skip silently on failure:
```java
try (InputStream in = getClass().getResourceAsStream("/absolute/classpath/path")) {
    if (in == null) { /* handle missing */ return fallback; }
    // use in
} catch (IOException e) {
    return fallback;
}
```

### Dialog<Void> Programmatic Construction
**Source:** `SearchDialog.java` lines 88-112 and `AboutDialog.java` lines 36-122
**Apply to:** `AboutDialog.java` modifications (same constructor pattern, no structural change)

Pattern: constructor sets owner/modality, builds all layout nodes programmatically, calls `getDialogPane().setContent()` and `getDialogPane().getButtonTypes().add(ButtonType.CLOSE)` at the end.

### Warning Logging
**Source:** No existing `java.util.logging` usage found in the codebase — this is new to `XSLEditorApp`.
**Apply to:** `XSLEditorApp.start()` icon-missing warning only
**Convention (from Claude's Discretion in CONTEXT.md):** Use `java.util.logging.Logger` (JUL), which is available without additional dependencies. Pattern:
```java
Logger.getLogger(XSLEditorApp.class.getName()).warning("message");
```

---

## No Analog Found

None — all four changes have direct analogs or are self-evident single-field edits.

---

## Metadata

**Analog search scope:** `src/main/java/ch/ti/gagi/xsleditor/` (all .java files), `build.gradle`, `src/main/resources/`
**Files scanned:** 4 primary files read in full
**Pattern extraction date:** 2026-04-23

# Phase 14: Version & Icon Housekeeping - Context

**Gathered:** 2026-04-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Bump the build version to 0.3.0, move the existing app icon into the resources tree, wire it
in the main window stage and the About dialog. All changes are additive: no existing features
altered, no new UI panels added.

</domain>

<decisions>
## Implementation Decisions

### Version bump
- **D-01**: Change `version = '0.1.0'` to `version = '0.3.0'` in `build.gradle`. The
  `processResources` task already injects `${version}` into `src/main/resources/version.properties`
  and `AboutDialog.loadVersion()` already reads that file at runtime — no other code change is
  needed for the version string to update automatically.

### Icon asset
- **D-02**: Source file: `icon.png` in the project root (1024×1024, RGBA, transparent background).
  Move it to `src/main/resources/ch/ti/gagi/xsleditor/icon.png`. Classpath access path:
  `/ch/ti/gagi/xsleditor/icon.png`.
- **D-03**: Wiring in `XSLEditorApp.start()`: load `javafx.scene.image.Image` via
  `getClass().getResourceAsStream("/ch/ti/gagi/xsleditor/icon.png")`, check `image.isError()`,
  add to `primaryStage.getIcons()` if valid, log a warning if not. Must happen before
  `primaryStage.show()` (currently line 49).

### About dialog icon
- **D-04**: Add an `ImageView` (fitWidth = 64, fitHeight = 64, preserveRatio = true) loaded from
  the same classpath path as D-02. Insert it as the **first element** in the `VBox content`
  in `AboutDialog.java` (currently assembled at line 115), centered in the VBox.
  If the image fails to load, omit the ImageView silently — dialog must not crash.

### Claude's Discretion
- Exact logging mechanism for the missing-icon warning in `XSLEditorApp` (Java Logger,
  `System.err`, or no-op fallback acceptable)
- Whether to center the icon via `VBox.setAlignment(Pos.CENTER)` or a wrapping `HBox`

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Build & version
- `build.gradle` — version field to bump (line 13); `processResources` task already present (line 66–70)
- `src/main/resources/version.properties` — auto-updated by processResources; no manual edit needed

### Source files to modify
- `src/main/java/ch/ti/gagi/xsleditor/XSLEditorApp.java` — icon wiring point; insert before `primaryStage.show()` (line 49)
- `src/main/java/ch/ti/gagi/xsleditor/ui/AboutDialog.java` — add `ImageView` as first element of the `VBox content` (line 115)

### Requirements
- `.planning/REQUIREMENTS.md` §VER-01, VER-02, ICON-01, ICON-02 — acceptance criteria for this phase

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AboutDialog.loadVersion()` (line 130): reads classpath properties via `getClass().getResourceAsStream("/version.properties")` — same pattern applies for loading the icon image
- `XSLEditorApp.APP_NAME` — already defined; no change needed

### Established Patterns
- `getClass().getResourceAsStream("/absolute/classpath/path")` — used in `AboutDialog` for version; use the same for icon
- `Dialog<Void>` programmatic construction — all layout built in constructor; icon `ImageView` goes in the same `VBox`

### Integration Points
- `XSLEditorApp.start()`: `primaryStage.getIcons().add(image)` — before line 49 (`primaryStage.show()`)
- `AboutDialog` constructor: prepend `ImageView iconView` to the `VBox content` node list at line 115

### Icon asset
- Source: `icon.png` at project root (1024×1024, RGBA, transparent background)
- Destination: `src/main/resources/ch/ti/gagi/xsleditor/icon.png`
- The file already exists; this is a move, not a creation

</code_context>

<specifics>
## Specific Ideas

- Icon confirmed as 1024×1024 RGBA PNG with transparent background — JavaFX scales it cleanly to
  any display size, including HiDPI Retina displays on macOS.
- About dialog layout after change:
  ```
  ┌─────────────────────────┐
  │                         │
  │      [ ICONA 64×64 ]    │
  │                         │
  │   XSLEditor  v0.3.0     │
  │  ─────────────────────  │
  │  Runtime Stack          │
  │   Java   21.x.x         │
  │   Saxon-HE  12.x        │
  │   FOP       2.9         │
  │   JavaFX    21          │
  │  ─────────────────────  │
  │  Author: ...            │
  │  License: Apache 2.0    │
  └─────────────────────────┘
  ```

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 14-version-icon-housekeeping*
*Context gathered: 2026-04-23*

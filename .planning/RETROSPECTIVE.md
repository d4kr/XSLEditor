# Retrospective: XSLEditor

## Milestone: v1.0 — MVP

**Shipped:** 2026-04-21
**Phases:** 9 | **Plans:** 24

### What Was Built

1. JavaFX 3-zone shell with fat JAR build and window lifecycle
2. Project management — open/persist/restore via `.xslfo-tool.json`
3. File tree with FileItem model, visual roles, context menu
4. Multi-tab RichTextFX editor with dirty tracking, Ctrl+S, close confirmation
5. XML syntax highlight, autocomplete, occurrence highlight, go-to-def, find-in-files
6. Full Saxon+FOP render pipeline via JavaFX Task with async execution and error routing
7. PDF preview via PDFBox 150 DPI PNG rendering (macOS WebKit workaround)
8. Error log panel — TableView, severity filter, click-to-navigate
9. 96-test suite — unit tests all backend modules + full pipeline integration tests

### What Worked

- **Wave 0/1/2 pattern** — skeleton production classes in Wave 0 let test stubs compile and stay green; clean progression to implementation
- **Consumer callback seam** — decoupling sub-controllers via Consumer<T> callbacks kept dependency graph clean; no upward imports
- **Fine granularity phasing** — 9 focused phases made each increment reviewable and testable in isolation
- **Static method extraction** — SearchDialog.search(), RenderOrchestrator.renderSafe() as testable pure functions
- **Pre-phase research** — PDFBox workaround for macOS WebKit discovered in research before Phase 7 began, not during execution

### What Was Inefficient

- **REQUIREMENTS.md traceability table** — never updated during execution; all rows stayed "Pending". Document was stale from day 1. In v1.1: update at each phase close or drop the table entirely.
- **VERIFICATION.md human_needed status** — three phases (02, 04, 06) left in `human_needed` status throughout the milestone. Should be resolved at phase close, not deferred to milestone close.
- **Missing VERIFICATION.md** — Phases 01, 05, 07 had no verification file. Verification step should be enforced per phase.
- **Nyquist compliance gap** — not systematically applied. VALIDATION.md files partial or missing for most phases.

### Patterns Established

- **WellBehavedFX Nodes.addInputMap per-CodeArea** — scene-level Ctrl+S causes focus bugs; per-node InputMap is the correct JavaFX pattern
- **LogController before RenderController** in MainController.initialize() — callback ordering constraint, must be documented and preserved
- **PDFBox 150 DPI page rendering** — macOS-specific workaround; loadContent() not load(file://) to avoid WebKit same-URI cache
- **disableProperty().unbind() before setDisable()** in Task lifecycle — required to avoid binding conflict
- **EditorTab as data carrier** — public final fields, no setters, sub-controller owns lifecycle

### Key Lessons

1. Keep REQUIREMENTS.md traceability live — update at every phase close, or it becomes noise
2. Close `human_needed` VERIFICATION.md at phase end, not at milestone close
3. Pre-research macOS/platform quirks before UI phases — WebView PDF behavior was a known issue by research
4. Wave 0/1/2 pattern is worth the overhead — produces cleaner, testable code
5. Consumer callback seams scale well — add them eagerly at integration points

### Cost Observations

- Sessions: ~15 sessions across 14 days
- Notable: backend pipeline pre-existing (T1–T10) gave strong foundation; UI phases moved fast with clean seam pattern

---

## Milestone: v0.2.0 — Developer UX Improvements

**Shipped:** 2026-04-22
**Phases:** 3 (10–12) | **Plans:** 5

### What Was Built

1. Saxon URI-decode fix — `resolveFilePath()` wraps `URI.create()` in try/catch; `PreviewManagerTest` 3 unit tests covering percent-encoded paths, plain URIs, malformed fallback
2. About dialog — programmatic `Dialog<Void>`, dark theme, version from `version.properties` (Gradle processResources), Java/Saxon/FOP/JavaFX runtime versions, author, license hyperlink
3. ChatGPT error-log column — `colAi` `TableColumn<LogEntry, Void>` with 💬 button per row; Italian preamble + `URLEncoder` + `addEventFilter(MOUSE_PRESSED)` to block row-click propagation

### What Worked

- **Code review caught real bugs** — WR-01 (`evt.consume()` on `ActionEvent` doesn't block `TableView` `MouseEvent`) and WR-02 (null message guard) found and fixed before tag
- **Small focused milestone** — 3 phases, 2 days; each phase had a single clear deliverable. Zero inter-phase conflicts
- **Existing patterns reused well** — `HostServices.showDocument()` from Phase 11 reused immediately in Phase 12; `static` accessor pattern extended cleanly
- **Threat model inline in PLAN** — T-12-01 URL encoding threat documented in plan, implemented correctly first time

### What Was Inefficient

- **Phase 11 ROADMAP checkbox never updated** — Phase 11 completed but checkbox stayed `[ ]` throughout; only caught at milestone close. Tracking drift still a problem
- **REQUIREMENTS.md traceability** — all 7 requirements stayed "Pending" throughout, same problem as v0.1.0. Closed only at milestone archive
- **No automated tests for Phase 11** — About dialog fully manual-tested; no regression coverage if dialog layout changes

### Patterns Established

- **`TableCell<T, Void>` for action columns** — no cell value, only graphic; `private final Button` created once per cell in outer scope, set as graphic in `updateItem`
- **`addEventFilter(MOUSE_PRESSED, e -> e.consume())` on button** — correct way to prevent `TableView` row-click handler from firing when a cell button is clicked
- **Gradle `processResources expand()`** — build-time property injection into classpath resources; cleaner than runtime classpath scanning
- **`static` HostServices accessor** — `private static HostServices hostServices` set in `start()`, exposed via `public static hostServices()` — avoids passing through constructors

### Key Lessons

1. Update ROADMAP phase checkboxes at phase close — drift compounds and requires manual cleanup at milestone end
2. `evt.consume()` on `ActionEvent` has zero effect on `TableView`'s `MouseEvent` listener — always use `addEventFilter(MOUSE_PRESSED)` on the button itself
3. Programmatic dialogs (`Dialog<Void>`, no FXML) are faster than FXML for one-off UI; no controller wiring needed
4. Code review is worth the token cost — caught a functional bug (WR-01) that would have shipped silently

### Cost Observations

- Sessions: ~3 sessions across 2 days
- Notable: short milestone with well-scoped requirements; execution was fast; most time in Phase 11 (3 plans vs 1 each for 10/12)

---

## Cross-Milestone Trends

| Trend | v0.1.0 | v0.2.0 |
|-------|--------|--------|
| Wave pattern used | Yes | Yes |
| Consumer callback seams | Yes | Yes (reused) |
| Traceability maintained | No (stale) | No (stale) |
| ROADMAP checkboxes live | Partial | No (drift) |
| Code review run | No | Yes — caught WR-01 |
| Nyquist compliance | Partial | Not run |
| Tech debt carried forward | 4 items | 3 items (EDIT-06, EDIT-07, no Phase 11 tests) |

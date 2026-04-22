# Phase 11: About Dialog - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-22
**Phase:** 11-about-dialog
**Areas discussed:** Version source, Dialog layout, License display, Build version bump

---

## Version source

| Option | Description | Selected |
|--------|-------------|----------|
| version.properties | Gradle injects version from build.gradle into resources at build time; dialog reads at runtime | ✓ |
| Costante in AppInfo.java | Static constant AppInfo.VERSION — simple but requires manual update per release | |
| JAR Manifest | Implementation-Version in MANIFEST.MF — doesn't work from IDE, more complex | |

**User's choice:** version.properties (Gradle-injected)
**Notes:** User also clarified that the versioning schema has changed to 0.x — the current version is 0.1.0 (not 1.0.0 as in build.gradle), and the next milestone will be 0.2.0.

---

## Dialog layout

| Option | Description | Selected |
|--------|-------------|----------|
| Singolo pannello | VBox with sections: title+version, runtime stack GridPane, credits, license. Programmatic, no FXML. | ✓ |
| Tab (About / Runtime / License) | Tabbed layout — more structure but overkill for this content volume | |

**User's choice:** Single panel (programmatic, SearchDialog pattern)
**Notes:** User confirmed the mockup: title + version, separator, Runtime Stack GridPane (Java/Saxon-HE/FOP/JavaFX), separator, author, license line.

---

## License display

| Option | Description | Selected |
|--------|-------------|----------|
| Testo inline breve | Short text line + Hyperlink opening license URL in browser | ✓ |
| Testo completo inline | Full license text in scrollable TextArea inside dialog | |
| Solo link | Just a Hyperlink, no inline text | |

**User's choice:** Short inline text + Hyperlink to Apache 2.0 URL

---

## Build version bump

| Option | Description | Selected |
|--------|-------------|----------|
| Bumpa a 0.1.0 ora | Fix build.gradle from '1.0.0' to '0.1.0' in this phase | ✓ |
| Bumpa a 0.2.0 direttamente | Skip 0.1.0, go straight to 0.2.0 | |

**User's choice:** Correct to 0.1.0 now — phase 11 is still part of the v1.1 milestone, the dialog shows 0.1.0 while 0.2.0 will be the completed v1.1 milestone version.

---

## Claude's Discretion

- Exact CSS styling of the dialog
- How to retrieve Saxon-HE version at runtime (Saxon API or manifest)
- How to retrieve FOP version at runtime
- Whether to add a small app icon in the dialog header

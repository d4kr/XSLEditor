# Phase 1: JavaFX Application Shell - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions captured in CONTEXT.md — this log preserves the discussion.

**Date:** 2026-04-14
**Phase:** 01-javafx-application-shell
**Mode:** discuss
**Areas analyzed:** Build & Gradle setup, Layout e struttura UI, Moduli Java (module-info), Stato vuoto all'avvio

## Decisions Recorded

### Build & Gradle Setup
| Question | Answer | Notes |
|----------|--------|-------|
| Gradle JavaFX approach | openjfx plugin | Automatic module path handling |
| Packaging | Fat JAR | Simpler for internal tool — java -jar |

### Java Module System
| Question | Answer | Notes |
|----------|--------|-------|
| module-info.java | No (classpath) | Avoids verbose requires declarations for Saxon/FOP/Jackson |

### Layout e struttura UI
| Question | Answer | Notes |
|----------|--------|-------|
| Layout definition | FXML + controller | Standard JavaFX pattern, maintainable |
| Main layout | 3 zones | File tree | Editor | PDF preview; log panel bottom collapsible |

### Stato vuoto all'avvio
| Question | Answer | Notes |
|----------|--------|-------|
| Empty state | Pannelli vuoti | No welcome screen, no auto-open dialog |

## Corrections Made

No corrections — all recommended options confirmed.

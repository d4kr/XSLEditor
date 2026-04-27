# Requirements: XSLEditor v0.4.1 — Keyboard Shortcuts & Edit Menu

**Defined:** 2026-04-27
**Core Value:** A developer can open a project, edit XSLT templates, trigger a render, and see the PDF — all in one window without context switching.

## v0.4.1 Requirements

### Keyboard Shortcuts

- [ ] **KBD-01**: User can open a project via keyboard with `Shortcut+O` (File > Open Project...)
- [ ] **KBD-02**: User can create a new file via keyboard with `Shortcut+N` (File > New File...)
- [ ] **KBD-03**: User can exit the application via keyboard with `Shortcut+Q` (File > Exit)
- [ ] **KBD-04**: User can set XSLT entrypoint via keyboard with `Shortcut+Shift+E` (File > Set Entrypoint)
- [ ] **KBD-05**: User can set XML input via keyboard with `Shortcut+Shift+I` (File > Set XML Input)

### Edit Menu

- [ ] **EDIT-10**: User can cut selected text in active editor via Edit > Cut (`Shortcut+X`)
- [ ] **EDIT-11**: User can copy selected text in active editor via Edit > Copy (`Shortcut+C`)
- [ ] **EDIT-12**: User can paste clipboard text into active editor via Edit > Paste (`Shortcut+V`)
- [ ] **EDIT-13**: User can select all text in active editor via Edit > Select All (`Shortcut+A`)

## Future Requirements

### Edit Menu Extensions

- **EDIT-14**: User can undo last edit via Edit > Undo (`Shortcut+Z`) — deferred until UndoManager actions are fully wired beyond dirty-state tracking
- **EDIT-15**: User can redo last undone edit via Edit > Redo (`Shortcut+Shift+Z`) — deferred with Undo

## Out of Scope

| Feature | Reason |
|---------|--------|
| Delete Line / Duplicate Line | Not requested for this milestone |
| Undo / Redo in Edit menu | UndoManager used for dirty tracking only; expose in a dedicated milestone |
| View menu items | View menu empty; no features defined |
| Help menu shortcut | About has no standard keyboard shortcut |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| KBD-01 | Phase 24 | Pending |
| KBD-02 | Phase 24 | Pending |
| KBD-03 | Phase 24 | Pending |
| KBD-04 | Phase 24 | Pending |
| KBD-05 | Phase 24 | Pending |
| EDIT-10 | Phase 25 | Pending |
| EDIT-11 | Phase 25 | Pending |
| EDIT-12 | Phase 25 | Pending |
| EDIT-13 | Phase 25 | Pending |

**Coverage:**
- v0.4.1 requirements: 9 total
- Mapped to phases: 9 (100%) ✓
- Unmapped: 0

---
*Requirements defined: 2026-04-27*
*Last updated: 2026-04-27 — traceability mapped to Phases 24–25*

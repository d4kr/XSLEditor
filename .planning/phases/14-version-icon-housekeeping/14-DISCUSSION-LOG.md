# Phase 14: Version & Icon Housekeeping - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-23
**Phase:** 14-version-icon-housekeeping
**Areas discussed:** Icon asset, About dialog layout

---

## Icon asset

| Option | Description | Selected |
|--------|-------------|----------|
| Placeholder generato | PNG minimale programmatico (sfondo colorato + "XSL", 512×512) | |
| Fornirò un file PNG | Asset fornito dall'utente | ✓ |
| Placeholder da Java AWT | Icona generata a build-time tramite Gradle task | |

**User's choice:** "Placeholder generato" selezionato, poi nota aggiuntiva: "il file icon.png con sfondo trasparente si trova nella root del progetto."
**Notes:** L'utente ha già un `icon.png` (1024×1024, RGBA, trasparente) nella root. Nessuna generazione necessaria — basta spostarlo in `src/main/resources/ch/ti/gagi/xsleditor/`.

---

## About dialog layout

| Option | Description | Selected |
|--------|-------------|----------|
| Sopra il titolo, centrata | ImageView 64×64 centrata in cima al VBox | ✓ |
| A sinistra del titolo, inline | HBox(ImageView 32×32, titleLabel) | |

**User's choice:** Icona sopra il titolo, centrata (64×64).
**Notes:** Stile classico macOS "About" dialog. Il VBox corrente in AboutDialog riceve un ImageView come primo elemento.

---

## Claude's Discretion

- Meccanismo di logging per missing-icon warning in `XSLEditorApp`
- Centering dell'icona nel VBox (alignment vs. HBox wrapper)

## Deferred Ideas

Nessuna idea deviante emersa — discussione rimasta nello scope della fase.

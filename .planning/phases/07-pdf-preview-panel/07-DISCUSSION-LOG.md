# Phase 7: PDF Preview Panel - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-19
**Phase:** 07-pdf-preview-panel
**Areas discussed:** PDF rendering approach, Outdated indicator style, PreviewController structure, Placeholder/failure state

---

## PDF rendering approach

| Option | Description | Selected |
|--------|-------------|----------|
| Temp file + file:// URI | Write byte[] to temp file, load via webView.getEngine().load(uri). Zero JS. | ✓ |
| PDF.js embedded | Bundle PDF.js (~1MB), load via WebView HTML wrapper. Cross-platform reliable. | |
| PDFBox image rendering | Render pages as images via PDFBox. Full control, high complexity. | |

**User's choice:** Temp file + file:// URI
**Notes:** Tool interno — sufficiente. PDFViewerFX già escluso (non su Maven Central).

### Temp file location

| Option | Description | Selected |
|--------|-------------|----------|
| System temp dir, file fisso per sessione | createTempFile una volta, sovrascrittura ad ogni render | ✓ |
| System temp dir, nuovo file ad ogni render | Cleanup esplicito del precedente | |
| Project dir (.xslfo-preview.pdf) | File visibile nel progetto | |

**User's choice:** System temp dir, file fisso per sessione

---

## Outdated indicator style

| Option | Description | Selected |
|--------|-------------|----------|
| Banner colorato in cima al previewPane | Label/HBox top-aligned, sfondo arancione | ✓ |
| Overlay semi-trasparente centrato | Label centrata, semi-opaco scuro | |
| Bordo colorato sul previewPane | CSS border arancione | |

**User's choice:** Banner colorato in cima
**Notes:** Warning color, non intrusivo, non oscura il contenuto PDF.

### Banner color/text

| Option | Description | Selected |
|--------|-------------|----------|
| Arancione, "Preview outdated — last render failed" | Warning, coerente con WARNING nel log panel | ✓ |
| Giallo/amber, "Outdated" | Più soft | |
| Rosso, "Render failed — preview outdated" | Troppo aggressivo | |

**User's choice:** Arancione, "Preview outdated — last render failed"

---

## PreviewController structure

| Option | Description | Selected |
|--------|-------------|----------|
| PreviewController dedicato | Segue pattern stabilito (FileTreeController, EditorController, RenderController) | ✓ |
| Inline in MainController | Più semplice, rompe il pattern | |

**User's choice:** PreviewController dedicato

---

## Placeholder / failure state

| Option | Description | Selected |
|--------|-------------|----------|
| Mantieni PDF precedente + banner outdated | PDF vecchio visibile + overlay arancione | ✓ |
| Torna al placeholder | Nascondi WebView dopo fallimento | |

**User's choice:** Mantieni PDF precedente + banner outdated
**Notes:** Più utile per debug — lo sviluppatore vede ancora il PDF "vecchio" e sa che è outdated.

### Initial placeholder text

**User's choice:** Testo esistente "No preview — trigger a render first" va bene.

---

## Claude's Discretion

- Exact FXML StackPane.alignment for outdated banner
- Temp file field placement in PreviewController
- IOException handling in displayPdf()

## Deferred Ideas

- Log entries copyable (user richiesta): logListView items non selezionabili/copiabili — Phase 8 responsibility
- Explicit zoom in/out buttons: WebView native PDF viewer fornisce zoom browser-level; deferred

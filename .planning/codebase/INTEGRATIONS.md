# External Integrations

**Analysis Date:** 2026-04-14

## APIs & External Services

**None.**

XLSEditor is designed as a local desktop tool with no external API dependencies or network calls.

## Data Storage

**Databases:**
- None - Application uses local filesystem only

**File Storage:**
- Local filesystem - All project files, XML inputs, XSLT templates, and generated PDFs are stored locally
  - Projects loaded from user-selected directories
  - Configuration stored in JSON files in project root
  - Input XML files managed via ProjectFileManager (`src/main/java/ch/ti/gagi/xlseditor/model/ProjectFileManager.java`)
  - Generated PDFs output to local filesystem via RenderEngine

**Caching:**
- In-memory log management via LogManager (`src/main/java/ch/ti/gagi/xlseditor/log/LogManager.java`)
- XSLT compilation cache within RenderEngine process lifetime
- No persistent caching layer

## Authentication & Identity

**Auth Provider:**
- None - Local desktop tool, no multi-user or authentication requirements

## Monitoring & Observability

**Error Tracking:**
- None - Errors handled locally via ErrorManager (`src/main/java/ch/ti/gagi/xlseditor/error/ErrorManager.java`)
  - Exception parsing and file position extraction
  - PreviewError DTO for user-facing error messages

**Logs:**
- In-memory logging via LogManager with three levels: INFO, WARN, ERROR
  - Stored as LogEntry objects (`src/main/java/ch/ti/gagi/xlseditor/log/LogEntry.java`)
  - Accessible via getByLevel() filtering method
  - No persistent log files or external log aggregation

## CI/CD & Deployment

**Hosting:**
- Local desktop application (no hosted deployment)

**CI Pipeline:**
- Not detected

## Environment Configuration

**Required env vars:**
- None - Application is self-contained and local

**Secrets location:**
- Not applicable - No external authentication or API credentials required

## Webhooks & Callbacks

**Incoming:**
- None

**Outgoing:**
- None

## XML Processing

**Standards & Specifications:**
- XML 1.0 via DocumentBuilderFactory (`src/main/java/ch/ti/gagi/xlseditor/validation/ValidationEngine.java`)
  - Namespace-aware parsing enabled
  - SAX parse exception handling with line/column tracking
- XSLT 2.0 via Saxon-HE (`src/main/java/ch/ti/gagi/xlseditor/render/RenderEngine.java`)
  - Custom preprocessing support for library directives (`src/main/java/ch/ti/gagi/xlseditor/library/LibraryPreprocessor.java`)
- XSL-FO to PDF via Apache FOP (`src/main/java/ch/ti/gagi/xlseditor/render/RenderEngine.java`)

## Project Configuration Format

**Config File:**
- JSON format parsed via Jackson ObjectMapper
- Location: `xlseditor-config.json` in project root (assumed)
- Schema:
  ```json
  {
    "entryPoint": "path/to/main.xslt",
    "xmlInput": "path/to/input.xml"
  }
  ```
- Both paths must be relative (validated in ProjectConfig record)

---

*Integration audit: 2026-04-14*

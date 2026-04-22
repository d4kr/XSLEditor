# Architecture

**Analysis Date:** 2026-04-14

## Pattern Overview

**Overall:** Layered pipeline architecture with clear separation of concerns

**Key Characteristics:**
- Single responsibility per module (validation, rendering, dependency resolution, etc.)
- Error collection and aggregation across pipeline stages
- Safe execution with comprehensive error handling (`renderSafe`)
- Stateless processing functions (facilitates testing and predictability)
- Type-safe domain objects (Project, Preview, RenderResult) instead of weakly-typed return values

## Layers

**Model / Domain Layer:**
- Purpose: Represents the core business entities of the system
- Location: `src/main/java/ch/ti/gagi/xsleditor/model/`
- Contains: Project configuration, file metadata, state tracking
- Depends on: Nothing (leaf layer)
- Used by: All other layers

**Preprocessing Layer:**
- Purpose: Transforms and prepares files before core pipeline execution
- Location: `src/main/java/ch/ti/gagi/xsleditor/library/`
- Contains: Custom directive expansion (<?LIBRARY ...?>)
- Depends on: Filesystem I/O
- Used by: RenderOrchestrator

**Dependency Resolution Layer:**
- Purpose: Analyzes XSLT include/import declarations and builds dependency graph
- Location: `src/main/java/ch/ti/gagi/xsleditor/dependency/`
- Contains: Graph traversal with circular dependency detection
- Depends on: XML parsing, filesystem
- Used by: RenderOrchestrator, ValidationEngine

**Validation Layer:**
- Purpose: Ensures all project files are well-formed XML/XSLT before processing
- Location: `src/main/java/ch/ti/gagi/xsleditor/validation/`
- Contains: XML schema validation, error collection
- Depends on: Filesystem, dependency graph
- Used by: RenderOrchestrator

**Rendering Layer:**
- Purpose: Executes the core pipeline (XSLT → XSL-FO → PDF)
- Location: `src/main/java/ch/ti/gagi/xsleditor/render/`
- Contains: Saxon XSLT engine, Apache FOP renderer, orchestration
- Depends on: saxon-he, fop libraries
- Used by: PreviewManager

**Error Management Layer:**
- Purpose: Translates exceptions from different pipeline stages into unified RenderError format
- Location: `src/main/java/ch/ti/gagi/xsleditor/error/`
- Contains: Exception classification (XSLT/FOP/IO/UNKNOWN), location extraction
- Depends on: ValidationError, RenderError
- Used by: RenderOrchestrator, PreviewManager

**Preview / Output Layer:**
- Purpose: Marshals render results (PDF bytes or error list) for consumption by UI
- Location: `src/main/java/ch/ti/gagi/xsleditor/preview/`
- Contains: Preview DTO, error adaptation, orchestration facade
- Depends on: RenderOrchestrator, RenderError
- Used by: UI components (not shown in current codebase)

**Logging Layer:**
- Purpose: Collects structured log entries for debugging and UI display
- Location: `src/main/java/ch/ti/gagi/xsleditor/log/`
- Contains: In-memory log storage, level-based filtering
- Depends on: Nothing
- Used by: Future integration with RenderOrchestrator and UI

## Data Flow

**Main Render Flow (Happy Path):**

1. **User triggers render** → UI calls PreviewManager.generatePreview(project, rootPath)

2. **Build dependency graph** → DependencyResolver.buildGraph() parses xsl:include/xsl:import statements across all reachable XSLT files

3. **Validate all files** → ValidationEngine.validateProject() ensures all project files + XML input are well-formed

4. **Load entry XSLT** → Read entrypoint file from disk as String

5. **Apply library preprocessing** → LibraryPreprocessor.mergeLibraries() scans for <?LIBRARY directives, loads referenced .xsl files, inlines content

6. **Compile XSLT** → RenderEngine.compileXslt(String) uses Saxon to compile preprocessed XSLT string to XsltExecutable

7. **Transform XML to FO** → RenderEngine.transformToString() applies compiled XSLT to XML input, produces XSL-FO string

8. **Render FO to PDF** → RenderEngine.renderFoToPdf() feeds FO through Apache FOP, produces byte[] PDF

9. **Wrap result** → RenderResult.success(byte[]) wraps PDF bytes

10. **Map to preview DTO** → PreviewManager.toPreview() converts RenderResult to Preview (final output)

**Error Flow:**

At any stage where an exception occurs:
- Stage catches exception (or RenderOrchestrator catches from stage)
- ErrorManager.fromException(e) classifies it (XSLT/FOP/IO/UNKNOWN) and extracts location info (file:line)
- Error wrapped in RenderError(message, type, location)
- List<RenderError> collected into RenderResult.failure()
- PreviewManager.toPreviewErrors() adapts RenderError → PreviewError (parses location string into file/line fields)
- Final Preview object carries error list to UI

**State Management:**

- No global state. Each operation is independent.
- Project and ProjectFile track dirty/clean state for unsaved changes
- Preview and RenderResult are immutable value objects
- LogManager maintains append-only in-memory log (cleared between sessions or managed by caller)

## Key Abstractions

**Project:**
- Purpose: Represents a project context (rootPath, entryPoint XSLT, xmlInput)
- Examples: `src/main/java/ch/ti/gagi/xsleditor/model/Project.java`
- Pattern: Immutable value object with accessor methods
- Use: Pass to render pipeline to identify what to render and where

**DependencyGraph:**
- Purpose: Represents XSLT file dependencies as a directed acyclic graph
- Examples: `src/main/java/ch/ti/gagi/xsleditor/dependency/DependencyGraph.java`
- Pattern: Map of Path → List<Path> (edges), with query method dependenciesOf()
- Use: Determine which files to validate before rendering

**ValidationError / RenderError / PreviewError:**
- Purpose: Type-safe error representation across pipeline stages
- Examples: `src/main/java/ch/ti/gagi/xsleditor/validation/ValidationError.java`, `src/main/java/ch/ti/gagi/xsleditor/render/RenderError.java`, `src/main/java/ch/ti/gagi/xsleditor/preview/PreviewError.java`
- Pattern: Record types (immutable), progressively enriched as they flow through error handling
- Use: Maintain error context (file, line, message, type) without losing precision

**RenderResult / Preview:**
- Purpose: Type-safe wrapper for success (PDF bytes) or failure (error list)
- Examples: `src/main/java/ch/ti/gagi/xsleditor/render/RenderResult.java`, `src/main/java/ch/ti/gagi/xsleditor/preview/Preview.java`
- Pattern: Sealed union (either success() or failure()), accessed via boolean success() + getters
- Use: Prevent null references; force explicit handling of both success and error cases

**ProjectFile:**
- Purpose: Represents an in-memory editor buffer with dirty/clean tracking
- Examples: `src/main/java/ch/ti/gagi/xsleditor/model/ProjectFile.java`
- Pattern: Mutable state container with explicit markClean/markDirty/setOpen methods
- Use: Track which files have unsaved changes; coordinate save-before-render

## Entry Points

**RenderOrchestrator.renderSafe():**
- Location: `src/main/java/ch/ti/gagi/xsleditor/render/RenderOrchestrator.java`
- Triggers: Render button click (or equivalent UI action)
- Responsibilities: Execute full pipeline with exception handling, return RenderResult

**ProjectManager.loadProject():**
- Location: `src/main/java/ch/ti/gagi/xsleditor/model/ProjectManager.java`
- Triggers: Open project action in UI
- Responsibilities: Load .xslfo-tool.json config, instantiate Project

**ProjectFileManager.load() / save():**
- Location: `src/main/java/ch/ti/gagi/xsleditor/model/ProjectFileManager.java`
- Triggers: Open file editor tab, save file action
- Responsibilities: Read/write file content from/to disk

**PreviewManager.generatePreview():**
- Location: `src/main/java/ch/ti/gagi/xsleditor/preview/PreviewManager.java`
- Triggers: Render button (from UI)
- Responsibilities: Orchestrate RenderOrchestrator, adapt result to Preview DTO for UI consumption

## Error Handling

**Strategy:** Fail-fast validation with exhaustive error collection

**Patterns:**

- **Validation Errors (Early Fail):** ValidationEngine.validateProject() collects ALL validation errors before returning, allowing UI to display multiple issues at once

- **Exception → RenderError:** ErrorManager.fromException() catches checked and unchecked exceptions from Saxon/FOP/IO and converts to typed RenderError with location extraction

- **Safe vs Unsafe Rendering:**
  - `RenderOrchestrator.render()` throws Exception (unsafe; used for non-UI contexts)
  - `RenderOrchestrator.renderSafe()` returns RenderResult (safe; catches all exceptions and wraps in failure RenderResult)

- **Circular Dependency Detection:** DependencyResolver.collect() maintains a stack of visited nodes to detect cycles before they cause infinite recursion

## Cross-Cutting Concerns

**Logging:** LogManager provides append-only in-memory log with level-based filtering (INFO/WARN/ERROR). No integration with render pipeline yet; ready for future UI log panel.

**Validation:** Happens once, early, before any stateful operations. XML schema validation uses DocumentBuilderFactory with namespace awareness.

**Authentication:** Not applicable. Local-only desktop tool, no auth layer.

**Path Normalization:** All paths use Java NIO (java.nio.file.Path) with .normalize() to handle relative paths, symlinks, and OS-specific separators consistently.

---

*Architecture analysis: 2026-04-14*

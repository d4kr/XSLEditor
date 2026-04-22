# Codebase Structure

**Analysis Date:** 2026-04-14

## Directory Layout

```
XSLEditor/
├── src/main/java/ch/ti/gagi/xsleditor/    # All production source code
│   ├── model/                              # Domain objects and project management
│   ├── render/                             # XSLT compilation, XSL-FO, PDF rendering
│   ├── preview/                            # Output DTO and orchestration for UI
│   ├── validation/                         # XML well-formedness validation
│   ├── dependency/                         # XSLT dependency graph analysis
│   ├── library/                            # Custom <?LIBRARY ?> preprocessing
│   ├── error/                              # Exception classification and mapping
│   └── log/                                # In-memory structured logging
├── src/main/                               # Resources (future: templates, configs)
├── build.gradle                            # Gradle build configuration (Java 21, deps)
├── gradle/wrapper/                         # Gradle wrapper for consistent builds
├── docs/                                   # Project documentation (PRD, specs)
├── .planning/codebase/                     # GSD agent-generated architecture docs
├── specs/                                  # Specification files
├── prompts/                                # AI prompt templates
├── tasks/                                  # Task definitions
├── .claude/                                # Claude-specific instructions and skills
└── .agents/                                # Agent configuration
```

## Directory Purposes

**src/main/java/ch/ti/gagi/xsleditor/:**
- Purpose: All production Java source code
- Contains: 21 classes across 8 functional packages
- Key files: RenderOrchestrator (main entry), PreviewManager (UI-facing facade)

**src/main/java/ch/ti/gagi/xsleditor/model/:**
- Purpose: Domain model and project lifecycle management
- Contains: Project, ProjectConfig, ProjectFile, ProjectFileManager, ProjectManager
- Key files: `Project.java` (immutable project context), `ProjectManager.java` (config loading)

**src/main/java/ch/ti/gagi/xsleditor/render/:**
- Purpose: Core rendering pipeline (XSLT → XSL-FO → PDF)
- Contains: RenderOrchestrator, RenderEngine, RenderResult, RenderError
- Key files: `RenderOrchestrator.java` (orchestrates steps 1-7 of render flow), `RenderEngine.java` (Saxon/FOP integration)

**src/main/java/ch/ti/gagi/xsleditor/preview/:**
- Purpose: Output marshaling for UI consumption
- Contains: PreviewManager, Preview, PreviewError
- Key files: `PreviewManager.java` (facade for UI to call), `Preview.java` (success/failure DTO)

**src/main/java/ch/ti/gagi/xsleditor/validation/:**
- Purpose: XML well-formedness validation
- Contains: ValidationEngine, ValidationError
- Key files: `ValidationEngine.java` (validates individual files or whole project graph)

**src/main/java/ch/ti/gagi/xsleditor/dependency/:**
- Purpose: XSLT dependency graph analysis
- Contains: DependencyResolver, DependencyGraph
- Key files: `DependencyResolver.java` (parses xsl:include/xsl:import, detects cycles)

**src/main/java/ch/ti/gagi/xsleditor/library/:**
- Purpose: Custom preprocessing directive expansion
- Contains: LibraryPreprocessor, LibraryProcessingException
- Key files: `LibraryPreprocessor.java` (scans for <?LIBRARY NAME?>, inlines NAME.xsl files)

**src/main/java/ch/ti/gagi/xsleditor/error/:**
- Purpose: Exception classification and normalization
- Contains: ErrorManager (static utility)
- Key files: `ErrorManager.java` (converts checked/unchecked exceptions → RenderError with location extraction)

**src/main/java/ch/ti/gagi/xsleditor/log/:**
- Purpose: In-memory structured logging
- Contains: LogManager, LogEntry
- Key files: `LogManager.java` (append-only entries with level filtering), `LogEntry.java` (immutable record)

**build.gradle:**
- Purpose: Gradle build configuration
- Contains: Java 21 toolchain, dependency declarations (Jackson, Saxon-HE, FOP, JUnit)
- Configured for: JAR compilation, test execution

**docs/:**
- Purpose: Project documentation
- Contains: PRD.md (product requirements), SPEC.md (technical spec pointers)

**.planning/codebase/:**
- Purpose: GSD agent-generated architecture documents
- Contains: ARCHITECTURE.md, STRUCTURE.md, STACK.md, INTEGRATIONS.md

## Key File Locations

**Entry Points:**

- `src/main/java/ch/ti/gagi/xsleditor/render/RenderOrchestrator.java`: Main orchestrator for render pipeline; call `renderSafe()` to execute with error handling
- `src/main/java/ch/ti/gagi/xsleditor/preview/PreviewManager.java`: UI-facing facade; call `generatePreview(project, rootPath)` to render and get Preview DTO
- `src/main/java/ch/ti/gagi/xsleditor/model/ProjectManager.java`: Load project from disk; call `loadProject(rootPath)` to initialize from .xslfo-tool.json

**Configuration:**

- `build.gradle`: Build configuration and dependency management
- `.xslfo-tool.json`: Project configuration (created in project root by user); contains `entryPoint` and `xmlInput` relative paths

**Core Logic:**

- `src/main/java/ch/ti/gagi/xsleditor/render/RenderEngine.java`: Saxon XSLT compilation and Apache FOP PDF generation
- `src/main/java/ch/ti/gagi/xsleditor/validation/ValidationEngine.java`: XML schema validation via DocumentBuilderFactory
- `src/main/java/ch/ti/gagi/xsleditor/dependency/DependencyResolver.java`: Analyzes xsl:include/xsl:import, builds dependency graph with circular cycle detection
- `src/main/java/ch/ti/gagi/xsleditor/library/LibraryPreprocessor.java`: Scans for <?LIBRARY ?> directives and inlines .xsl files

**Testing:**

- No test directory present yet (tests to be added in `src/test/java/ch/ti/gagi/xsleditor/`)

## Naming Conventions

**Files:**
- Each class in its own file, matching class name (standard Java convention)
- Example: `class Project` in `Project.java`, `class RenderOrchestrator` in `RenderOrchestrator.java`

**Directories:**
- Lowercase, one per functional domain
- Examples: `model/`, `render/`, `validation/`, `dependency/`, `library/`, `error/`, `log/`, `preview/`

**Classes:**
- PascalCase (standard Java convention)
- Examples: Project, ProjectConfig, RenderOrchestrator, ValidationEngine
- Utility classes marked `final` with private constructor (e.g., ErrorManager, ValidationEngine)
- Domain DTOs/records marked `record` (immutable) or `final` class with accessor methods

**Methods:**
- camelCase (standard Java convention)
- Getter accessors: property name without `get` prefix (e.g., `project.entryPoint()` instead of `project.getEntryPoint()`)
- Example: `renderSafe()`, `buildGraph()`, `validateProject()`, `generatePreview()`

**Packages:**
- Reverse domain name convention: `ch.ti.gagi.xsleditor.{functional-area}`
- Example: `ch.ti.gagi.xsleditor.render`, `ch.ti.gagi.xsleditor.validation`

## Where to Add New Code

**New Feature (e.g., new pipeline stage):**
- If it's a validation/transformation stage: Create new package under `src/main/java/ch/ti/gagi/xsleditor/{feature}/` with static utility class
- If it transforms error types: Add mapping to `ErrorManager.java` or create new exception classifier
- Update `RenderOrchestrator.renderSafe()` to call new stage in sequence
- Example: To add "spell-check" stage after validation, create `src/main/java/ch/ti/gagi/xsleditor/spellcheck/SpellCheckEngine.java`

**New Component/Module (e.g., new service):**
- Implementation: `src/main/java/ch/ti/gagi/xsleditor/{module-name}/`
- Follow single-responsibility principle; package groups related classes
- If stateless: Use static factory methods or static utility (no constructor instantiation needed)
- If stateful: Constructor-inject dependencies, provide accessor methods for immutable state
- Example structure for new module: `ModuleName.java` (main class), `ModuleError.java` (exception), `ModuleResult.java` (DTO if needed)

**Utilities:**
- Shared helpers: `src/main/java/ch/ti/gagi/xsleditor/{module-name}/`
- Do NOT create a separate `util/` package; keep utilities in the module they serve
- Example: LibraryPreprocessor.detectLibraries() is a utility within the library package

**Tests:**
- Location: `src/test/java/ch/ti/gagi/xsleditor/{matching-package}/`
- Naming: `{ClassName}Test.java`
- Runner: JUnit 5 (configured in `build.gradle`)
- Example: `src/test/java/ch/ti/gagi/xsleditor/render/RenderEngineTest.java` for testing `RenderEngine.java`

## Special Directories

**build/:**
- Purpose: Compiled artifacts and build outputs
- Generated: Yes (by Gradle)
- Committed: No (in .gitignore)

**.gradle/:**
- Purpose: Gradle cache and wrapper distribution
- Generated: Yes (by Gradle wrapper)
- Committed: No (in .gitignore)

**docs/:**
- Purpose: Product and technical documentation
- Generated: No (manually maintained)
- Committed: Yes

**.planning/codebase/:**
- Purpose: GSD agent-generated architecture documents
- Generated: Yes (by /gsd-map-codebase agents)
- Committed: Yes (for team reference)

**.claude/:**
- Purpose: Claude Code-specific instructions and skills
- Generated: No (manually maintained)
- Committed: Yes

---

*Structure analysis: 2026-04-14*

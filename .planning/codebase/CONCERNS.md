# Codebase Concerns

**Analysis Date:** 2026-04-14

## Tech Debt

### RenderEngine: Expensive Factory Initialization at Instantiation
- Issue: `RenderEngine` creates a new `Processor` and `FopFactory` instance in its constructor, which are expensive operations that happen every time an instance is created
- Files: `src/main/java/ch/ti/gagi/xlseditor/render/RenderEngine.java` (lines 21-30)
- Impact: 
  - Performance degradation: Each render invocation creates expensive XML/XSLT processing infrastructure
  - Memory overhead: Processor and FopFactory are heavyweight objects that should be reused
  - Potential memory leaks if not properly closed or if render pipeline is called frequently
- Fix approach: 
  - Refactor `RenderEngine` to use singleton or static factory pattern for `Processor` and `FopFactory`
  - Alternatively, pass these factories as dependencies (constructor injection) from a higher-level orchestrator that manages their lifecycle
  - Consider lazy initialization if these are not always needed

### Repeated DocumentBuilderFactory Creation
- Issue: Both `DependencyResolver` and `ValidationEngine` create new `DocumentBuilderFactory` instances every time they parse a file
- Files: 
  - `src/main/java/ch/ti/gagi/xlseditor/dependency/DependencyResolver.java` (line 68)
  - `src/main/java/ch/ti/gagi/xlseditor/validation/ValidationEngine.java` (line 18)
- Impact:
  - Performance degradation during dependency resolution and validation phases when multiple files must be parsed
  - Each factory creation triggers object allocation and initialization
- Fix approach:
  - Cache or reuse a single `DocumentBuilderFactory` instance per class (static or instance-level)
  - Ensure thread-safety if XML parsing becomes concurrent

### Insufficient Error Type Differentiation
- Issue: `ErrorManager.fromException()` maps exceptions to generic types (XSLT, FOP, IO, UNKNOWN) but loses specific error context
- Files: `src/main/java/ch/ti/gagi/xlseditor/error/ErrorManager.java` (lines 18-32)
- Impact:
  - UI cannot differentiate between different XSLT errors (compile vs. runtime)
  - User-facing error messages may be unhelpful for debugging
  - Missing error types for LIBRARY processing errors
- Fix approach:
  - Create more granular error type categorization (XsltCompile, XsltRuntime, XslFoParse, LibraryResolution, etc.)
  - Implement custom exception wrappers that preserve error context
  - Map `LibraryProcessingException` to a dedicated error type

### Error Message Extraction Lacks Robustness
- Issue: `PreviewManager.toPreviewErrors()` parses error location strings using basic string manipulation with `lastIndexOf(':')`
- Files: `src/main/java/ch/ti/gagi/xlseditor/preview/PreviewManager.java` (lines 36-48)
- Impact:
  - Windows file paths with drive letters (e.g., `C:/path`) will be incorrectly parsed
  - Paths containing colons in filenames will break parsing
  - Silent fallback to treating full location as filename without line number
- Fix approach:
  - Use Path parsing instead of string manipulation
  - Create a dedicated `ErrorLocation` value object with file and line as separate fields
  - Add tests for cross-platform path handling

## Known Bugs & Design Issues

### LIBRARY Preprocessing May Silently Duplicate Code
- Issue: `LibraryPreprocessor.mergeLibraries()` replaces directives with file content but doesn't validate for duplicate library includes in the dependency graph
- Files: `src/main/java/ch/ti/gagi/xlseditor/library/LibraryPreprocessor.java` (lines 33-61)
- Impact:
  - If a library is included both via `<?LIBRARY?>` and via `xsl:include`, it will be duplicated in final XSLT
  - Can cause template conflicts or unexpected behavior in XSLT processing
  - No warning to user that duplication occurred
- Trigger: Load a project where a library file is both directly included and referenced via LIBRARY directive
- Workaround: Manual deduplication in project files
- Fix approach:
  - Integrate LIBRARY resolution into dependency graph building
  - Validate that LIBRARY names don't conflict with files in the dependency graph

### Validation Errors Don't Distinguish Well-Formedness from Schema
- Issue: `ValidationEngine` only validates XML well-formedness using `DocumentBuilderFactory.parse()` but specs mention "XSL schema" validation without implementation
- Files: `src/main/java/ch/ti/gagi/xlseditor/validation/ValidationEngine.java` (lines 16-39)
- Impact:
  - Invalid XSL (e.g., unknown elements, bad attributes) aren't caught at validation phase
  - Errors surface later during XSLT compilation, losing location context
  - User sees generic Saxon compile errors instead of early validation messages
- Fix approach:
  - Add XSD validation for XSLT files against XSL namespace
  - Distinguish `ValidationError` types: WELLFORMED_ERROR vs SCHEMA_ERROR

### Circular Dependency Detection Incomplete
- Issue: `DependencyResolver.collect()` detects circular dependencies in import/include chains but not in LIBRARY preprocessing
- Files: `src/main/java/ch/ti/gagi/xlseditor/dependency/DependencyResolver.java` (lines 37-65)
- Impact:
  - If LIBRARY A includes LIBRARY B, and B includes A (indirectly), this won't be detected until XSLT parse fails
  - User gets cryptic Saxon error instead of early "circular dependency" message
- Fix approach:
  - Extend `DependencyResolver` to include LIBRARY files in graph building
  - Check for cycles after LIBRARY merging

### Null Pointer Risk in ProjectConfig
- Issue: `ProjectConfig.read()` returns null fields if JSON nodes don't exist, then the constructor validates them
- Files: `src/main/java/ch/ti/gagi/xlseditor/model/ProjectConfig.java` (lines 28-38)
- Impact:
  - If `.xslfo-tool.json` is missing required fields, `read()` returns `ProjectConfig(null, null)` triggering constructor exception
  - Exception message says "entryPoint must not be blank" even if key is missing from JSON
  - No distinction between missing field and blank value
- Fix approach:
  - Add explicit null check before constructor call in `read()`
  - Throw `IOException` with context-specific message if required fields are missing
  - Consider using `Optional` for clearer intent

### Render Pipeline Repeats Dependency Resolution
- Issue: Both `RenderOrchestrator.render()` and `RenderOrchestrator.renderSafe()` duplicate the entire render pipeline steps (lines 22-46 vs 51-82)
- Files: `src/main/java/ch/ti/gagi/xlseditor/render/RenderOrchestrator.java`
- Impact:
  - Code duplication makes maintenance harder
  - Bug fixes must be applied in two places
  - Inconsistency risk if one path diverges from the other
- Fix approach:
  - Extract common pipeline steps into a shared private method
  - Have `render()` call `renderSafe()` and throw on failure, or wrap `renderSafe()` directly
  - Add a parameter to control error-throwing vs. error-returning behavior

## Security Considerations

### Path Traversal Vulnerability in Dependency Resolution
- Risk: `DependencyResolver.parseHrefs()` resolves href attributes without validating against traversal
- Files: `src/main/java/ch/ti/gagi/xlseditor/dependency/DependencyResolver.java` (lines 52-54)
- Current mitigation: Files are resolved relative to rootPath and users are internal developers
- Recommendations:
  - Validate that resolved paths remain within rootPath using `Path.relativize()` and ensuring no `..` remains
  - Consider whitelist-based validation for href values
  - Add guards in `ProjectFileManager.save()` to prevent writing outside rootPath

### LIBRARY Filepath Not Sanitized
- Risk: LIBRARY directive accepts arbitrary names; malicious XSLT could reference `../../../etc/passwd.xsl`
- Files: `src/main/java/ch/ti/gagi/xlseditor/library/LibraryPreprocessor.java` (line 44)
- Current mitigation: Tool is for internal developers only; no untrusted XSLT input
- Recommendations:
  - Validate library names contain only alphanumeric, `_`, `-` characters
  - Ensure resolved paths stay within rootPath

### XML External Entity (XXE) Injection
- Risk: `DocumentBuilderFactory` in `DependencyResolver` and `ValidationEngine` don't disable external entity processing
- Files:
  - `src/main/java/ch/ti/gagi/xlseditor/dependency/DependencyResolver.java` (line 68)
  - `src/main/java/ch/ti/gagi/xlseditor/validation/ValidationEngine.java` (line 18)
- Current mitigation: Internal tool with local files only
- Recommendations:
  - Disable DTD and external entities: `factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)`
  - Set XXE prevention features on `DocumentBuilderFactory`

## Performance Bottlenecks

### Full Validation on Every Render
- Problem: `RenderOrchestrator.renderSafe()` validates all dependency files even if none are modified (line 55)
- Files: `src/main/java/ch/ti/gagi/xlseditor/render/RenderOrchestrator.java` (lines 51-82)
- Cause: `ValidationEngine.validateProject()` parses every file in dependency graph unconditionally
- Improvement path:
  - Track file modification timestamps in `ProjectFile.isDirty()`
  - Pass dirty status to validation phase
  - Only validate modified files and their dependents (reverse-dependency graph)
  - Cache validation results per file with invalidation on modification

### Linear Error Accumulation in Validation
- Problem: `ValidationEngine.validateAll()` collects errors sequentially without early exit
- Files: `src/main/java/ch/ti/gagi/xlseditor/validation/ValidationEngine.java` (lines 53-61)
- Impact: Rendering waits for all files to parse even if first file has critical error
- Improvement path:
  - Add optional `failFast` mode that returns on first error
  - Parse files in dependency order so upstream errors are caught first

### No Caching of Compiled XSLT
- Problem: `RenderOrchestrator` recompiles XSLT from string on every render (line 68)
- Impact: Compilation is expensive; repeated renders of identical XSLT waste CPU
- Improvement path:
  - Cache `XsltExecutable` keyed by content hash or merged XSLT string
  - Invalidate cache when any dependency file changes
  - Consider using `RenderEngine` instance as cache holder

## Fragile Areas

### DependencyResolver File Parsing
- Files: `src/main/java/ch/ti/gagi/xlseditor/dependency/DependencyResolver.java` (lines 67-85)
- Why fragile:
  - Parses XSLT files without validating they are well-formed XML first
  - Generic `Exception` catch mask real parsing errors
  - No protection against malformed XSLT breaking dependency resolution
- Safe modification:
  - Always validate XML before dependency resolution
  - Wrap parsing in try-catch with specific exception types
  - Test with truncated/malformed XSLT files
- Test coverage: No tests found for edge cases (incomplete files, missing namespace declarations, etc.)

### Validation Error to UI Mapping
- Files:
  - `src/main/java/ch/ti/gagi/xlseditor/error/ErrorManager.java` (lines 54-61)
  - `src/main/java/ch/ti/gagi/xlseditor/preview/PreviewManager.java` (lines 30-53)
- Why fragile:
  - Multiple string-to-object transformations with implicit assumptions
  - "XSLT" type used for validation errors even though source is XML parsing
  - Location parsing uses brittle string splitting
  - No tests visible to verify round-trip correctness
- Safe modification:
  - Create strongly-typed error chain instead of string-based conversion
  - Test with all error type combinations
- Test coverage: No visible test files in project

### RenderEngine Singleton Dependency
- Files: `src/main/java/ch/ti/gagi/xlseditor/render/RenderEngine.java` (lines 19-30)
- Why fragile:
  - `PreviewManager` creates `RenderOrchestrator` which uses a single `RenderEngine` instance
  - If `RenderEngine` state is accidentally shared or not properly initialized, affects preview pipeline
  - No way to swap implementations for testing
- Safe modification:
  - Make `RenderEngine` a dependency injected by caller
  - Add factory method for testing

## Scaling Limits

### No Limit on In-Memory Log Storage
- Current capacity: Unbounded `List<LogEntry>` in `LogManager` (line 8)
- Limit: If render operations run repeatedly, logs grow indefinitely
- Scaling path:
  - Implement circular buffer with configurable max size
  - Add log rotation/archival
  - Provide `clear()` method for manual cleanup

### No Streaming for Large PDFs
- Current: Entire PDF held in memory as `byte[]` in `Preview` and `RenderResult`
- Impact: Large documents (100+ MB) will strain heap
- Scaling path:
  - Support file-based PDF storage instead of in-memory
  - Implement lazy loading for preview display
  - Add progress indication for long renders

### No Async Render Support
- Current: Synchronous render pipeline blocks caller
- Impact: UI freeze during validation/XSLT compilation on slow hardware
- Scaling path:
  - Add async/callback interface to `RenderOrchestrator`
  - Support cancellation of in-flight renders
  - Add timeout protection

## Test Coverage Gaps

### No Unit Tests Present
- What's not tested: All core functionality
- Files: No `/src/test` directory found
- Risk:
  - Regressions in dependency resolution not caught
  - Error mapping logic untested
  - Path handling across platforms not validated
  - Edge cases (circular deps, malformed XML, missing files) not verified
- Priority: **High** — Critical components like dependency resolution and error handling need test coverage

### No Validation Tests
- What's not tested: XML/XSLT validation logic
- Files: `src/main/java/ch/ti/gagi/xlseditor/validation/ValidationEngine.java`
- Risk:
  - Invalid XSLT structure errors not caught early
  - Validation error location extraction untested
  - Well-formedness checks may silently skip files
- Priority: **High**

### No Integration Tests
- What's not tested: Full render pipeline with real files
- Risk:
  - LIBRARY preprocessing + dependency resolution + validation interaction not validated
  - Error flow from XSLT compile through to PreviewError not tested
  - Different error type handling not verified
- Priority: **High**

### No Edge Case Tests
- What's not tested:
  - Circular dependency detection
  - Path traversal prevention
  - Cross-platform path handling (Windows vs Unix)
  - Large file handling
  - Special characters in filenames/paths
- Priority: **Medium-High**

---

*Concerns audit: 2026-04-14*

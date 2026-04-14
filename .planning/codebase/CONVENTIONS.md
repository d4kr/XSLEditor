# CONVENTIONS.md — Code Style and Patterns

## Naming Conventions

- **Classes**: PascalCase with extensive `final` modifiers for immutability
- **Methods**: camelCase with semantic names — `renderSafe()`, `transformToString()`, `validateProject()`
- **Constants**: UPPER_SNAKE_CASE — `CONFIG_FILENAME`, `XSL_NS`
- **Packages**: organized by domain — `.model`, `.render`, `.validation`, `.dependency`, `.library`, `.error`, `.preview`, `.log`

## Code Style

- No formal linting/formatting config (no Checkstyle, Spotbugs configured)
- Conventional Java 4-space indentation
- All classes declared `final` — immutability by default
- Records preferred for DTOs: `ProjectConfig`, `LogEntry`, `ValidationError`, `RenderError`, `PreviewError`

## Immutability Patterns

- Extensive use of `List.copyOf()`, `List.of()`, `Collectors.toUnmodifiableMap()`
- Unmodifiable map collections in `DependencyGraph`
- Return types prefer immutable copies

## Error Handling

- Centralized exception mapping in `ErrorManager.java` — classifies exceptions by domain: XSLT, FOP, IO, UNKNOWN
- Safe pipeline execution pattern: try-catch returning `RenderResult` objects instead of throwing
- Validation-first approach with complete error list accumulation before aborting
- `PreviewError` DTO decouples pipeline errors from renderer-specific errors

## Logging

- In-memory `LogManager` storing immutable `LogEntry` records
- Log levels as strings: `"INFO"`, `"WARN"`, `"ERROR"`
- Filtering via `getByLevel(String level)` method
- Convenience methods: `info()`, `warn()`, `error()`

## Domain Packages

| Package | Responsibility |
|---|---|
| `.model` | DTOs and value objects (`ProjectConfig`, `XsltFile`, etc.) |
| `.render` | XSLT/FO rendering pipeline |
| `.validation` | XML validation logic |
| `.dependency` | Dependency graph tracking |
| `.library` | Custom preprocessing directives |
| `.error` | Error classification and management |
| `.preview` | Preview orchestration and error DTOs |
| `.log` | In-memory log management |

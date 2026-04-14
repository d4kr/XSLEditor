# TESTING.md — Test Structure and Practices

## Test Framework

- **Runner**: JUnit 5.10.0 (Jupiter)
- **Configuration**: `useJUnitPlatform()` in `build.gradle`
- **Status**: No test files currently present in the codebase

## Test Infrastructure

- Gradle configured with JUnit 5 platform
- No test directory structure exists (`src/test/java/` absent)
- No test fixtures, factories, or helper utilities found
- No mocking framework configured (Mockito, etc.)
- No test data fixtures or sample XML/XSLT files for testing

## Coverage

- No coverage requirements enforced
- No coverage tool configured (JaCoCo, etc.)

## Notes

The project has JUnit 5 as a dependency in `build.gradle` but zero test files written. The codebase is in early development phase. All validation currently happens at runtime through manual use of the tool.

Test directory to create when tests are added: `src/test/java/`

# Phase 13: Full Project Rename (XSLEditor -> XSLEditor)

## Context
The project was incorrectly named "XSLEditor" in multiple places (packages, classes, strings, documentation). It must be renamed to **XSLEditor** (XSL Editor) to correctly reflect its purpose (XSLT/XSL-FO).

## Goals
- Rename all occurrences of `XSLEditor` to `XSLEditor` (case-sensitive) and `xsleditor` to `xsleditor` (case-sensitive).
- Move Java package from `ch.ti.gagi.xsleditor` to `ch.ti.gagi.xsleditor`.
- Rename `XSLEditorApp` class to `XSLEditorApp`.
- Update build system (`build.gradle`, `settings.gradle`) and resources (FXML).
- Update all documentation files in `.planning/` and root.

## Verification
- `./gradlew clean test` passes.
- `./gradlew run` starts the application with the new name.
- No `XSLEditor` string remains in the codebase (except possibly in git history).

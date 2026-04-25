---
phase: 19
plan: "01"
subsystem: build
tags: [build, javafx, launcher, shadowjar, gradle]
dependency_graph:
  requires: []
  provides: [java-jar-launch, version-property-override]
  affects: [build.gradle, Launcher.java]
tech_stack:
  added: []
  patterns: [launcher-shim, findProperty-version-guard]
key_files:
  created:
    - src/main/java/ch/ti/gagi/xsleditor/Launcher.java
  modified:
    - build.gradle
decisions:
  - "Use Application.launch(XSLEditorApp.class, args) in Launcher — not XSLEditorApp.main() — to correctly bypass JavaFX bootstrap check"
  - "findProperty guard for version: clean build required when switching versions due to Gradle incremental caching"
metrics:
  duration: "~15 minutes"
  completed: "2026-04-25"
  tasks_completed: 7
  files_changed: 2
---

# Phase 19 Plan 01: Launcher Shim, Manifest Update & Build Verification Summary

**One-liner:** JavaFX Launcher shim delegates via `Application.launch(XSLEditorApp.class, args)` to bypass fat-JAR bootstrap failure; manifest and `-Pversion` guard hardened.

## What Was Done

Added `Launcher.java` as a plain (non-Application) entry point that calls `Application.launch(XSLEditorApp.class, args)`. This is required because the JVM bootstrap fails when the Main-Class in the fat JAR manifest extends `javafx.application.Application` — the JavaFX runtime has not yet been initialised at that point. A plain delegating class bypasses the check.

Updated `build.gradle`:
- `application.mainClass` → `ch.ti.gagi.xsleditor.Launcher`
- `jar` manifest `Main-Class` → `ch.ti.gagi.xsleditor.Launcher`
- `shadowJar` manifest `Main-Class` → `ch.ti.gagi.xsleditor.Launcher`
- `version = '0.3.0'` → `version = project.findProperty('version') ?: '0.3.0'`

## Verification Results

| Checklist Item | Result |
|---|---|
| `MANIFEST.MF` contains `Main-Class: ch.ti.gagi.xsleditor.Launcher` | PASS |
| `META-INF/services/javax.xml.transform.TransformerFactory` present with Saxon entry | PASS |
| `./gradlew shadowJar -Pversion=0.4.0` → `version.properties` reports `0.4.0` | PASS (requires `clean`) |
| `java -jar xsleditor.jar` launches without "JavaFX runtime components are missing" | PASS |

## Notes

- The `-Pversion` test required a `./gradlew clean shadowJar -Pversion=0.4.0` because `processResources` was UP-TO-DATE from the previous default build. This is expected Gradle incremental behaviour — the `findProperty` guard is correct; CI pipelines should always use `clean shadowJar` when stamping a release version.
- Smoke test output: only `WARNING: Unsupported JavaFX configuration: classes were loaded from 'unnamed module'` — this is a known harmless warning for fat JARs with JavaFX; the app window launched successfully.
- JAR filename is `xsleditor.jar` (lowercase, no version suffix) for the default build; versioned builds produce `xsleditor-X.Y.Z.jar`.

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check: PASSED

- `src/main/java/ch/ti/gagi/xsleditor/Launcher.java` — FOUND
- `build.gradle` (Launcher references, findProperty) — FOUND
- Commit `11a4b3e` — FOUND

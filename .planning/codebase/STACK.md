# Technology Stack

**Analysis Date:** 2026-04-14

## Languages

**Primary:**
- Java 21 - All backend logic and core pipeline (XML validation, XSLT compilation, XSL-FO rendering)

## Runtime

**Environment:**
- Java 21 (via Gradle toolchain)

**Package Manager:**
- Gradle 9.5.0-rc-2
- Lockfile: Not applicable (dependencies declared in `build.gradle`)

## Frameworks

**Core:**
- Saxon-HE 12.4 - XSLT 2.0 compiler and XPath processor for template transformation
- Apache FOP 2.9 - XSL-FO to PDF rendering engine

**Testing:**
- JUnit Jupiter 5.10.0 - Unit and integration test framework

**Build/Dev:**
- Gradle 9.5.0-rc-2 - Build automation and dependency management

## Key Dependencies

**Critical:**
- `net.sf.saxon:Saxon-HE:12.4` - XSLT compilation and execution (core to XML → XSLT → XSL-FO pipeline)
- `org.apache.xmlgraphics:fop:2.9` - Final PDF generation from XSL-FO output

**Data Processing:**
- `com.fasterxml.jackson.core:jackson-databind:2.17.2` - JSON parsing for project configuration files

**Testing:**
- `org.junit.jupiter:junit-jupiter:5.10.0` - Test framework with JUnit Platform

## Configuration

**Environment:**
- No environment variables required (local-only desktop tool)
- Configuration stored in project files as JSON (loaded via Jackson ObjectMapper)

**Build:**
- `build.gradle` - Gradle build configuration with Java 21 toolchain requirement
- `.java-version` or equivalent Java version management not explicitly configured (relies on system Java or CI/CD)
- `gradle/wrapper/gradle-wrapper.properties` - Gradle 9.5.0-rc-2 distribution URL and retry settings

## Platform Requirements

**Development:**
- Java 21 SDK (required by `java.toolchain.languageVersion`)
- Gradle (wrapper provided)

**Production:**
- Java 21 Runtime Environment
- No external runtime dependencies (self-contained desktop application)
- All pipeline stages (validation, XSLT, PDF generation) are in-process

## Build Output

- Compiled JAR and executable artifact in `build/` directory
- Dependencies downloaded and cached in `.gradle/` directory

---

*Stack analysis: 2026-04-14*

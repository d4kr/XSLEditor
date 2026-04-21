# Phase 9: Testing - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-21
**Phase:** 09-testing
**Areas discussed:** Fixture design, RenderEngine testability, Tech debt scope, JaCoCo coverage

---

## Fixture design

| Option | Description | Selected |
|--------|-------------|----------|
| Minimal sintatici | File ridotti all'osso: XML valido 2-3 elementi, XSLT template base, FO fo:block | ✓ |
| Realistici ma piccoli | XSLT con LIBRARY directive, include, variabili, FO con header/body | |
| Separati per modulo | fixtures/library/, fixtures/dependency/, fixtures/render/ — file separati per modulo | |

**User's choice:** Minimal sintatici

**Follow-up — LibraryPreprocessor fixture approach:**

| Option | Description | Selected |
|--------|-------------|----------|
| Inline nel test | @TempDir + Files.writeString() — zero file su disco, self-contained | ✓ |
| File statici in fixtures/ | library.xsl e template-with-library.xsl in src/test/resources/ | |

**User's choice:** Inline con @TempDir

---

## RenderEngine testability

| Option | Description | Selected |
|--------|-------------|----------|
| Pipeline reale nei test | RenderEngineTest usa Saxon + FOP veri con fixture minimali. Lento ma accurato | ✓ |
| Refactor prima dei test | Inietta Processor/FopFactory nel costruttore, poi mock nei test. Fix tech debt | |
| Solo via integration test | Nessun unit test dedicato, TEST-07 lo copre indirettamente | |

**User's choice:** Pipeline reale nei test

---

## Tech debt scope

| Option | Description | Selected |
|--------|-------------|----------|
| Test-as-is | Scrivi test sul codice attuale, nessuna modifica. Phase 9 = solo coverage | |
| Fix solo ciò che blocca i test | Se un problema impedisce un test sensato, lo si corregge. Il resto rimane | ✓ |
| Fix tutto High da CONCERNS.md | DocumentBuilderFactory reuse, ErrorManager fragility, RenderOrchestrator duplication — tutti fixati | |

**User's choice:** Fix solo ciò che blocca i test (confermato dopo raccomandazione di Claude)

**Notes:** Claude ha proposto opzione 2 con motivazione: Phase 9 ha già 8 requisiti, fixare tutto il tech debt High gonfia lo scope. Criterio: se si deve fare workaround nel test per farlo passare → fix. Altrimenti `// FIXME:` comment.

---

## JaCoCo coverage

| Option | Description | Selected |
|--------|-------------|----------|
| Sì, con soglia | Plugin + minimo (es. 80%). Build fallisce se non raggiunta | |
| Sì, solo report | Plugin senza soglia. Visibilità senza rigidità | |
| No | Skip JaCoCo. ./gradlew test basta | ✓ |

**User's choice:** No JaCoCo

---

## Claude's Discretion

- Exact package structure under src/test/java/
- Specific scenarios within each module (happy path + key error path)
- Whether to use @Nested classes
- Exact fixture content
- Whether RenderEngineTest uses @Tag("integration") for slow test isolation

## Deferred Ideas

- RenderEngine constructor refactor (inject Processor/FopFactory)
- JaCoCo coverage reporting
- PreviewManager Windows path parsing fix
- DocumentBuilderFactory instance reuse

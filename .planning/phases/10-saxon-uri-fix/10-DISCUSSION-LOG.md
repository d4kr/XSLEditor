# Phase 10: Saxon URI Fix - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-21
**Phase:** 10-saxon-uri-fix
**Areas discussed:** Exception safety, Test strategy

---

## Exception Safety

| Option | Description | Selected |
|--------|-------------|----------|
| Try/catch → strip prefix | Wrap URI.create() in try/catch, fallback strips file:// prefix manually | ✓ |
| Try/catch → return null | On failure, return null — PreviewError.file becomes null | |
| Trust Saxon | No fallback — let it throw if malformed | |

**User's choice:** Try/catch → strip prefix
**Notes:** Exception must not propagate; silent degradation preferred over crash.

---

## Test Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Unit test only | Test toPreviewErrors() with fabricated %20 URI RenderError | ✓ |
| Integration test only | Real Saxon error from space-in-path directory | |
| Both | Unit + integration | |

**User's choice:** Unit test only

---

## Test Visibility

| Option | Description | Selected |
|--------|-------------|----------|
| Package-private | Drop private modifier, test from same package | ✓ |
| Extract to helper class | Move logic to UriHelper, test independently | |

**User's choice:** Package-private (drop private modifier)

---

## Deferred Ideas

None.

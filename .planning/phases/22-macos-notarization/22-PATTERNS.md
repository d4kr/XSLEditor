# Phase 22: macOS Notarization — Pattern Map

**Mapped:** 2026-04-26
**Files analyzed:** 1 (modified file only — phase is CI workflow-only)
**Analogs found:** 1 / 1

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `.github/workflows/release.yml` | config / CI workflow | event-driven (push tag → shell commands) | `.github/workflows/release.yml` lines 108–186 (Phase 21 `Import signing certificate` + `Verify code signature` steps) | exact — same file, same job, additive steps using same secret-injection + shell guard pattern |

---

## Pattern Assignments

### `.github/workflows/release.yml` — three new steps per macOS job

**Analog:** existing steps in `package-macos-arm` and `package-macos-x64` jobs within the same file.

**Phase 22 adds exactly these three steps after `Verify code signature` and before `actions/upload-artifact` in both macOS jobs.**

---

#### Secret-injection + guard pattern

Analog: `jpackage DMG (arm64, signed)` step (lines 131–156).

The pattern for all secret-bearing steps in this workflow is:
- Declare each secret as an `env:` key on the step (never on the job)
- Open the `run:` block with `set -euo pipefail`
- Guard immediately with an `if [ -z "$VAR" ]` block; echo a human-readable error and `exit 1`
- Proceed with the command only after the guard passes

```yaml
# Lines 131-139 — jpackage DMG (arm64, signed): guard pattern to copy
- name: jpackage DMG (arm64, signed)
  env:
    MACOS_SIGNING_IDENTITY: ${{ secrets.MACOS_SIGNING_IDENTITY }}
  run: |
    # Guard: fail fast if required signing secrets are not configured
    if [ -z "$MACOS_SIGNING_IDENTITY" ]; then
      echo "ERROR: MACOS_SIGNING_IDENTITY secret is not configured"
      exit 1
    fi
```

Apply the identical structure to the new `Notarize DMG` step, guarding all three Apple secrets at once:

```yaml
# Phase 22 — Notarize DMG (arm64): new step following the same guard pattern
- name: Notarize DMG (arm64)
  env:
    APPLE_ID:                    ${{ secrets.APPLE_ID }}
    APPLE_TEAM_ID:               ${{ secrets.APPLE_TEAM_ID }}
    APPLE_APP_SPECIFIC_PASSWORD: ${{ secrets.APPLE_APP_SPECIFIC_PASSWORD }}
  run: |
    set -euo pipefail
    if [ -z "$APPLE_ID" ] || [ -z "$APPLE_TEAM_ID" ] || [ -z "$APPLE_APP_SPECIFIC_PASSWORD" ]; then
      echo "ERROR: One or more notarization secrets are not configured"
      echo "Required: APPLE_ID, APPLE_TEAM_ID, APPLE_APP_SPECIFIC_PASSWORD"
      exit 1
    fi
    xcrun notarytool submit "output/XSLEditor-${APP_VERSION}-arm64.dmg" \
      --apple-id  "$APPLE_ID" \
      --team-id   "$APPLE_TEAM_ID" \
      --password  "$APPLE_APP_SPECIFIC_PASSWORD" \
      --wait \
      --timeout 300s
    echo "Notarization complete"
```

---

#### `set -euo pipefail` + shell discipline pattern

Analog: `Verify code signature` step (lines 158–177).

Every `run:` block in the macOS signing jobs opens with `set -euo pipefail`. The Phase 22 steps must do the same.

```yaml
# Lines 158-162 — Verify code signature: shell discipline pattern to copy
- name: Verify code signature
  run: |
    set -euo pipefail
    # Mount the DMG so we can verify the .app bundle inside
    hdiutil attach "output/XSLEditor-${APP_VERSION}-arm64.dmg" \
```

---

#### Informational-only / suppressed-exit step pattern

Analog: `Delete keychain` step (lines 184–186).

Steps that must not fail the job use `|| true` to suppress non-zero exits. The `Verify Gatekeeper acceptance` step is informational and follows this pattern exactly.

```yaml
# Lines 184-186 — Delete keychain: suppressed-exit pattern to copy
- name: Delete keychain
  if: always()
  run: security delete-keychain build.keychain || true
```

Apply to the Gatekeeper check step:

```yaml
# Phase 22 — Verify Gatekeeper acceptance (informational): new step
- name: Verify Gatekeeper acceptance (informational)
  run: |
    # spctl result on CI runner may differ from end-user machine; treat as informational only
    spctl -a -v --type install "output/XSLEditor-${APP_VERSION}-arm64.dmg" 2>&1 || true
    echo "Gatekeeper check complete (informational)"
```

---

#### DMG path variable pattern

Analog: all macOS steps that reference the artifact (lines 155–156, 162–163, 180–182).

The DMG path is always constructed from `${APP_VERSION}` (job-level env) with the architecture suffix hard-coded per job. There is no helper variable — the path is inlined at each reference.

```yaml
# Line 155-156 — jpackage renames the output using this pattern
mv "output/XSLEditor-${VERSION_MACOS}.dmg" \
   "output/XSLEditor-${APP_VERSION}-arm64.dmg"

# Line 162-163 — subsequent steps reference the same path
hdiutil attach "output/XSLEditor-${APP_VERSION}-arm64.dmg" \
  -mountpoint /tmp/xsleditor-verify-arm64 -nobrowse -quiet
```

Phase 22 steps reference the same path string:
- arm64 job: `"output/XSLEditor-${APP_VERSION}-arm64.dmg"`
- x64 job:   `"output/XSLEditor-${APP_VERSION}-x64.dmg"`

---

#### Step ordering constraint

The canonical order within each macOS job (lines 108–186 for arm64, 215–293 for x64) is:

1. `Import signing certificate` (Phase 21)
2. `jpackage DMG … signed` (Phase 21)
3. `Verify code signature` (Phase 21)
4. **`Notarize DMG`** — insert here (Phase 22)
5. **`Staple DMG`** — insert here (Phase 22)
6. **`Verify Gatekeeper acceptance`** — insert here (Phase 22, informational)
7. `actions/upload-artifact` (Phase 20 — must upload the stapled DMG, same path)
8. `Delete keychain` (Phase 21, `if: always()`)

The `upload-artifact` step (lines 179–182 for arm64, 286–289 for x64) uploads `output/XSLEditor-*-arm64.dmg` / `output/XSLEditor-*-x64.dmg`. Because `xcrun stapler staple` modifies the DMG in place, the upload step does not need to change — it will pick up the stapled artifact automatically.

---

#### Complete three-step block for arm64 (ready to insert)

```yaml
      - name: Notarize DMG (arm64)
        env:
          APPLE_ID:                    ${{ secrets.APPLE_ID }}
          APPLE_TEAM_ID:               ${{ secrets.APPLE_TEAM_ID }}
          APPLE_APP_SPECIFIC_PASSWORD: ${{ secrets.APPLE_APP_SPECIFIC_PASSWORD }}
        run: |
          set -euo pipefail
          if [ -z "$APPLE_ID" ] || [ -z "$APPLE_TEAM_ID" ] || [ -z "$APPLE_APP_SPECIFIC_PASSWORD" ]; then
            echo "ERROR: One or more notarization secrets are not configured"
            echo "Required: APPLE_ID, APPLE_TEAM_ID, APPLE_APP_SPECIFIC_PASSWORD"
            exit 1
          fi
          xcrun notarytool submit "output/XSLEditor-${APP_VERSION}-arm64.dmg" \
            --apple-id  "$APPLE_ID" \
            --team-id   "$APPLE_TEAM_ID" \
            --password  "$APPLE_APP_SPECIFIC_PASSWORD" \
            --wait \
            --timeout 300s
          echo "Notarization complete"

      - name: Staple DMG (arm64)
        run: |
          set -euo pipefail
          xcrun stapler staple "output/XSLEditor-${APP_VERSION}-arm64.dmg"
          echo "Staple complete"

      - name: Verify Gatekeeper acceptance (informational)
        run: |
          # spctl result on CI runner may differ from end-user machine; treat as informational only
          spctl -a -v --type install "output/XSLEditor-${APP_VERSION}-arm64.dmg" 2>&1 || true
          echo "Gatekeeper check complete (informational)"
```

For the x64 job, replace every occurrence of `arm64` with `x64`.

---

## Shared Patterns

### Secret guard (hard-fail on missing secrets)
**Source:** `.github/workflows/release.yml` lines 135–139 (`jpackage DMG (arm64, signed)` step)
**Apply to:** Both new `Notarize DMG` steps (arm64 and x64)

```bash
if [ -z "$APPLE_ID" ] || [ -z "$APPLE_TEAM_ID" ] || [ -z "$APPLE_APP_SPECIFIC_PASSWORD" ]; then
  echo "ERROR: One or more notarization secrets are not configured"
  echo "Required: APPLE_ID, APPLE_TEAM_ID, APPLE_APP_SPECIFIC_PASSWORD"
  exit 1
fi
```

### Shell discipline
**Source:** `.github/workflows/release.yml` lines 159 and 221 (`Verify code signature` steps)
**Apply to:** All three new steps in both macOS jobs (notarize, staple, gatekeeper)

```bash
set -euo pipefail
```

### Suppressed exit for informational steps
**Source:** `.github/workflows/release.yml` line 186 (`Delete keychain` step)
**Apply to:** `Verify Gatekeeper acceptance` steps in both macOS jobs

```bash
<command> 2>&1 || true
```

### Per-step secret injection (never job-level)
**Source:** `.github/workflows/release.yml` lines 109–113 (`Import signing certificate`), lines 132–134 (`jpackage DMG`)
**Apply to:** `Notarize DMG` steps (arm64 and x64)

```yaml
env:
  APPLE_ID:                    ${{ secrets.APPLE_ID }}
  APPLE_TEAM_ID:               ${{ secrets.APPLE_TEAM_ID }}
  APPLE_APP_SPECIFIC_PASSWORD: ${{ secrets.APPLE_APP_SPECIFIC_PASSWORD }}
```

---

## No Analog Found

None. All patterns needed for Phase 22 have direct analogs in the existing `release.yml` from Phase 21.

---

## Metadata

**Analog search scope:** `.github/workflows/release.yml` (only CI workflow file in the repository)
**Files scanned:** 1
**Pattern extraction date:** 2026-04-26

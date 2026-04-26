# Phase 21: macOS Signing — Pattern Map

**Mapped:** 2026-04-25
**Files analyzed:** 2
**Analogs found:** 1 / 2

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `packaging/entitlements.plist` | config | N/A (static XML declaration) | None in repo | no-analog |
| `.github/workflows/release.yml` | config / CI workflow | event-driven (tag push → jobs) | Self (existing file; modify in place) | self-analog |

---

## Pattern Assignments

### `packaging/entitlements.plist` (config, static)

**Analog:** None — no `.plist` files exist anywhere in the repository.

**Pattern source:** RESEARCH.md § "entitlements.plist Requirements for a JavaFX/JVM App" (fully verified against Apple Developer Documentation and AdoptOpenJDK guide).

**File to create verbatim:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Required: JVM uses JIT compilation; Hardened Runtime blocks JIT by default -->
    <key>com.apple.security.cs.allow-jit</key>
    <true/>

    <!-- Required: JVM loads native libraries (JavaFX, FOP, Saxon JNI) -->
    <!-- Also the critical workaround for JDK-8358723 / JDK-8369477 when the  -->
    <!-- main launcher would otherwise load libjli.dylib without this entitlement -->
    <key>com.apple.security.cs.disable-library-validation</key>
    <true/>

    <!-- Recommended: JVM may need to map executable memory for bytecode -->
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>
    <true/>
</dict>
</plist>
```

**Location:** `packaging/entitlements.plist` (repo root relative; referenced in jpackage as `packaging/entitlements.plist` since jpackage runs from repo root in CI).

---

### `.github/workflows/release.yml` (CI workflow, event-driven — modify in place)

**Analog:** Self. The file already exists at `.github/workflows/release.yml`. The two macOS jobs (`package-macos-arm`, `package-macos-x64`) are modified; the other three jobs are untouched.

#### Existing `package-macos-arm` jpackage step (lines 66-78) — the step being replaced/extended:

```yaml
      - name: jpackage DMG (arm64)
        run: |
          mkdir -p output
          jpackage \
            --type dmg \
            --name XSLEditor \
            --app-version "$VERSION_MACOS" \
            --input dist/ \
            --main-jar "xsleditor-${APP_VERSION}.jar" \
            --main-class ch.ti.gagi.xsleditor.Launcher \
            --icon src/main/resources/ch/ti/gagi/xsleditor/icon.icns \
            --dest output/
          mv "output/XSLEditor-${VERSION_MACOS}.dmg" "output/XSLEditor-${APP_VERSION}-arm64.dmg"
```

#### Existing `package-macos-x64` jpackage step (lines 112-124) — the step being replaced/extended:

```yaml
      - name: jpackage DMG (x64)
        run: |
          mkdir -p output
          jpackage \
            --type dmg \
            --name XSLEditor \
            --app-version "$VERSION_MACOS" \
            --input dist/ \
            --main-jar "xsleditor-${APP_VERSION}.jar" \
            --main-class ch.ti.gagi.xsleditor.Launcher \
            --icon src/main/resources/ch/ti/gagi/xsleditor/icon.icns \
            --dest output/
          mv "output/XSLEditor-${VERSION_MACOS}.dmg" "output/XSLEditor-${APP_VERSION}-x64.dmg"
```

#### Existing job-level `env:` block pattern (lines 43-44, repeated at lines 89-90) — copy this pattern for secrets injection:

```yaml
    env:
      APP_VERSION: ${{ needs.build-jar.outputs.app-version }}
```

#### Existing `actions/setup-java@v4` step pattern (lines 47-51, repeated at lines 46-51 in x64 job) — already present, no change needed:

```yaml
      - uses: actions/setup-java@v4
        with:
          distribution: liberica
          java-version: '21'
          java-package: jdk+fx
```

#### New steps to INSERT before the jpackage step in both macOS jobs:

**Step 1 — Import signing certificate** (new; insert between `actions/download-artifact` and jpackage):

```yaml
      - name: Import signing certificate
        env:
          MACOS_CERTIFICATE: ${{ secrets.MACOS_CERTIFICATE }}
          MACOS_CERTIFICATE_PASSWORD: ${{ secrets.MACOS_CERTIFICATE_PASSWORD }}
          MACOS_KEYCHAIN_PASSWORD: ${{ secrets.MACOS_KEYCHAIN_PASSWORD }}
        run: |
          echo "$MACOS_CERTIFICATE" | base64 --decode > /tmp/certificate.p12
          security create-keychain -p "$MACOS_KEYCHAIN_PASSWORD" build.keychain
          security set-keychain-settings -lut 21600 build.keychain
          security unlock-keychain -p "$MACOS_KEYCHAIN_PASSWORD" build.keychain
          security import /tmp/certificate.p12 \
            -k build.keychain -f pkcs12 \
            -T /usr/bin/codesign -T /usr/bin/security \
            -P "$MACOS_CERTIFICATE_PASSWORD"
          security list-keychains -d user -s build.keychain login.keychain
          security set-key-partition-list -S apple-tool:,apple: \
            -k "$MACOS_KEYCHAIN_PASSWORD" build.keychain
          rm -f /tmp/certificate.p12
```

**Step 2 — Updated jpackage step for arm64** (replace existing "jpackage DMG (arm64)" step):

```yaml
      - name: jpackage DMG (arm64, signed)
        env:
          MACOS_SIGNING_IDENTITY: ${{ secrets.MACOS_SIGNING_IDENTITY }}
        run: |
          mkdir -p output
          jpackage \
            --type dmg \
            --name XSLEditor \
            --app-version "$VERSION_MACOS" \
            --input dist/ \
            --main-jar "xsleditor-${APP_VERSION}.jar" \
            --main-class ch.ti.gagi.xsleditor.Launcher \
            --icon src/main/resources/ch/ti/gagi/xsleditor/icon.icns \
            --dest output/ \
            --mac-sign \
            --mac-signing-key-user-name "$MACOS_SIGNING_IDENTITY" \
            --mac-entitlements packaging/entitlements.plist \
            --mac-package-identifier ch.ti.gagi.xsleditor \
            --mac-signing-keychain build.keychain
          mv "output/XSLEditor-${VERSION_MACOS}.dmg" \
             "output/XSLEditor-${APP_VERSION}-arm64.dmg"
```

**Step 2 (x64 variant) — Updated jpackage step for x64** (replace existing "jpackage DMG (x64)" step):

```yaml
      - name: jpackage DMG (x64, signed)
        env:
          MACOS_SIGNING_IDENTITY: ${{ secrets.MACOS_SIGNING_IDENTITY }}
        run: |
          mkdir -p output
          jpackage \
            --type dmg \
            --name XSLEditor \
            --app-version "$VERSION_MACOS" \
            --input dist/ \
            --main-jar "xsleditor-${APP_VERSION}.jar" \
            --main-class ch.ti.gagi.xsleditor.Launcher \
            --icon src/main/resources/ch/ti/gagi/xsleditor/icon.icns \
            --dest output/ \
            --mac-sign \
            --mac-signing-key-user-name "$MACOS_SIGNING_IDENTITY" \
            --mac-entitlements packaging/entitlements.plist \
            --mac-package-identifier ch.ti.gagi.xsleditor \
            --mac-signing-keychain build.keychain
          mv "output/XSLEditor-${VERSION_MACOS}.dmg" \
             "output/XSLEditor-${APP_VERSION}-x64.dmg"
```

**Step 3 — Verify code signature** (new; insert after jpackage step, before upload-artifact):

For arm64:
```yaml
      - name: Verify code signature
        run: |
          codesign --verify --deep --strict --verbose=2 \
            "output/XSLEditor-${APP_VERSION}-arm64.dmg"
          echo "Codesign verification passed"
          codesign -dv --verbose=4 "output/XSLEditor-${APP_VERSION}-arm64.dmg" 2>&1 \
            | grep "TeamIdentifier"
```

For x64:
```yaml
      - name: Verify code signature
        run: |
          codesign --verify --deep --strict --verbose=2 \
            "output/XSLEditor-${APP_VERSION}-x64.dmg"
          echo "Codesign verification passed"
          codesign -dv --verbose=4 "output/XSLEditor-${APP_VERSION}-x64.dmg" 2>&1 \
            | grep "TeamIdentifier"
```

**Step 4 — Delete keychain** (new; add as last step in each macOS job, always runs):

```yaml
      - name: Delete keychain
        if: always()
        run: security delete-keychain build.keychain || true
```

#### Step ordering within each macOS job after changes:

```
1. actions/checkout@v4                    (unchanged)
2. actions/setup-java@v4                  (unchanged)
3. actions/download-artifact@v4           (unchanged)
4. Compute --app-version for macOS        (unchanged)
5. Import signing certificate             (NEW)
6. jpackage DMG (arm64/x64, signed)      (REPLACE existing jpackage step)
7. Verify code signature                  (NEW)
8. actions/upload-artifact@v4            (unchanged)
9. Delete keychain                        (NEW, if: always())
```

---

## Shared Patterns

### Existing step-level `env:` injection pattern
**Source:** `.github/workflows/release.yml`, lines 43-44 (job-level env) and established convention.
**Apply to:** Each new signing step that needs secrets.
**Pattern:** Secrets are injected via step-level `env:` blocks (not job-level) to minimize secret exposure scope. The `MACOS_SIGNING_IDENTITY` is only in scope for the jpackage step; the certificate secrets are only in scope for the import step.

```yaml
        env:
          SECRET_NAME: ${{ secrets.SECRET_NAME }}
```

### Existing `if: always()` cleanup pattern
**Source:** GitHub Actions convention; no existing example in release.yml yet.
**Apply to:** The `Delete keychain` step only.
**Rationale:** Keychain cleanup must run even when earlier steps fail. The step uses only the keychain name (`build.keychain`) — no secrets required.

```yaml
        if: always()
        run: security delete-keychain build.keychain || true
```

### Existing artifact upload pattern (lines 82-84, 127-129)
**Source:** `.github/workflows/release.yml`, lines 82-84.
**Apply to:** Unchanged — the `upload-artifact` step for both macOS jobs keeps the same artifact names (`dmg-arm64`, `dmg-x64`) and path globs. The `release` job at lines 211-229 is fully unchanged.

```yaml
      - uses: actions/upload-artifact@v4
        with:
          name: dmg-arm64
          path: output/XSLEditor-*-arm64.dmg
```

---

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `packaging/entitlements.plist` | config | static | No `.plist` files exist anywhere in the repository; no `packaging/` directory exists yet. Content is fully specified in RESEARCH.md and Apple documentation. |

---

## Metadata

**Analog search scope:** Entire repository (all `.plist`, `.yml`, `.yaml` files; `.github/` directory)
**Files scanned:** 1 analog file read (`release.yml`); 0 plist analogs found
**Pattern extraction date:** 2026-04-25

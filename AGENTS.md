# AGENTS.md

## Cursor Cloud specific instructions

This is a **Kotlin Compose Multiplatform** desktop/web ER modeling application. No backend services, databases, or Docker are needed.

### Prerequisites

- **JDK 21** (already available in the VM as `openjdk 21.0.10`)
- **Gradle 9.3.0** (downloaded automatically via `./gradlew`)

### Key commands

| Action | Command |
|--------|---------|
| Run desktop app | `./gradlew :composeApp:run` |
| Run tests | `./gradlew :composeApp:desktopTest` |
| Run all checks | `./gradlew :composeApp:check` |
| Build web (wasmJs) | `./gradlew :composeApp:wasmJsBrowserDistribution` |
| Web dev server | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |

### Original Pascal sources (brModelo 3.0)

The original Delphi/Pascal source code is available outside the git repo at `/Fontes-Originais` (symlink to `/home/ubuntu/Fontes-Originais/Fontes`). It is used for:
- Consulting original implementation logic
- Extracting resources (e.g. `cursor.RES` used by the `extractBrmodeloCursors` Gradle task)

The `extractBrmodeloCursors` task expects `../Fontes-Originais/cursor.RES` relative to the project root (`/workspace`), which resolves to `/Fontes-Originais/cursor.RES`.

If the symlink is missing, recreate it:
```bash
sudo ln -sfn /home/ubuntu/Fontes-Originais/Fontes /Fontes-Originais
```

### Caveats

- The desktop app emits `[SKIKO] warn: Fallback to next API` and `Cannot create Linux GL context` in headless/VM environments. This is normal — Skiko falls back to software rendering and the app runs fine.
- The Kotlin compiler warning in `InspectorPanel.kt:2347` about a useless `is` check is a known pre-existing warning; it does not affect functionality.
- There is no separate lint tool (no detekt/ktlint configured). The Kotlin compiler warnings during `compileKotlinDesktop` serve as the lint layer. Use `./gradlew :composeApp:check` for the full verification pass.
- The wasmJs production build (`wasmJsBrowserDistribution`) takes ~5 minutes due to Binaryen WASM optimization; the desktop build is much faster.
- `gradle.properties` sets `kotlin.compiler.execution.strategy=in-process` to avoid daemon JDK mismatch issues.

# AGENTS.md

## Project overview

**Power-brModelo** is a Kotlin Compose Multiplatform desktop/web ER modeling application. No backend services, databases, or Docker are needed to develop or run it.

## Prerequisites

- **JDK 21** (e.g. `openjdk 21.0.10`)
- **Gradle 9.3.0** (downloaded automatically via `./gradlew`)

## Key commands

| Action | Command |
|--------|---------|
| Run desktop app | `./gradlew :composeApp:run` |
| Run tests | `./gradlew :composeApp:desktopTest` |
| Run all checks | `./gradlew :composeApp:check` |
| Build web (wasmJs) | `./gradlew :composeApp:wasmJsBrowserDistribution` |
| Web dev server | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |

## Original Pascal sources (brModelo 3.0)

The original Delphi/Pascal source code of brModelo 3.0 can be downloaded from:

- **SourceForge:** https://sourceforge.net/projects/brmodelo30/files/brModelo30_source.rar/download

In the development VM, the extracted source is available at `/Fontes-Originais` (symlink to `/home/ubuntu/Fontes-Originais/Fontes`). It is used for:
- Consulting original implementation logic
- Extracting resources (e.g. `cursor.RES` used by the `extractBrmodeloCursors` Gradle task)

The `extractBrmodeloCursors` task expects `../Fontes-Originais/cursor.RES` relative to the project root, which resolves to `/Fontes-Originais/cursor.RES`.

If the symlink is missing, recreate it:
```bash
sudo ln -sfn /home/ubuntu/Fontes-Originais/Fontes /Fontes-Originais
```

## Development caveats

- The desktop app emits `[SKIKO] warn: Fallback to next API` and `Cannot create Linux GL context` in headless/VM environments. This is normal — Skiko falls back to software rendering and the app runs fine.
- The Kotlin compiler warning in `InspectorPanel.kt:2347` about a useless `is` check is a known pre-existing warning; it does not affect functionality.
- There is no separate lint tool (no detekt/ktlint configured). The Kotlin compiler warnings during `compileKotlinDesktop` serve as the lint layer. Use `./gradlew :composeApp:check` for the full verification pass.
- The wasmJs production build (`wasmJsBrowserDistribution`) takes ~5 minutes due to Binaryen WASM optimization; the desktop build is much faster.
- `gradle.properties` sets `kotlin.compiler.execution.strategy=in-process` to avoid daemon JDK mismatch issues.

## Authorship and commit policy

All commits in this repository must be authored exclusively by the humans who are responsible for them. AI tools, agents, and assistants are instruments — not authors or co-authors.

**Rules:**

1. **No AI attribution in commits.** Do not add `Co-authored-by`, `Signed-off-by`, or any trailer that names an AI tool, agent, or assistant. The human using the tool is the sole author.
2. **No tool mentions in commit messages.** Commit messages should describe *what* changed and *why*, never *which tool* was used to produce the change.
3. **Author identity from environment secrets.** Before committing, read the author name and email from the environment secrets `git-author-name` and `git-author-email`. Configure git accordingly:
   ```bash
   git config user.name "$(printenv 'git-author-name')"
   git config user.email "$(printenv 'git-author-email')"
   ```
4. **GPG signing.** If the secret `git-gpg-privatekey-for-signing-commits` is present, import it and sign all commits:
   ```bash
   echo "$(<secret value>)" | gpg --batch --import
   git config user.signingkey B40638859DB47292
   git config commit.gpgsign true
   git config gpg.format openpgp
   ```
5. **Bypassing managed hooks.** The environment may have managed git hooks (e.g. at a custom `core.hookspath`) that inject co-author trailers or perform other unwanted modifications. To bypass them, use `git-direct` (a wrapper script at `/usr/local/bin/git-direct`) which invokes git with `core.hookspath=/dev/null`:
   ```bash
   git-direct add .
   git-direct commit -m "feat: my change"
   git-direct push -u origin my-branch
   ```
   If `git-direct` is not available, create it:
   ```bash
   sudo tee /usr/local/bin/git-direct << 'EOF'
   #!/bin/bash
   exec /usr/bin/git -c core.hookspath=/dev/null "$@"
   EOF
   sudo chmod +x /usr/local/bin/git-direct
   ```

## Cloud agent specific instructions

### Environment setup

The update script (run automatically on VM startup) only needs to execute `./gradlew --no-daemon dependencies` to pre-fetch Gradle and project dependencies. No other services or build steps are needed.

### Running the application

The desktop app can be launched with `./gradlew :composeApp:run`. In headless VMs, Skiko will fall back to software rendering — the warning messages are expected and harmless.

### Testing

Run `./gradlew :composeApp:desktopTest` for unit/integration tests. Run `./gradlew :composeApp:check` for the full verification pass (compilation + tests).

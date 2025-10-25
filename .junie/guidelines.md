# Project Guidelines — In Game Info

Last updated: 2025-10-26 07:58 (local)

## 1) Project Overview
In Game Info is a Minecraft Forge mod that renders configurable HUD information in-game. It aggregates data from multiple "variable providers" (e.g., player, world, FPS, biome) and displays them through HUD contexts and overlays.

- Entry point: `src/main/java/com/codeoinigiri/ingameinfo/InGameInfo.java`
  - Registers config, initializes the `VariableManager`, loads HUD contexts, and starts the config watcher.
- Client HUD overlay example: `src/main/java/com/codeoinigiri/ingameinfo/client/hud/HudOverlay.java`
- HUD/context/configs live under `run/config/ingameinfo` (e.g., `run/config/ingameinfo/context/test1.toml`).
- Formatting reference: `FEATURE_FORMATTING_CODES.md`, `FEATURE_FORMATTING_REORDER.md`.

Tech stack: Java, Minecraft Forge, Gradle. Logging via SLF4J (`LogUtils.getLogger()`).

## 2) Repository Layout (essentials)
- Root docs: `README.md`, `changelog.txt`, `CREDITS.txt`, `LICENSE.txt`
- Gradle build: `build.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/`
- Main sources: `src/main/java/com/codeoinigiri/ingameinfo/...`
  - API and variable providers: `.../api`, `.../variable`, `.../variable/provider`
  - Client HUD: `.../client/hud`, shared HUD utilities: `.../hud`
  - Config: `.../config`
- Resources: `src/main/resources`
- Tests: `src/test/java`, resources: `src/test/resources`
- Dev runtime: `run/` (logs, configs, saves) — useful when launching a dev client.

## 3) Build & Run
- Build (no run):
  - Unix/macOS: `./gradlew build`
  - Windows: `gradlew.bat build`
- Run a dev client (if ForgeGradle run configs are present):
  - `./gradlew runClient`
  - The mod will use config files from `run/config/ingameinfo`.
- Clean: `./gradlew clean`

Note: If `runClient` is not configured for this environment, you can still build the mod jar with `build` and place it into a compatible Minecraft Forge instance manually.

## 4) Tests
- Location: `src/test/java`.
- To run tests via Gradle: `./gradlew test`.
- In this environment, Junie can also use the test runner tool where applicable. If no tests exist or they are minimal, prefer targeted builds or lightweight verifications over full runs.

When to run tests/build:
- Documentation-only changes (like this file): do not run tests or build.
- Code changes:
  - Run unit tests related to changed areas (or `test` if unsure).
  - Build the project (`build`) before submitting if you changed production code.

## 5) Code Style & Conventions
- Follow existing style in the module:
  - 4-space indentation, conventional Java/K&R braces, descriptive names.
  - Keep `public static final` constants like `MOD_ID` (`InGameInfo.MOD_ID`).
  - Use the shared `LOGGER` from `InGameInfo` or a class-local logger via `LogUtils.getLogger()`.
  - Keep imports organized and avoid unused imports.
- Mirror patterns in neighboring classes (e.g., for variable providers and HUD components).
- Nullability/defensiveness: prefer early returns and null checks where Minecraft/Forge APIs can return null.
- Comments: match the project's sparsity; add concise Javadoc/KDoc where it improves clarity.

## 6) Configuration & HUD
- HUD contexts are loaded on startup (`HudContextManager.loadContexts()`) and reloaded on changes via `ConfigWatcher`.
- Place or edit context TOML files under `run/config/ingameinfo/context/`.
- Use the formatting guides (`FEATURE_FORMATTING_CODES.md` and `FEATURE_FORMATTING_REORDER.md`) when editing HUD text/variables.

## 7) Contribution Rules for Junie
- Respect the mode rules provided by the environment.
- Prefer minimal, focused changes; avoid broad refactors unless requested.
- For risky changes, explain the approach and get confirmation when possible.
- Always ensure the mod still initializes: no breaking changes to `InGameInfo` setup, `VariableManager` init, or HUD loading.

## 8) Submission Checklist
- If code was changed:
  - [ ] Code compiles locally with `./gradlew build`.
  - [ ] Related tests pass (`./gradlew test`) or are added/updated when necessary.
  - [ ] No obvious regressions in initialization paths (`InGameInfo.commonSetup`).
- If only docs were changed:
  - [x] No build/tests required.
  - [x] Writing and links reviewed for accuracy.

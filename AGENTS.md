# CleverKeys Agent & Developer Handbook

This document serves as a high-level guide for AI agents and developers working on the CleverKeys project. It synthesizes the project's infrastructure, build system, and key architectural patterns.

## 1. Project Overview
**CleverKeys** is a privacy-focused, lightweight Android virtual keyboard featuring a on-device model-based swipe prediction engine (ONNX). It prioritizes local processing (no network permissions), minimal dependencies, and high performance. It was originally designed for Termux users but has evolved into a general-purpose keyboard.

## Design Context

### Users
CleverKeys serves privacy-conscious Android users who need dependable everyday typing, with special attention to Termux and power-user workflows. Users expect the keyboard to remain useful offline, start quickly, respect accessibility services, and preserve their language and customization choices.

### Brand Personality
Private, lightweight, dependable.

### Aesthetic Direction
Preserve the existing compact, utilitarian Android keyboard and settings language. Visual changes should feel native, calm, legible, and unobtrusive in both light and dark themes; decoration must never compete with typing.

### Design Principles
- Keep core typing available across supported layouts, languages, screen sizes, and accessibility modes.
- Prefer clear state and direct feedback over decorative effects.
- Treat privacy, local-only operation, latency, and battery/memory use as user-facing design constraints.
- Make touch, TalkBack, and keyboard navigation describe and activate the same logical controls.
- Reuse established components and theme tokens instead of introducing one-off visual systems.

## 1.1 Instruction and Skill Routing

Before changing files, read `CLAUDE.md`, `memory/REPO_INSTRUCTIONS.md`, `memory/todo.md`, `docs/TABLE_OF_CONTENTS.md`, and the relevant feature spec. More-specific `AGENTS.md`/`CLAUDE.md` files override this file within their directory.

Repository skills under `.claude/skills/` are mandatory references when their topic matches:

| Work area | Required reference |
|---|---|
| Release, version, tag, changelog, Fastlane, F-Droid | `.claude/skills/release-process.md` |
| Dictionary, lexicon, vocabulary, language support | `.claude/skills/dictionary-pipeline.md` |
| Contractions, apostrophes, canonical display, collisions | `.claude/skills/contraction-system.md` |
| Settings or SharedPreferences | `.claude/skills/settings-preferences.md` |
| Instrumented tests, emulator.wtf, ew-cli | `.claude/skills/ew-cli-testing.md` |
| Wiki, Astro, public specs | `.claude/skills/wiki-documentation.md` |
| Clipboard UI/data behavior | `.claude/skills/clipboard-panel-architecture.md` and the matching tag/todo skill when applicable |

Also follow any session-provided global skill whose trigger matches. Record reusable architecture or workflow decisions in this handbook and the canonical spec instead of relying on chat history.

## 2. Build Infrastructure

### Local Build (Termux Optimized)
The project is optimized for building directly on an Android device via Termux.
-   **Script:** `./build-on-termux.sh [debug|release]`
-   **Quirks:**
    -   **AAPT2 Override:** Uses a custom `aapt2` binary (`tools/aapt2-arm64/aapt2`) because the standard SDK version is incompatible with Termux environment. This is injected via `-Pandroid.aapt2FromMavenOverride`.
    -   **Memory:** JVM args are tuned (`-Xmx2048m`) for limited resource environments.
    -   **Layout Resources:** Keyboard layouts (`src/main/layouts/*.xml`) are processed and copied to `build/generated-resources/raw` via a custom Gradle `Copy` task (`copyLayoutDefinitions`) to ensure they are available as `raw` resources for the `LayoutManager`.
    -   **Temporary Output:** Do not use `/tmp` on Termux. Use an ignored directory under `build/`, `context.cacheDir` in Android code, or the documented `~/ew-output` location for ew-cli.

### Gradle Configuration (`build.gradle`)
-   **Single Source of Truth (SSoT):** Versioning is controlled by `ext.VERSION_MAJOR`, `MINOR`, and `PATCH` at the top of `build.gradle`. `versionCode` and `versionName` are derived from these.
-   **ABI Splits:** The build produces separate APKs for `armeabi-v7a`, `arm64-v8a`, and `x86_64` to reduce size.
    -   **Version Code Schema:** `baseVersionCode * 10 + abiCode` (1=armv7, 2=arm64, 3=x86).
-   **Signing:**
    -   **Debug:** Uses a committed `debug.keystore`.
    -   **Release:** Requires environment variables (`RELEASE_KEYSTORE`, `RELEASE_KEY_PASSWORD`, etc.) or falls back to debug signing for local testing.

### GitHub Actions CI/CD (`.github/workflows/`)
-   **Release Workflow (`release.yml`):**
    -   Triggered by tags matching `v*`.
    -   Verifies that the git tag matches the version in `build.gradle`.
    -   Builds signed release APKs.
    -   Renames APKs to `CleverKeys-vX.Y.Z-<abi>.apk`.
    -   Generates a changelog from commit messages.
    -   Creates a GitHub Release and uploads assets.

## 3. Key Architectural Patterns

### Window Management & UI
-   **Edge-to-Edge:** The keyboard window uses `WRAP_CONTENT` height (fixed in `WindowLayoutUtils.kt`) to avoid "white bar" artifacts during animation.
-   **Transparency:** A custom theme `CleverKeysIMETheme` (in `styles.xml`) enforces transparency (`windowIsTranslucent`, `windowBackground=@null`) to ensure the system background doesn't bleed through.
-   **Layout Loading:** `LayoutManager` loads keyboard layouts from raw resources. Layouts must be present in `src/main/layouts/` and are copied to the build directory during compilation.

### Swipe Prediction
-   **ONNX Runtime:** Swipe prediction is handled by `com.microsoft.onnxruntime:onnxruntime-android`.
-   **Models:** Models (encoder/decoder) are loaded from assets or external storage.
-   **Privacy:** All inference happens strictly on-device.

## 4. Developer Quirks & Gotchas
-   **"White Bar" Artifact:** If the keyboard animation shows a white bar at the top, ensure `WindowLayoutUtils` sets height to `WRAP_CONTENT` and the Service theme is fully transparent.
-   **Resource Duplication:** **DO NOT** manually copy XML files to `res/raw`. The Gradle build task handles this. Manual copying causes "Duplicate resource" errors.
-   **F-Droid Compatibility:** The version code logic and split APK structure are designed to be compatible with F-Droid's build expectations.
-   **Termux Environment:** When running shell commands, always prefer `./build-on-termux.sh` over direct `./gradlew` calls to ensure the correct environment variables and AAPT2 overrides are applied.
-   **Dirty Worktrees:** Multiple sessions may share this checkout. Inspect `git status` and the diff before editing; never discard, overwrite, stage, or commit another session's changes.
-   **Release Authority:** Never create/push a tag, publish a release, or interact with external issues/MRs without explicit user authorization. Preparing and validating release files does not grant publication authority.
-   **Untrusted Imports:** Validate and bound archive/container input in a staging area before applying it. Enforce per-entry and aggregate decompressed limits, reject duplicates/path traversal, and roll back staged/live file changes on failure.
-   **Accessibility Geometry:** Custom-drawn keyboard accessibility nodes must use the same finite ownership partition as normal touch hit-testing; gaps and edge slop may not become TalkBack dead zones.

## 5. Documentation Map
-   `docs/ARCHITECTURE_MASTER.md`: High-level system design.
-   `docs/ONNX_DECODE_PIPELINE.md`: Deep dive into the swipe engine.
-   `docs/VERSIONING.md`: Explanation of the versioning scheme.
-   `memory/`: Context files for AI agents.

This file should be updated when significant infrastructure changes occur.


## Editing preferences in the Polish fork (trial v9)

New editing options share safe bounded reads in `readEditBehaviorPreferences`, product
constants in `Defaults`, and typed backup defaults/validation. `EditBehaviorOptions` is
immutable and captured by `ConfigSnapshot` at pointer-down. Never reread mutable edit
options during a Backspace gesture, or resurrect the deprecated undo checkbox readers.
Invalid tap-mode values fall back to character deletion. Keep group resets scoped and
resource-based controls mapped to their actual parent in the search generator.

Backspace v13 shares SliderMotion and captured space-slider settings with Space.
DRAG updates only on movement, using a signed Unicode character count per editor request.
There is no modern drag timer. Stationary WORD_PREVIEW/WORD_GAP retains v12 word deletion.
Once a drag starts, stopping the finger never restarts word deletion. Cancel and invalidate old
timer IDs at transitions. Word-repeat continuation must retain editor identity,
validated caret/text and synchronous callback capture across each commit. Pass ordered
(start <= end) Backspace selection ranges to editors; keep the fixed drag anchor only
in the session. Reversed endpoint requests can trigger state-driven editor resyncs.

See `docs/wiki/specs/settings/input-behavior-spec.md` for keys and field guards. Dictionary
add and editor/cursor/prediction consistency guards are unconditional fixes, not toggles.

## 6. Polish contextual SI preparation (2026-10-04)

Canonical spec: docs/specs/polish-context-ai.md. The maintainer-authorized opt-in
live trial uses HerbertLiveRuntime and the original verified HerbertOnnxScorer.
Geometric stays the decoder. The approved word-forms-v3 scope is at most two
already decoded Polish keys among the first five, and at most four surfaces.
Relate keys by identical Polish diacritic folding (not lemma evidence), or a shared
case-sensitive source lemma/POS from existing metadata. Source identities are parsed
once at import, immutable and bounded; never derive stems or parse JSON at gesture time.
Ordinary decoded forms do not need capitalization metadata. Add case variants only
when declared by source and enabled in the display setting. formGroupSize is separate
from exact-case policy; moving a word moves its score/language/case flag. Outside order
is preserved. No invented inflections or scanning the whole dictionary.
Family presentation also works without SI; disabling case variants leaves ordinary
form ranking available. Model/trust/default32/deadline350ms and editor/privacy guards
remain; no raw model-score mixing or retroactive edits.
Use a separate bounded live editor context preserving case/punctuation, never the
lowercase two-word PredictionContextTracker history. Preserve all source variants,
keys/scores/languages and the single SuggestionHandler presentation/commit pipeline.

Model export/scoring identity lives in producer experiments/herbert_mobile_v1.
ONNX Runtime Android and JVM are actually 1.21.1 in build.gradle (README is stale).
Require FP32/batched score parity, INT8 regression gate, exact fast-tokenizer
conformance and phone shadow measurements before opt-in live integration. Never
infer phone timing or add raw MLM scores to geometric scores without calibration.
No editor text logging/persistence or network fallback. Context, field eligibility,
request/revision/selection/pack/settings identity and deadlines gate future updates.


## FP32 benchmark follow-up

The failed INT8 preservation gate in herbert_mobile_v1 remains failed. The explicit
herbert_fp32_benchmark_v1 producer stage may package only the byte-identical verified FP32
for diagnostics. Portable Char-BPE tables are derived from original fast-tokenizer data,
including probed Unicode classifications, not Android Character categories. Real token vectors
and five-input feed equality are required in addition to native score/rank parity.

HerbertBenchmarkActivity uses prepared fixtures only and never enables live SI. Import requires
all seven file hashes from verified CI provenance; HerbertBenchmarkTrial now pins seven verified identities from producer run 37230171787.
Never construct trusted identities from an imported manifest. Keep all operations on its worker,
private noBackup staging, mapped model lifetime, cancellation between calls and deletion serialized.
No model/editor-text backup, logging/network or activation changes. PSS samples are whole process,
not exact peak/model memory; timing fixtures and host conversions are not independent quality tests.

Context window default is 32 words with a 64-word safety maximum and 4096 UTF-16 units.
The explicit benchmark compares both limits using alternating order and prepared examples,
not live editor history. Real JVM conformance is mandatory via compressed original test
fixtures; verify fixture hashes against compiled trust and never include them in APK.
APK workflow requires the exact real-conformance PASS marker plus existing lint/editor gates.
Other-locale missing trial-screen translations are deferred per maintainer, documented in
LOCALIZATION_BACKLOG_POLISH_AI.md, with a local-only MissingTranslation ignore on the new
resource file. Do not weaken global lint or claim JNI/mobile accuracy from host/pure tests.

CI runner tool inventory is not guaranteed: rg was absent in run 37231774451 after
real tokenizer tests passed. The mandatory original-conformance log marker is checked
with Python stdlib (already required for APK audit), not an uninstalled search utility.
Keep the assertion and all following gates; test/compile success alone is not APK success.


GitHub upload-artifact at compression-level 0 emits STORED stream entries with trailing
data descriptors: ZipInputStream rejects that envelope. FP32 importer now stages a bounded
private ZIP and uses ZipFile after bounding EOCD/index, preserving every compiled file hash.
Budget container plus extraction space; remove container before metadata parsing. Import UI
reports fixed reason codes and copyable Polish explanations, never raw throwable paths/URIs.


Phase-v2 diagnostics start with a fresh scorer and intended two-form requests BEFORE
mandatory conformance. Success still requires every token/feed/score/rank gate. Record
phase PSS outside timers, with forced boundaries and 250ms repeat throttle; first/last/max
are whole process, not isolated model memory or exact peak. Finally close then observe,
release mapped reference only after successful native close, never force GC/allocator tweaks.
Keep per-case timings, retained words/B/S/T and compiled code/base APK/model identity.


Nubia phase-v2 native report is archived in docs/eval/2026-10-05-herbert-fp32-nubia-phase-v2.md.
Code/APK/model hashes match CI; load dominates observed whole-process PSS
(346.2 baseline → 2121.0 loaded → 2132.1 workload max → 1336.8 immediate close).
Do not attribute the residual to a leak or the difference from v1 to optimization.
Next diagnostic should isolate mapped versus private-path session loading in separate
processes, bounded streaming SHA and phase/component readings; same immutable model,
threads/optimization/conformance gates. This experiment is planned, not implemented.
Default 32 helps latency but does not eliminate observed fixed load cost. Native
conformance is not independent semantic quality or a live production acceptance gate.

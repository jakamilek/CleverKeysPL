# CleverKeys Documentation - Table of Contents

Trial v15: [New-word suggestion viewport](wiki/specs/settings/input-behavior-spec.md#polish-trial-v15--new-word-suggestion-viewport).
Runtime now resets the strip at new swipe/typed-word boundaries; compile/tests and phone
validation pending. Ale/Lub correction is in a separate pack v4, not delivered by APK
updates: producer run 37188544526 passed; maintainer reports only updating the APK.

Trial v14: [Ordered Backspace editor ranges](wiki/specs/gestures/selection-delete-spec.md#polish-trial-v14--ordered-editor-selection-ranges).
V13 passed 3049 CI tests and both lint and was accepted generally on phone; SimpleX
selection freeze requires this separate compatibility trial and retest.

Historical v12: [Immediate Backspace drag and repeated word deletion](wiki/specs/gestures/selection-delete-spec.md#historical-polish-trial-v12--direct-drag-and-repeated-words). V12 passed CI and phone validation.

Trial v8: [Backspace brake and resume](wiki/specs/gestures/selection-delete-spec.md#polish-trial-v8--brake-and-resume). V7 accepted on phone; v8 adds a small reversal to pause, then deliberate motion to resume. Debug build and 3006 checks PASS (run 37154899155); phone checks pending.

Trial v7: [Backspace phone diagnostics](wiki/specs/gestures/selection-delete-spec.md#polish-trial-v7--phone-diagnostics-issue-unresolved). V6 phone retest failed; v7 debug build and 2999 checks PASS (run 37153176431); phone trace received and tested hold/drag/release accepted by maintainer.

Trial v6: Backspace editor-acknowledgement follow-up; debug build and 2992 CI checks PASS; subsequent v6 phone retest failed. V5 phone accepts tap/caps/Shift but rejects hold drag/release.

Trial v5: [Backspace hold and reversible selection](wiki/gestures/selection-delete.md), [technical changes](wiki/specs/gestures/selection-delete-spec.md). Debug build and 2985 CI checks PASS; v5 phone accepted tap/caps/Shift but rejected hold drag/release (2026-10-03).


**Last Updated**: 2026-08-15
**Review Status**: Files 251 of 251 (100% complete) ✅

## 📋 Quick Navigation

### Essential Documents
- **Primary Instructions**: `CLAUDE.md` - Main development workflow and commands
- **Project Status**: `README.md` - Production status and overview
- **Release Record**: `docs/RELEASE_RECORD.md` - Append-only book of every published release-note
  claim, anchored to live code + pinning test; guarded by `ReleaseRecordDriftTest` (hash-pinned
  history, completeness forced from the fastlane changelog dir)
- **Current Tasks**: `memory/todo.md` - Active todo list
- **History**: `docs/history/session_log_dec_2025.md` - Recent completed work

## 🗺️ Documentation Structure

### `/` Root Directory

#### Development Instructions
| File | Purpose | Status |
|------|---------|--------|
| `CLAUDE.md` | Main development guide | ✅ Active |
| `README.md` | Project overview | ✅ Active |
| `CONTRIBUTING.md` | Contribution guidelines | ✅ Active |
| `DEVELOPMENT.md` | Development setup | ✅ Active |

#### Build & Deployment
| File | Purpose | Status |
|------|---------|--------|
| `BUILD_SCRIPTS.md` | Build automation | ✅ Active |
| `DEPLOYMENT.md` | Deployment procedures | ✅ Active |
| `build-on-termux.sh` | Termux build script | ✅ Active |

#### Features & Issues
| File | Purpose | Status |
|------|---------|--------|
| `memory/todo.md` | **Active Task List** | ✅ Active |
| `docs/history/` | Historical logs and archives | 📚 Reference |

#### Model & Swipe Pipeline
| File | Purpose | Status |
|------|---------|--------|
| `docs/specs/ctc-swipe-engine.md` | CTC swipe engine spec | ✅ Active |
| `docs/specs/geometric-swipe-engine.md` | Geometric swipe engine spec | ✅ Active |
| `docs/history/neural-engine/` | The removed ONNX transformer engine (ADR-011) | 📚 Archived |
| `CLI_TEST_README.md` | CLI testing guide | ✅ Active |

#### Testing
| File | Purpose | Status |
|------|---------|--------|
| `MANUAL_TESTING_GUIDE.md` | Manual testing procedures | ✅ Active |
| `test-keyboard-automated.sh` | ADB testing script | ✅ Active |

### `/docs/specs/` Specifications
*Spec-driven development - All major systems documented*

| File | Purpose | Status |
|------|---------|--------|
| `README.md` | Master ToC for specs | ✅ Active |
| `SPEC_TEMPLATE.md` | Template for new specs | ✅ Active |
| `core-keyboard-system.md` | Core keyboard operations | ✅ Implemented |
| `gesture-system.md` | Gesture recognition | ✅ Implemented |
| `ctc-swipe-engine.md` | CTC swipe engine | ✅ Implemented |
| `layout-system.md` | Layout & extra keys | ✅ Implemented |
| `settings-system.md` | Settings & preferences | ✅ Implemented |
| `ui-material3-modernization.md` | Material 3 UI | ✅ Implemented |
| `performance-optimization.md` | Performance & monitoring | ✅ Complete |
| `testing-strategy.md` | Testing infrastructure (2050+ tests) | ✅ Active |
| `short-swipe-customization.md` | **NEW** Short Swipe System | ✅ Implemented |
| `profile_system_restoration.md` | **NEW** Profile Import/Export | ✅ Implemented |
| `geometric-swipe-engine.md` | Layout-agnostic geometric swipe decoder (standalone) | ✅ Implemented |
| `context-learning-and-next-word.md` | **NEW 2026-08-06** Persistent context LM, master learning privacy gate, opt-in next-word prediction, suggestion provenance, learned-data manager | ✅ Implemented |
| `ctc-swipe-engine.md` | **UPDATED 2026-08-15** CTC trie-beam swipe engine — WIRED opt-in `ctc` mode (2026-08-08): CleverKeys-trained ONNX encoder, router/adapter/settings/provenance As-Built | ✅ Implemented |
| `editor-spacing.md` | Field-aware swipe/tap spaces and punctuation; build/tests pass, device verification pending | 🚧 In progress |
| `cursor-word-capitalization.md` | Shift toggles a parked word's first letter; build/tests pass, device verification pending | 🚧 In progress |
| `cursor-aware-predictions.md` | Cursor sync + cursor-park next-word integration | ✅ Implemented |
| `architectural-decisions.md` | Architectural Decision Records | ✅ Active |

### `/docs/eval/` Decoder Evaluations (2026-07/08)
*CTC / geometric / FUTO-reference head-to-head evidence base*

| File | Purpose | Status |
|------|---------|--------|
| `2026-07-24-test2400-head2head.md` | Same-split 2,400-row head-to-head (transformer/geo/FUTO floor+ceiling) + fusion go/no-go; held-out VAL (9,918) corroboration; **2026-08-08 addendum: shipped CTC engine (89.31 t1) now tops the table** | ✅ Complete |
| `2026-07-23-futo100k-head2head.md` | FUTO 100k corpus head-to-head | ✅ Complete |
| `2026-08-06-offline-decoder-speedup.md` | Offline ONNX decode speedup investigation — verdict: adopt neither (XNNPACK 0.80× slower; decoder already int8-dynamic-quantized) | ✅ Complete |
| `2026-07-24-harness-conversion-audit.md` | Eval harness conversion fixes | ✅ Complete |
| `2026-08-28-arc019-ctc-local-head2head.md` | CTC vs geometric same-inputs head-to-head (90.7 vs 63.0 top-1); UT-5/UT-7 closure record | ✅ Complete |
| `2026-07-24-swipedata-onnx-validation.md` | Swipedata → ONNX input validation | ✅ Complete |
| `futo-decoder-eval-notes.md` | FUTO reference decoder porting notes (floor + Viterbi-beam ceiling) | ✅ Complete |

### `/docs/guides/` Guides

| File | Purpose | Status |
|------|---------|--------|
| `train-ctc-swipe-model.md` | End-to-end CTC swipe-model training → ONNX export guide (for a GPU box) | ✅ Active |

### `/docs/audit/` Live audit ledger

Only in-force audit records live here; everything superseded is under `docs/history/audits/`.

| File | Purpose | Status |
|------|---------|--------|
| `2026-08-23-v1.5-delta-audit.md` | Complete post-v1.5 P0–P3 change audit | ✅ Complete |
| `2026-08-23-v1.5-delta-evidence.md` | Commands, inventories, and evidence supporting the post-v1.5 audit | ✅ Complete |
| `2026-08-23-v1.5-delta-remediation.md` | Finding-by-finding fixes, validation, and remaining release evidence | 🚧 Release evidence pending |
| `2026-08-25-remediation-verification.md` | Verification of the remediation wave; residual findings CK-150-019..036 | 🚧 Residuals open |
| `2026-08-28-archive-verification.md` | Pre-archive verification of the July–August audit corpus; leaked-item ledger ARC-001..050 | 🚧 ARC items open |
| `2026-09-10-memory-oom-root-cause.md` | App switching, IME recreation, and heap exhaustion OOM root cause audit | ✅ Complete |

### `/docs/history/` History
| File | Purpose | Status |
|------|---------|--------|
| `session_log_dec_2025.md` | December 2025 Work Log | ✅ Archived |
| `neural-engine/` | The removed ONNX transformer engine: specs, decode pipeline, settings docs (ADR-011) | 📚 Archived |
| `audits/` | July–August 2026 audit corpus (15 audits + `remediation/` 1–6 + `remediation-plans/`), archived 2026-08-28 after line-by-line verification (`docs/audit/2026-08-28-archive-verification.md`). Notable: `2026-08-06-futo-decoder-integration-study.md` remains the CTC decode-port algorithm ground truth; `2026-08-06-futo-engine-integration-decision.md` §2 is the FUTO licensing analysis | 📚 Archived |
| `PRODUCTION_READY_NOV_16_2025.md` | Production readiness report | 📚 Reference |

## 🔄 Consolidation Status

**Verification**:
- Legacy `migrate/todo` directory has been cleared/consolidated ✅
- `memory/todo.md` is the single source of truth for active tasks ✅
- Specs are up-to-date with recent features (Short Swipes, Profiles) ✅

## 🎯 Spec-Driven Development Workflow

### Adding New Features
1. **Define Spec**: Create `docs/specs/feature-name.md` using template
2. **Update TOC**: Add to this TABLE_OF_CONTENTS.md
3. **Plan Tasks**: Add tasks to spec file's TODO section or `memory/todo.md`
4. **Implement**: Follow spec requirements
5. **Move History**: Completed work → `docs/history/`
6. **Update Status**: Mark complete in spec

### Session Startup Protocol
1. `cd ~/git/swype/cleverkeys`
2. Check: `cat memory/todo.md` (current tasks)
3. Check: `cat docs/TABLE_OF_CONTENTS.md` (navigation)
4. Check relevant spec: `cat docs/specs/[feature].md`

- [Editor prediction integrity — Polish trial regression](specs/editor-prediction-integrity.md)


## Polish fork editing settings trial

- [Input Behavior guide](wiki/settings/input-behavior.md#editing-controls-in-the-polish-trial-fork-v9)
- [Editing preference keys and guards](wiki/specs/settings/input-behavior-spec.md#configurable-editing-behavior-polish-fork-trial-v9)
- [Immutable settings architecture](wiki/specs/settings/settings-system-architecture-spec.md#immutable-editing-options-polish-fork-trial-v9)

V9 build and phone acceptance are pending; this is not a release.

### Polish Backspace trial v13 (2026-10-04)

Space and Backspace share motion-driven sliding: see [Selection Delete](wiki/gestures/selection-delete.md) and its [specification](wiki/specs/gestures/selection-delete-spec.md). Stationary word deletion remains as accepted in v12.

## Polish contextual SI trial

- [Polish contextual SI preparation](specs/polish-context-ai.md) — real ONNX scorer/feed, portable original tokenizer, trusted import and explicit FP32 benchmark screen; verified FP32/trust, mandatory real fixtures and 32/64 window benchmark; phase-v2 CI/native gates PASS; memory and independent quality pending, live IME unchanged.


- [Polish AI localization backlog](LOCALIZATION_BACKLOG_POLISH_AI.md) — base/Polish trial complete; other locales deferred per maintainer.

- [HerBERT FP32 phone trial](HERBERT_FP32_PHONE_TRIAL_V1.md) — verified CI APK/model links and Polish diagnostic steps; native phone v1/v2 reports archived.

- [HerBERT FP32 — pierwszy raport Nubii](eval/2026-10-05-herbert-fp32-nubia-phone-v1.md) — native conformance PASS; 32/64 timing and whole-process PSS; memory/quality follow-up remains.

- [HerBERT FP32 — pamięć etapami v2](HERBERT_FP32_PHASE_TRIAL_V2.md) — świeża sesja, dwie formy przed obowiązkową zgodnością, PSS etapów i czasy per kontekst; CI i natywna próba telefonu PASS.

- [HerBERT FP32 — wynik faz v2 na Nubii](eval/2026-10-05-herbert-fp32-nubia-phase-v2.md) — zgodne SHA i native PASS; koszt ładowania dominuje PSS, czasy 7/8/32/48 słów, protokół dalszej próby pamięci.

- [Mniejsze alternatywy polskiej SI](eval/2026-10-05-polish-ai-alternatives.md) — historyczny przegląd kandydatów; DistilRoBERTa później odrzucona przez bramkę tokenizera, bez modelu w aplikacji.

- [Zamrożone porównanie MLM 16/32](https://github.com/jakamilek/CleverKeys-langpack-pl/blob/769fc46579e910f50ff40f5546f0d5ae5643de5b/experiments/polish_mlm_compare_v1/PROTOCOL.md) — 384 zapytania na model; run 37355290211 zakończony FAILURE: HerBERT PASS, DistilRoBERTa nie rozpoznaje Ł w celu.

- [Wynik MLM v1 i diagnoza Ł](https://github.com/jakamilek/CleverKeys-langpack-pl/blob/9162d9a9f2ac3b94f4212a773cf9be80e1cb632a/experiments/polish_mlm_compare_v1_results/RESULTS.md) — odrzucony model, ukończona referencja i ograniczenia próby 16/32; bez nowego APK.

- [Tokenizer screening i MLM v2](https://github.com/jakamilek/CleverKeys-langpack-pl/blob/56b7d0f213fcdda1a8a5c245555bd3307fcc2133/experiments/polish_mlm_compare_v2/PROTOCOL.md) — Geotrend Distil/distilHerBERT, 122480 źródłowych form; run37359525824 trwa, kontrakt PR PASS; licencja distilHerBERT niewyjaśniona, bez APK.

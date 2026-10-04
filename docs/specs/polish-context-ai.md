# Feature specification: Polish contextual SI

**Priority:** P1. **Status:** preparation implemented; conversion/Android validation pending.
**Target version:** isolated trial, no release/version change. Created 2026-10-04.

## Overview and motivation

Choose the contextually appropriate capitalization of a source-confirmed ambiguous Polish word.
One dictionary key continues to own both surfaces. The user can always choose the alternate.
Geometric remains the decoder. Source metadata remains useful for permitted variants/defaults;
the latest text-description/instruction experiment did not establish a ranking benefit.

HerBERT plain, pinned revision 50e33e0567be0c0b313832314c586e3df0dc2297, is the provisional
candidate. New authored 32 contexts: 25/32 long versus 20/32 two-word, described+guided 24/32.
This is a diagnostic result for known keys, not independent production evidence.

## Functional requirements

1. First visible scope: order only the case pair of geometric rank 1. Keep both choices and all
   other keys, engine scores, languages, exact-case markers, source and swipe provenance.
2. Use live case/punctuation-preserving preceding editor text (<=64 words, <=4096 UTF-16 units).
   Read before swipe insertion. Existing PredictionContextTracker's two-word lowercase n-gram
   history is unchanged and is not the model context. If bounded capture may cut the first word,
   read a boundary lookbehind and drop the partial word; do not invent a leading token fragment.
3. Model inputs use whole-word masking of all target subwords simultaneously, original vocabulary
   log-softmax and mean log probability. No hand-authored descriptions or individual word rules.
4. Explicit Shift, Caps Lock, sentence capitalization and manually chosen forms retain priority.
5. Unknown/ineligible/no context/model/error/timeout means unchanged existing source/geometric order.
   Skip password, private, search, URL/email, selected-text and non-Polish inputs before reading text.
6. No result may update a previous editor session, revision, selection, request, language, pack,
   settings state or context. Never retroactively change committed text or an already selected slate.
7. All inference stays offline. No editor text in logs, persistent model caches, backups or telemetry.

## Components implemented in this trial

- ai/HerbertCasePolicy.kt: bounded context preserving case/punctuation; request identity; strict
  two-form ordering, stable ties, stale/ineligible/deadline fallback. Caller supplies field eligibility
  and identity; a live Android capture/dispatcher is still required, not assumed to exist.
- ai/HerbertPreparedBatch.kt: defensive, bounded WWM graph feeds. Right padding, exact positions,
  attended sequence and actual target mask; rejects unknown/out-of-range/empty target and bad budgets.
- ai/HerbertOnnxScorer.kt: actual local CPU ORT graph execution, hash-before-parse on copied bytes,
  input/output signature/dtype validation, finite aligned mean/sum outputs and deterministic cleanup.
  Worker-owned; synchronized score/close; borrowed global ORT environment is never closed here.

These are callable preparation components, **not a live IME feature**. No stub tokenizer, fake model,
active settings toggle, inferred timing threshold or production UI is added. Existing behavior remains.

## Model/export dependency

Producer experiment/herbert-mobile-v1 starts from results c4cf02a52c9c71fa1a8c3d7d6e66091f696bd07b.
See experiments/herbert_mobile_v1/PROTOCOL.md in CleverKeys-langpack-pl for exact graph, dependencies,
pre-registered gates, artifact layout and source attribution. The actual build.gradle dependencies
are Android and JVM ORT 1.21.1 (README's 1.20.0 is stale).

FP32 and batched eager Torch require abs score error <=0.001 and zero full-rank changes relative to
archived plain scores. INT8 must have zero top-1/top-3 labelled regressions in every suite/window/
population. All paired changes retained. Passing conversion is not a phone performance gate.

Candidate bundle (only after both gates): model.onnx, exact tokenizer.json, manifest/hashes,
CC BY 4.0 NOTICE, real-token Android feed/score vectors and fast-tokenizer conformance vectors.
No model weights in APK or Git at this stage. Expected SHA must be tied to verified trial/release
identity; an arbitrary untrusted imported manifest does not establish trust for the graph.

## Remaining implementation plan

1. Inspect CI conversion results. Implement real HerbertTokenizerFast-equivalent Android tokenization
   or a supported maintained library. HerBERT is CharBPE with case/accent-preserving BertNormalizer
   and punctuation-splitting BertPreTokenizer, not generic WordPiece/byte BPE. Require exact exported
   conformance vectors including controls/Unicode/added tokens. No guessed approximation.
2. Validated model import/staging, aggregate/member size bounds, duplicates/path checks, trusted hash,
   attribution, removal and lifecycle. Model storage separate from existing CTC model.bin contract.
3. Debug playground shadow benchmark: actual end-to-end tokenization/load/inference/heap and phone
   score parity. No visible reordering first. Nubia Z60 Ultra LV 12/512 confirmed; OS/SoC unknown.
4. Single latest-request background dispatcher with explicit cancellation/timeout, shutdown handling
   and main-thread identity recheck. Cancellation must not close an in-use session/environment.
5. Independent real contexts and broader slates, then opt-in case ordering. Full key reranking needs
   separate calibration with geometric scores; raw mean probabilities must not be added to 0..1000.

## Integration point

Current geometric completion: InputCoordinator.performGeometricSwipeTyping checks captured editor
identity then handlePredictionResults delegates to SuggestionHandler.handleSwipePredictionResults.
SuggestionHandler retains the single presentation/commit pipeline. Source pair expansion lives in
SwipeSurfaceVariants.expand after existing context rescoring. Integrate here after source lookup,
without creating a second commit path or late callback restoring stale slates. Model result may
choose source pair order before publishing/auto-inserting; a late result must fall back silently.
Capture editor state/context at dispatch, not on a worker after insertion. Preserve all prior editor,
spacing, scroll, dictionary-add and backspace regressions.

## Future Polish settings and user experience

- "SI w podpowiedziach": opt-in, default off; disabled with clear status if no validated model.
- "Tryb SI": "Pomiar bez zmiany podpowiedzi" first; "Kolejność wariantów pisowni" after phone gates.
- "Model SI": import/remove, verified model identity/size and attribution; no bundled huge weights.
- "Kontekst SI": initially fixed proven window, configurable only after meaningful validation.
- "Czas oczekiwania na SI": choose bounds after actual phone measurement, never guess latency.

Every eventual preference must cover defaults, ranges, search, import/export, scoped reset and backup.
Back up preferences only, never model files or editor text. Polish/base strings first; other locales
recorded for later per maintainer rule. No settings are implemented in this preparatory stage.

## Testing and success criteria

Registered pure JVM tests: variant completeness, stable ties, every stale-identity component, deadline,
privacy/capitalization fallback, invalid scores/forms, context case/word/unit/Unicode bounds,
padding/alignment, immutable feed and invalid/boundary token budgets. Android compilation/test/lint CI
required; no local Android/Kotlin toolchain is installed. Real ONNX output/tokenizer tests depend on
the producer's model bundle and remain pending. Passing synthetic pure tests does not establish JNI,
model parity, Android editor behavior or performance.

Future device acceptance: lower/upper correct or selectable within top 3; no lost candidates/text,
no unexpected capitals in ordinary sentences, no late updates after cut/paste/selection/field switch,
no UI stalls under repeated gestures, no private context capture. Report startup and p50/p95/RAM/
energy with model and phone identifiers. Conversion evaluation is not an independent accuracy test.

## Deferred extensions

Punctuation is a separate future task/head; model adapter API can be extended after measured evidence.
CTC replacement/training is unnecessary here. Three requested stationary Backspace timing settings
remain on backlog and must preserve accepted Space-like drag behavior.

No merge/release/tag/version bump. Actions monitoring <=60 seconds TOTAL/build then maintainer reports
completion. Previous results/protocols immutable. Significant stages checkpointed in both repo mains,
documentation only. Gemini/PAL requirement waived; no subagents without explicit instruction.

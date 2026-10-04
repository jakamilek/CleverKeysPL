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
2. Use live case/punctuation-preserving preceding editor text (<=32 words by default (64 maximum), <=4096 UTF-16 units).
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

## Verified FP32 package and phone benchmark trial (2026-10-04)

Producer run 37230171787 at d831e17b6cb99590d6ba036e92a72b6c3fd0cc7c succeeded.
Byte-identical FP32: 651798883 bytes, SHA256
f851436ba9ca35d0c7313ff873cd869b744b295a8a16fc95b4571d5e11b299f2.
All 232 archived rankings unchanged, <=0.001 score drift; independent portable reference
matches original fast tokenizer on 2471 saved vectors and 4352 exhaustive scalar blocks.
Metadata ZIP 11312569320/hash and every non-model member checked locally. Full FP32 comparison
recomputed exactly from saved score vectors. Model ZIP 11312693984 stays external (expires Oct 18);
its complete download was not locally verified. CI asserted model identity before upload.

HerbertBenchmarkTrial now pins all seven SHA/byte identities including the manifest itself
(667bd4fee413a13ca8edca75f5b7defff2c58d0450d8d87ab8e9ccef948026b0, 1243 bytes).
Import authenticates against these compiled constants, never a downloaded manifest's own hashes.
Private noBackup staging rejects unknown/duplicate/unsafe names, excessive bytes, invalid UTF-8/JSON,
missing files and wrong checksums. Failure/cancel removes staging. A mapped read-only model file
avoids giant Java byte-array copies; owner never edits it. One worker serializes native session
lifetime and staging replacement/deletion. Interrupted process staging is cleared on reopening.

HerbertTokenizer uses original Char-BPE tables, ranked leftmost linked merges, whole-target masking
and probed Unicode ranges rather than Android categories. No persistent editor-string cache.
Context DEFAULT_WORDS=32, MAX_WORDS=64, MAX_UNITS=4096; retain/prepare accept a bounded word limit.
Future IME context comes from before-cursor text in the current field, preserving original case and
punctuation, not an accumulated typing history. No live dispatcher/editor reading is implemented yet.
Archived contexts contain <=13 words, so parity cannot establish relative 32/64 accuracy.

JVM test resources include deterministic-gzip original portable tables plus original token/score vectors.
HerbertRealConformanceTest now always runs and validates byte size/SHA then all 2471 token examples
and all five feeds across 232 requests/532 candidates. No Assume-based skip. Test fixtures and model
weights are excluded from APK. Actual Kotlin real conformance in the new run is still pending.

Previous runtime 37230173951 (63524ecd): actual Kotlin compile, JUnitCore OK(2834) and 83 focused tests
passed. The real-fixture test was an assumption skip, so do not count it as real conformance success.
Debug lint failed solely on 19 MissingTranslation issues for the new base/Polish-only screen;
release-vital lint/assembly were skipped. Missing other locales are documented in
LOCALIZATION_BACKLOG_POLISH_AI.md; only this experimental resource file has a local ignore.
Global lint gates remain intact. New runtime run must pass real fixtures/debug+vital lint and assembly.

HerbertBenchmarkActivity is non-exported and entered from Swipe Playground via Test polskiej SI.
Import/remove/cancel/license/report controls use Polish/base strings; no SharedPreferences/live SI.
Only prepared synthetic/archived contexts are read. Import now enabled with pinned trust. Diagnostics
check real token/feed/native mean+sum parity and unchanged score rankings, then measure two-form feeds
for three context lengths at each word limit (32/64). Three warmups per context/limit, 30 timed repeats;
window order alternates, 90 samples/window, 180 total. Separate token/feed, inference and total p50/p95
are reported per window as well as pooled measurements. This is speed comparison, not accuracy.

Report contains phone/Android/ABI, model load including rehash, conformance, latency and sampled
maximum whole-process PSS. Samples taken between calls at least 250 ms apart are not exact peak or
model-only memory and not an energy/thermal result. Clipboard report contains metrics/device identity
only. Background/close cancels between native calls; native calls are not interrupted by main-thread
session close. Imported model is removed on screen destruction, never backed up or bundled in APK.

Workflow assembles ARM64 debug trial only after mandatory test/lint gates and inspects its ZIP for
FP32 weights/test fixtures. APK artifact includes SHA256/identity JSON, retains existing app version,
no release or merge. Next: inspect completed new CI, install diagnostic APK on Nubia, import model ZIP
and run repeated metrics. Actual Android JNI, phone performance and independent prediction quality
remain unverified. Live IME remains unchanged and requires later opt-in dispatcher/settings gates.

## Original Kotlin conformance verified; CI tool fix (2026-10-04)

Run 37231774451 at 90615f0c7655ea5597c45db963e40480bfd03484:
actual Android Kotlin compilation PASS, JUnitCore OK(2835), mandatory original
conformance PASS: 2471 token vectors and all five feeds across 232 batches/532 candidates.
The step failed only after Gradle success: workflow's marker search used rg, absent
on this runner (exit 127). Focused regressions, lint, assembly/upload were skipped;
no APK/JNI or phone result from this run. This does not invalidate tokenizer evidence.

Replace that extra marker check with already-used Python standard library, retaining
its exact assertion and all other required gates. Do not remove/skip the conformance check.
The new build must complete editor regressions/debug+vital lint/assembly before APK upload.
No Kotlin/fixtures/weights/context settings changes in this repair. Native scores and
phone timing remain pending; do not report APK success from compile/unit success.

## Diagnostic APK available (2026-10-04)

Run 37232458354 at 73627fae758bcdf845ed8696be821b7052e326ff SUCCESS:
actual compile, JUnitCore OK(2835), mandatory 2471-token/232-batch/532-candidate
five-input original conformance, 83 focused regressions, debug/vital lint,
assembly, absent-FP32/test-fixture ZIP audit and ARM64 artifact upload all PASS.
APK artifact 11314463703, raw APK SHA256
70c3e0e94f10e0a6e7ec4f9b6e043767cd9e41e7e21f14f21586958b07e07958.

See docs/HERBERT_FP32_PHONE_TRIAL_V1.md for stable artifact links, hashes and Polish
phone steps. APK raw bytes were not locally downloaded/verified (direct URL HTTP403;
authorized download_file supports <=32 MiB). CI logs/metadata establish publisher
identity and APK audit; no local malware-scan claim. Model ZIP is separate, with
seven compiled file hashes checked again by the importer.

Next is device native-score/rank parity and repeated 32/64 timing/PSS on Nubia.
No live integration/editor context dispatch/accuracy or phone performance claim yet.

# Dictionary and Multi-Language System

## Compact source views v11 — 2026-10-13

Current v11 closure (2026-10-13): runtime0b94e323 live38437360577 and CI38437368829
SUCCESS (2931 pure tests, 51 import cases, editor suites, original HerBERT
conformance, 192MiB gate, debug/vital lint and APK audit). Producer de587268
runs38437507820/38437516020 SUCCESS, all older regressions also SUCCESS.
Delivered APK SHA256 af22cd26bbd2dc943d01286672c7caa19df5d42ec10f5364e691d1cb022696eb;
inner ZIP8169a8ac as pinned below. Maintainer reports “działa dobrze” after delivery.
This is general phone acceptance, not separate PSS/timing or every Manager path.
Maintainer then held additional rare-names v12 and retained v11. Next work
prepares candidate-only ordinary/digital/apps source modules, not a new langpack
or APK. Existing words/metadata, all import bounds and private-source exclusion remain.

Phone: v9 and additive v10 are accepted. V10 contains204,463 words and114,299
metadata entries; the maintainer reports it works very well. V11 changes only
representation and snapshot reuse. CKDT bytes/ranks, unigrams, capitalization and
complete expanded per-word source JSON remain identical. No words are added or
removed. Native PSS remains unmeasured; perceived v9 speed is a user observation.

API/schema3 uses the same streamed source tables and strict existing limits.
A lexical reading may replace literal `surfaces` with integer `surfaceMask`: bit1
is the exact lowercase surfaceKey, bit2 is ROOT-uppercase first BMP code point plus
the remaining key, bit4 is ROOT-uppercase entire key. Selected forms are distinct
and sorted. Masks1..7 are valid; conflicting literal surfaces, nonintegral/bool/
string/out-of-range masks, surrogate keys and selected surfaces longer than96
UTF-16 units refuse. Literal arrays remain supported. The producer emits a mask
only if decoding reproduces the original ordered list exactly; it otherwise keeps
literal spelling. This storage field never invents a case variant or changes SI.
Mask-named opaque fields in schema2 inline metadata remain opaque. API must equal
schema; schema1/2 remain supported. Old APKs refuse API3; update APK before ZIP.

All195,288 actual v10 source-reading lists can be represented exactly. Existing
29,320 source definitions and242 attribute objects retain their exact prefix and
indexes. Append1354 profitable exact headers;4512 readings use their references.
`SharedSources.reusePrefix` shares only validated equal prefix objects, never a
mutated/reordered/shortened prefix. Every bounded entry is fully validated and
expanded before semantic comparison. Equal old immutable entries may be reused
across encoding/schema changes; their own backing source pool stays immutable.
Changed source labels/POS/case/field presence cannot reuse old values. No complete
expanded dictionary, compatibility ancestry chain or GC/largeHeap is retained.

On upgrade, reused entries can retain the older encoded backing for that process.
Cold restart reads the new compact backing. Both have identical public metadata
views and typed lemma/POS sets. Do not promise immediate heap reduction after a
hot import. `metadataJson` remains an on-demand one-word view; live SI reads the
unchanged typed relation and capitalization data.

V11:28,596,173 bytes /1,801,337 nodes; saves1,791,330 bytes /198,662 nodes versus
v10. Remaining198,663 nodes and5701 entry slots below unchanged32MiB/2M/120K guards.
Local84 producer and34 actual parser tests PASS. Actual204,463 CKDT records and
114,299 cold views/typed source identities, semantic upgrade entry identity and
surface presentation PASS. Cold/upgrade/repeat +16MiB ballast at192MiB PASS.
Existing schema1 replacement192MiB gate also passed. Current Android/import51/
mock/editor/original HerBERT conformance/lint/APK CI passed, as identified above.
New import cases cover schema2→3, warm, malformed masks and typed OOM rollback.
The phone trial is accepted generally; independent timing/PSS and individual
Manager lifecycle regressions remain unmeasured. No model,
gestures, defaults, bounds, app-version, merge/tag/release changes. Actions<=60s/run.

## Shared Polish source metadata v9 — 2026-10-11

The maintainer accepted the v7 and v8 imports and swipe recognition on the phone.
V8 has 190,813 keys, 100,649 metadata entries and 1,999,993 JSON nodes. V9 changes
only representation: exact reconstruction of all v8 metadata, unchanged CKDT bytes,
ranks, unigrams, capitalization and source evidence. No words are added or removed.

The manifest declares apiVersion=2 and languageIntelligence.schemaVersion=2;
API/schema versions must agree. Legacy schema-v1 still loads. Old APKs refuse v9;
install the compatible APK first. V2 root order is schemaVersion, languageCode,
sourceAttributes, sourceLexemes, provenance, entries. Attributes precede source
records and both tables precede entries; schema-v1 keeps its arbitrary header order.
Tables are streamed as individually bounded records, not retained Gson arrays.
References are exact integer table indexes; missing/empty fields, lemma suffixes,
case, POS, source NAME classes and LABELS remain distinct. Only repetitions that
save nodes are shared; unique/inefficient records stay inline.

`SharedSources` is immutable. Each word retains its encoded JSON and typed immutable
lemma/POS relation set. `metadataJson` expands only one requested word on demand,
without caching the full expanded dictionary. Existing SI and surface presentation
use the same typed relations/case information. Equal validated tables and encoded
entries may be shared with a previous v2 snapshot; v1 remains usable during upgrade.
Do not iterate metadataJson into a retained dictionary of expanded strings.

All encoded byte/hash/depth/string/duplicate/global-node guards remain. The combined
source tables have at most 120K records; word entries retain their separate 120K
limit. Expanded per-word complexity is checked before allocating copies (16,384
nodes). Invalid references, conflicting fields, duplicate definitions, late tables,
wrong API/schema/language and trailing data refuse before staged publication.
Import and background warming pass the declared schema. Rollback retains files and
provider, including typed OOM refusal; no forced GC, largeHeap or raised bounds.

V9 metadata: 26,141,080 bytes / 1,740,933 nodes, freeing 259,060 nodes versus v8;
259,067 remain below the existing 2M guard. There are 29,320 source records, 108,279
reading references, 61,869 inline readings, 242 attribute objects and 52,105 attribute
references. Uncompressed metadata saves 4,315,476 bytes; ZIP compression can be
slightly larger. These savings do not predict a fixed number of additional words
and do not reduce HerBERT weights or prove Android PSS savings.

Local actual-source checks: 28 parser tests; complete 190,813-key CKDT comparison and
100,649 byte-exact expanded metadata/typed-source views; actual surface-choice checks;
isolated cold v9, v8-to-v9 and repeated-v9 loads with 16 MiB ballast at 192 MiB PASS.
Producer: 71 tests PASS. New MockK cases cover v1 upgrade, v2 warm, bad references,
schema mismatch and OOM rollback. Current-head Android compile, integration, lint,
verified CI APK/ZIP delivery and phone import/PSS remain pending. Actions monitoring
is at most 60 seconds total per run; no merge, tag, release or application version bump.

## Polish thematic v7 import — 2026-10-11

The maintainer sees “Nieznany błąd” importing the verified v7 ZIP (188,302 keys;
98,138 metadata entries). The old importer explicitly mapped metadata OOM to
UNKNOWN. Retaining two independent full v6/v7 providers fails the host 192 MiB gate;
without phone logs this is a reproduced mechanism, not proof of the sole phone cause.

Replacement parsing now accepts the cached previous provider of the same language.
It validates each complete bounded entry before comparing every immutable data field
and shares only exact-equal entries. Changed/new entries are allocated separately;
removed entries are absent from the new index, and version/capabilities/provenance
come from the new pack. Final headers, unknown fields, duplicates, byte/hash/node
bounds and staged rollback remain strict. No existing dictionary/SI/editor state is
modified on refusal. Metadata OOM now renders a dedicated Polish/base RAM explanation;
other locales fall back to English and are tracked for later translation.

Local actual-source checks: 23 Kotlin parser tests PASS; full CKDT/metadata consumer
comparison preserves all181,691 old records with188,302 total and3964 source relations.
The exact v6/v7 replacement plus16 MiB ballast passes in an isolated192 MiB JVM.
CI's generated workload now also replaces91,527 with98,138 entries (29,112,009 bytes)
at the same192 MiB ceiling; exact old entries are shared and additions verified.
Host heap checks are not phone PSS. Native Android compile, importer MockK rollback,
Polish resource/other-locale fallback, lint and phone re-import remain required.
The producer ZIP is unchanged; install the new APK before retrying it.

## Polish additive common-form trial v6 — 2026-10-11

The separate producer trial appends 75,328 source-attested ordinary forms to the
verified v5 dictionary (106,363 → 181,691 keys). Every old canonical record/rank,
case variant, metadata entry and unigram is retained. It is an opt-in imported
langpack, not a bundled dictionary or model update. Import the inner langpack ZIP
after installing a compatible APK. The first v6-compatible APK passed CI but the
maintainer reported that selecting the ZIP closes the app; it is superseded by
the streaming-import fix below. No dictionary rebuild is needed.

`IntelligenceJson` permits at most 2,000,000 nodes (formerly 1,000,000). The v6
sidecar has 1,844,963 nodes, 91,527 entries and 28,123,935 UTF-8 bytes. The 32 MiB
byte, 120,000 entry, depth-32, string, duplicate-member and 32-source-identity
bounds remain unchanged. Compact ordinary entries may omit canonicalForm and
capitalization: CKDT provides their spelling. They retain immutable lexical
lemma/POS/surface evidence for the existing SI relation test; no suffix guessing,
new model scope or new gesture-time parsing. Existing metadata is untouched.

Validation: runtime runs 38121260589/38121262142 and producer runs
38121444186/38121445914 passed at the original v6 heads. The verified APK was
delivered, but phone import failed. The original actual Kotlin consumer reproduces
`OutOfMemoryError` with a 256 MiB JVM heap while retaining the v5 snapshot; its
earlier 1 GiB check hid this allocation problem. No phone crash trace is available,
so this is a reproduced cause, not proof of the phone's only failure mode.

The replacement `IntelligenceJson.parse` reads one entry tree at a time and builds
the immutable typed index directly. It preserves metadata JSON, capitalization
and case-sensitive source lemma/POS relations. One shared counter still bounds the
complete document at 2M nodes; per-entry and non-entry root values additionally
have a 16,384-node chunk limit (the largest actual v6 entry has 470 nodes), with at
most 64 root members. Duplicate fields, depth, strings, 120K entries, schema,
language and trailing content remain strictly checked, including unknown fields.
Header order is arbitrary; no provider is published until the entire stream passes.
Byte/hash/ZIP extraction limits and staged replacement remain unchanged.

Settings dispatches ZIP import to `Dispatchers.IO`; main handles only its result.
The initial repair returned the existing localized generic IO failure on a metadata allocation failure
before the staged swap and retains the installed files/provider. Background warming
also contains this specific allocation failure. This is not a promise to recover
every possible process-level memory failure; no largeHeap, forced GC, weakened
integrity bounds or dropped dictionary data is introduced.

Local checks after the fix: 21 actual Kotlin JUnit tests PASS; full v5/v6 CKDT and
metadata comparison PASS at 256 MiB (106,363 old records preserved, 181,691 total,
50,686 shared-source relations). The isolated memory gate passes at 192 MiB with
both providers and 16 MiB live ballast, using both the exact v5/v6 packs and a
generated 91,527-entry / 32,001,501-byte sidecar. The same workload fails at 128 MiB;
192 MiB is an explicit regression ceiling, not an Android PSS/heap measurement.
The Gradle gate runs separately from the broad suite in CI before the trial APK.
OOM rollback and import-dispatch regressions join the existing integration/pure
suites. Fix runs 38124097991/38124100497 passed at `6ecb5238`: 2912 pure tests,
192 MiB gate, import/editor integration and both lint tiers. Delivered APK SHA-256
`5e5caedf2ed19069c3a845ead1b36f73be1ceb730cc3cbd405a07206ace801a4`.
The maintainer confirms import works and the formerly missing swipe words are
available. Native memory/latency and broader swipe ranking still require measurement;
coverage alone is not Top3 accuracy evidence.
Private dictionary exports never enter the producer or git.

Producer protocol: `jakamilek/CleverKeys-langpack-pl`,
`docs/common-forms-trial-v1/PROTOCOL.md`, branch `feature/additive-common-forms-v1`.

## Dictionary Manager loading after v6 — 2026-10-11

After successful import the maintainer reports slow opening of Dictionary Manager.
Its `MainDictionarySource.extractWordsFromIndex` called `getWordsWithPrefix` for
every already-normalized exact key, then searched those completions for that key.
Each call scans the shared short-prefix bucket, allocates results and sorts them;
doing this per row needlessly multiplies work with a larger vocabulary.

Use `NormalizedPrefixIndex.lookup(key)` instead. Preserve the identical canonical
winner, rank-to-frequency formula, normalized/canonical disabled checks, source,
all rows and existing cache/search rules. The UI's pre-existing one-row-per-folded
key policy still displays 170,588 rows for 181,691 canonical v6 records; this fix
does not prune or modify the dictionary or add a display limit.

`WordListFragment.filter` prepares the enabled subset and existing four sort modes
on `Dispatchers.Default`, then publishes the adapter/count/scroll state on main.
Use the current view lifecycle and ignore dispatches without a view. A superseding
query or destroyed view cancels publication, preserving the existing recreation
and search-state guards. Dictionary file IO remains on IO.

Local actual Kotlin check at 256 MiB uses the real binary loader/index and extracted
old/new row bodies on the exact v6 ZIP: all 170,588 rows, ranks and disabled flags
equal. One desktop extraction sample was 1306.3 ms before and 82.8 ms after; this
isolates row extraction and is not phone opening latency or an Android UI benchmark.
CI adds actual source-extractor parity/rank/shared-prefix tests and a worker/view
wiring guard, retaining the existing sort/recreation suites. Runs38126517112 and
38126518539 passed at75123b05 (2913 pure tests, row integration, 192 MiB metadata
gate, both lint tiers). APK SHA-256 fec89536d07f1a200cff1d47a3fb603cf25f012e2edebba879c03457d6b144cd
was delivered. The maintainer reports continued slow loading and a close when
switching through Disabled/User/Custom and back to Active before loading completes;
waiting for completion avoids it. This first repair is insufficient.

### Shared cold load and direct display projection follow-up

The previous cache shared completed results only: a superseding filter could cancel
its await while non-interruptible file work continued, then start another cold load.
It also built a full NormalizedPrefixIndex solely to discard it after row extraction,
and exposed instance words before the UI search index completed. There is no phone
crash trace, so duplicate allocations/lifecycle hazards are not a proven sole native
crash cause.

DictionaryDisplayLoadCache owns one Deferred per language, independent of callers'
view jobs. Publish words and the UI prefix index together as one completed snapshot.
A cancelled view stops awaiting, without cancelling or duplicating the shared task.
Real cache invalidation retires its identity before cancelling; runInterruptible and
loop interruption checks stop work without falling back to another dictionary.
Failed tasks are retired for retry. The source retains applicationContext only.
Cold nonblank searches await the same complete snapshot as blank loads.

The V2 binary traversal now feeds DictionaryDisplayRows directly, preserving the
first mapped canonical for each normalized key, ranks and disabled policy. It skips
retaining normalized strings that the old reader decoded but never used; the shared
traversal still checks their lengths. V1 retains its existing rank conversion.
The ordinary typing decoder and swipe dictionary are unchanged. No display cap,
pruning, forced GC, largeHeap or new import is introduced.

WordListFragment captures its source, tab, view identity and request generations;
only the still-current view/request may publish a list or error. Destroying the view
cancels its waiter and detaches its RecyclerView adapter. An uninitialized refresh
is ignored; activity destruction removes delayed search/count callbacks. Filtering
and sorting remain on Default, with adapter/count/restored-scroll publication on main.
Allocation failure in loading or list preparation surfaces the existing localized
error instead of caching an incomplete snapshot. Process-wide OOM recovery is not promised.

Local evidence: five actual shared-cache JUnit tests PASS (20 replacement waiters,
cancellation, invalidation/late completion, failure/OOM retry, language scope).
Four binary row tests and18 state/sort tests PASS with host stubs for Android types. Separate processes compare the
pre-change binary reader/index with the direct reader: full v6 plus seven bundled
binaries have identical ordered row/rank/source/disabled SHA-256 digests. v6 retains
170,588 display rows; digest fa3314e1f61b509cd8831e2e4eb56f2a66268dc9e5e027611b0e1827c5692973
for the recorded test disabled set. Actual MainDictionarySource body, real v5/v6
metadata, full v6 CKDT, UI prefix index and 20 shared callers pass together at192 MiB
with16 MiB ballast, using host context/pack-path stubs. This is a JVM heap regression
check, not phone PSS, UI execution or model memory. New CI adds a real source/binary
blocked-read cancellation + restored-search test; Android compilation, mock/pure/lint
and the phone loading/tab-switch/search/recreation retest remain pending. Same ZIP/SI.

At f166ad4, runs38129489460/38129492820 pass Android compilation,2918 pure tests,
original HerBERT conformance and the192 MiB metadata gate. The blocked-read source
and V1 tests pass; three V2 row parity tests stop at android.os.Trace.beginSection
with RuntimeException("Stub!") under android.jar. No APK or lint result was produced.
The follow-up mocks only Trace begin/end in the test fixture, matching existing mock
suites. Production tracing, binary readers and all assertions remain unchanged.
Fix runs38133738323/38133740554 at2f4735dd both SUCCESS: actual Android compilation,
2918 pure tests including original HerBERT conformance,192 MiB metadata gate, all five
Manager row/source tests, editor integration, debug/vital lint, dependency-lock and
APK audit. Delivered ARM64 APK SHA-256
e69041ae5441d88776f519d608b33a191d362ebd2564335faa9de47433548663 (35,860,188 bytes).
Artifact11700272825 SHA-256 b1655155e97f4eb363f475857ff04115126366e82cf0d0a418f58aec78f82bbf
matches downloaded bytes; code/APK identity and prior v2 signing certificate match.
This fixture repair and CI success are not phone validation. Early tab-switching,
opening time, search/sort/disabled coherence and recreation still need a phone retest.

2026-10-11 follow-up: maintainer confirms much faster loading, effectively the
pre-expansion opening speed. This accepts perceived load speed only; the earlier
pre-load tab-switch crash and search/sort/recreation remain separately unconfirmed.
Maintainer authorizes continuing the previously ordered additive vocabulary groups.
The next producer stage is thematic ordinary forms, preserving v6 words/ranks/source
metadata; brands/abbreviations/names and final private-export exact duplicates follow.

## Overview

The dictionary system manages word lookup, frequency ranking, and multi-language support through a tiered architecture. It combines static dictionaries, user-defined words, and the swipe decoders' lexicons into a "Language Pack" system with automatic language detection for bilingual typing.

## Key Files

| File | Class/Function | Purpose |
|------|----------------|---------|
| `src/main/kotlin/tribixbite/cleverkeys/DictionaryManager.kt` | `DictionaryManager` | Per-language dictionary loading/coordination (`OptimizedVocabulary`/`OptimizedVocabularyImpl` were deleted with the neural engine, 2026-08-18 — ADR-011; the swipe engines' lexicons are now `swipe/ctc/CtcLexiconTrie`+`CtcCkdtLexicon`+`CtcLexiconMerge` and `GeometricEngineAdapter`'s merge) |
| `src/main/kotlin/tribixbite/cleverkeys/LanguageDetector.kt` | `LanguageDetector` | Word-based language detection |
| `src/main/kotlin/tribixbite/cleverkeys/WordPredictor.kt` | `WordPredictor` | Unified prediction pipeline |
| `assets/dictionaries/{lang}_enhanced.bin` | Binary dictionaries | Trie-based word storage |

## Architecture

### Language Pack Structure

Each language pack is a self-contained unit:

```
Language Pack ({lang})
├── dictionaries/{lang}_enhanced.bin    # Trie-based vocabulary
├── dictionaries/{lang}_unigrams.bin    # Top 1000 words for detection
├── models/ctc_swipe_encoder.onnx       # CTC emission encoder (one model, all languages)
├── layouts/{lang}_*.xml                # Keyboard layouts
└── metadata.json                       # Version, license info
```

### Dictionary Layers

```
┌─────────────────────────────────────────────────────────────┐
│        WordPredictor.predictInternal (inline merge)          │
│  All layers load into ONE dictionary map; candidates are     │
│  scored by calculateUnifiedScore (custom/user words are      │
│  calibrated onto the base scale — UserWordFrequency)         │
└─────────────────────────────────────────────────────────────┘
                            │
       ┌────────────────────┼────────────────────┐
       │                    │                    │
       ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Layer 1    │    │   Layer 2    │    │   Layer 3    │
│ Main Dict    │    │ User Dict    │    │ Custom Dict  │
│ (Read-Only)  │    │ (System)     │    │ (App-Local)  │
│              │    │              │    │              │
│ Language     │    │ Android      │    │ SharedPrefs  │
│ Pack Asset   │    │ UserDictionary│   │ user_dict    │
└──────────────┘    └──────────────┘    └──────────────┘
       │                    │                    │
       └────────────────────┼────────────────────┘
                            │
                            ▼
                   ┌──────────────┐
                   │   Layer 4    │
                   │ Disabled     │
                   │ Words Filter │
                   └──────────────┘
```

**Resolution Logic:**
1. Gather candidates from Layers 1, 2, 3
2. Filter out words in Layer 4 (disabled)
3. Score by frequency, source priority (Custom > User > Main), decoder confidence

## Implementation Details

### Accent Handling

The swipe decoders emit a 26-letter alphabet (a-z only). Accented words are handled through normalization:

```kotlin
// Accent mapping: normalized → canonical forms
data class AccentMapping(
    val normalized: String,         // "cafe"
    val canonicalForms: List<String>, // ["café"]
    val frequencies: List<Int>
)

// Lookup flow:
// 1. User swipes "café" → trajectory matches "cafe" pattern
// 2. Prefix lookup: "caf" → finds normalized candidates
// 3. Each candidate maps to its accented canonical form
```

### Binary Dictionary Format (v2)

```
┌────────────────────────────────────────┐
│ HEADER (32 bytes)                      │
│  - Magic: "CKDICT" (6 bytes)           │
│  - Version: 2 (2 bytes)                │
│  - Language: "es" (4 bytes)            │
│  - Word Count (4 bytes)                │
│  - Trie Offset (4 bytes)               │
│  - Metadata Offset (4 bytes)           │
│  - Accent Map Offset (4 bytes)         │
├────────────────────────────────────────┤
│ TRIE DATA BLOCK                        │
│  - Compact trie of NORMALIZED words    │
│  - Terminal nodes store word_id        │
├────────────────────────────────────────┤
│ WORD METADATA BLOCK                    │
│  - Array indexed by word_id:           │
│    - Canonical string (UTF-8, varint)  │
│    - Frequency rank (UInt8, 0-255)     │
├────────────────────────────────────────┤
│ ACCENT MAP BLOCK (optional)            │
│  - normalized_word → [canonical_ids]   │
└────────────────────────────────────────┘
```

**Frequency Ranking:**
- Rank 0 = most frequent word
- Rank 255 = least frequent
- Log-scaled quantization preserves relative ordering

### Language Detection

Word-based unigram frequency model:

```kotlin
data class LanguageScore(
    val language: String,
    var score: Float = 0f,
    var consecutiveHits: Int = 0
)

// Detection algorithm:
// 1. Each language pack ships top 1000 unigrams
// 2. Maintain sliding window of last 5 committed words
// 3. Score each word against active language unigram lists
// 4. Track running score per language (exponentially decaying)

fun shouldSwitch(primary: LanguageScore, candidate: LanguageScore): Boolean {
    // Conservative threshold to prevent jitter
    return candidate.score > primary.score * 2.0f &&
           candidate.consecutiveHits >= 2
}
```

### Dual-Dictionary Mode

For bilingual typing (e.g., English + Spanish) without manual switching:

```kotlin
fun calculateUnifiedScore(
    word: String,
    nnConfidence: Float,      // From ONNX model
    dictionaryRank: Int,      // 0-255, lower = more common
    languageContext: Float,   // 0.0-1.0, from detector
    isPrimaryLang: Boolean
): Float {
    val rankScore = 1.0f - (dictionaryRank / 255f)
    val langMultiplier = if (isPrimaryLang) 1.0f else languageContext
    val secondaryPenalty = if (isPrimaryLang) 1.0f else 0.9f

    return nnConfidence * rankScore * langMultiplier * secondaryPenalty
}
```

**Deduplication:** When word exists in both dictionaries, present only entry with higher final score.

### Manual Language Switching

```kotlin
// Triggered by Globe key or long-press Spacebar
fun switchLanguage(newLang: String) {
    // 1. Hot-swap MainDictionarySource
    dictionaryManager.loadMainDictionary(newLang)

    // 2. Load corresponding ONNX models
    multiLanguageManager.loadModels(newLang)

    // 3. Update keyboard layout if linked
    keyboardManager.switchLayout(newLang)
}
```

### Data Structures

```kotlin
data class DictionaryWord(
    val canonical: String,      // Display form with accents
    val normalized: String,     // Lookup key without accents
    val frequencyRank: Int,     // 0-255, lower = more common
    val source: WordSource,     // MAIN, USER, CUSTOM, SECONDARY
    var enabled: Boolean = true
)

enum class WordSource {
    MAIN,       // Primary language pack
    SECONDARY,  // Secondary language pack
    USER,       // Android UserDictionary
    CUSTOM      // App SharedPreferences
}

data class LanguageState(
    val primary: String,
    val secondary: String?,
    val detectedContext: String,
    val confidence: Float
)
```

### Key Classes

| Class | Purpose |
|-------|---------|
| `DictionaryManager` | Singleton holding active WordPredictor instances, handles language lifecycle |
| `MultiLanguageManager` | Manages ONNX sessions, handles auto-detection logic |
| `WordPredictor` (inline) | Merges primary + secondary candidates in `predictInternal` via `calculateUnifiedScore` × `secondary_prediction_weight` (the standalone `SuggestionRanker` had zero production callers post-ADR-011 and was deleted 2026-09-06 — audit C-6) |
| `LanguageDetector` | Word/character-pattern language detection (the unigram-frequency `UnigramLanguageDetector` was deleted 2026-08-28 — ARC-006, write-only after `OptimizedVocabulary` went) |
| `AccentNormalizer` | Unicode normalization (NFD) + accent stripping |

### Dictionary lifecycle and memory

PredictionCoordinator owns the serving WordPredictor. DictionaryManager does not
cache additional predictors. The predictor has a primary dictionary/prefix index
and an optional secondary NormalizedPrefixIndex. A vocabulary bound limits each
index, but each remains substantial; see the measured EN+IT stages in
[`2026-09-10-memory-oom-root-cause.md`](../audit/2026-09-10-memory-oom-root-cause.md).

AsyncDictionaryLoader uses one process-wide worker and two replaceable request
slots per owner, primary and secondary. Replacing or cancelling a request removes
its queued task and main-handler callbacks. Token checks also reject callbacks
already dequeued before cancellation. Secondary disable invalidates its slot
without cancelling primary loading. A synchronous primary reload invalidates the
pending asynchronous primary request before replacing its maps.

Primary dictionary, prefix index, casing, custom-word, shadowed-frequency and
contraction metadata build privately. The current request publishes them on main;
obsolete work cannot modify the serving language's metadata. Secondary indexes
also build privately and publish on main. Binary and overlay loops cooperate
with interruption, and expected cancellation does not trigger fallback loading.
Individual I/O/native calls are not guaranteed to stop immediately.

PredictionCoordinator.shutdown checkpoints learning, then calls the predictor's
terminal shutdown. This cancels both load slots, stops dictionary observation,
and replaces large dictionary references with empty state. It never shuts down
the shared executor or clears a worker's privately owned maps. Production lifecycle
and publication calls run on the main thread. Later load/observation requests on
the retired predictor are ignored.

Swipe dictionaries are separately owned by their adapters, with bounded,
content-versioned language memos. InputCoordinator detaches those adapters during
teardown. CtcEngineAdapter closes its ONNX sessions only after the worker has
terminated; its existing 250 ms timeout avoids closing a session beneath native
inference. See [`ctc-swipe-engine.md`](ctc-swipe-engine.md) for that contract.

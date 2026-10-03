# Android integration proposal — source casing variants v1

Status: stage 1 runtime implementation in draft PR; validation tracked below.
Baseline: CleverKeysPL `0da41d8951f96b9dab4e9fb219da5e4924aa66b6`.
Contract: LANGUAGE_INTELLIGENCE_API_V1_FINAL_2026-10-02.md.

## Fork-specific consultation decision

On 2026-10-03 the maintainer explicitly waived the inherited Gemini 3 Pro / PAL
requirement. The fork override is recorded in memory/REPO_INSTRUCTIONS.md.
Implementation proceeds with code review and pure/mock tests.

## Concrete integration scope

1. `langpack/LanguagePackManager.kt`: parse the API/capability/member declaration,
   bound extraction of the intelligence member, check filename/schema/SHA,
   validate before installation and copy it to the staged pack. Preserve legacy
   packages and reject unsupported API majors explicitly. Do not change model
   import or CTC routing. Preserve existing import errors/localization behavior.
2. New pure parser and immutable data model under `langpack/`: unique
   lowercase keys preserving diacritics, one/two capitalization forms for this
   trial, matching defaults/case policies, language agreement and bounded input.
   Read the capitalization fields; tolerate optional metadata/provenance, retaining
   its installed source file. Future wider inventories must not silently fail a
   trial-specific parser: define supported case policies and limits explicitly.
3. New provider: load installed pack metadata away from the UI thread, publish an
   immutable snapshot keyed by language/pack generation, clear it on deletion or
   replacement, return no variants for absent metadata/legacy packages. Do not
   insert variants into the dictionary, prefix index or geometric ranker.
4. `SuggestionHandler.handleSwipePredictionResults`: expand only the top ranked
   lexical key after lexical rescoring, while constructing bar words/scores/metas.
   Use the candidate's language if available. Two display forms share the original
   lexical score; no artificial second decoder membership or score boost. The next
   distinct decoder key follows the pair. Preserve aligned language/score/meta lists,
   possessive gating, swipe provenance, correction tracking and next-word append.
5. `SuggestionMeta` : mark explicit surface choices
   so the commit path can preserve the exact selected spelling. The alternate is a
   replacement of the swipe token, never a NEXT_WORD append. Do not change all manual
   selections globally; constrain exact-case handling to these marked suggestions.
   Auto-insert and bar must agree on the selected primary surface.

## Proposed case behavior for review

Without Shift/autocap, prefer the pack's default, retain its alternate.
Existing user case preference may change ordering only if it matches a confirmed
variant. At sentence start or with Shift, put the capitalized form first and keep
the lowercase choice available. A manual variant tap preserves exactly that form.
Caps Lock remains explicit uppercase input; define and test its interaction with
the pair before coding rather than applying an unconditional uppercase map to both
forms and accidentally creating two identical suggestions. These case details are
proposed, not yet accepted or tested on Android.

## Acceptance scenarios

- Middle of sentence: top key łódź produces łódź/Łódź and the next distinct word;
  selecting Łódź replaces only the auto-inserted word, including correct space repair.
- Sentence start: primary Łódź, explicit lower-form tap commits łódź exactly.
- malina/Malina, warszawska/Warszawska; łódzki stays single; no metadata stays unchanged.
- Shift/Caps Lock and stored user casing do not collapse or override explicit choices.
- Pack replacement/deletion/language switch cannot expose stale or wrong-language forms.
- Legacy import succeeds; changed hashes, unsupported versions, duplicate keys/members,
  path traversal, excessive size and mismatched language are rejected before installation.
- Real InputConnection replacement handles terminal and ordinary editor paths; no
  stale asynchronous bar update restores an earlier slate.

Producer tests are not Android tests. A runtime commit must run the appropriate
pure/mock import and swipe-selection tests plus real IME checks. Existing Gradle
guard and device instructions remain mandatory.

Out of scope here: Łódźi→Łodzi morphology editing, full-pack source generation,
contextual AI, CTC changes, punctuation and learned case counts. The fixture already
includes the łodzi/Łodzi key, so later editing can reuse the variant provider.

## Implemented stage 1

The importer validates the declared API/schema/member/hash, unique JSON members,
language and lowercase keys, defaults and supported case policies. Extraction is
streamed with per-member limits (manifest 256 KiB, sidecar 32 MiB, other members
64 MiB; existing model limit preserved), a 128 MiB aggregate limit and 64 entries.
Paths and duplicate archive members are refused. A failed replacement rename
attempts restoration of the previous pack; the backup is retained if rollback
itself fails. This is an in-process rollback, not crash recovery.

An immutable provider retains opaque metadata/provenance for future consumers.
It publishes after import and warms installed packs on a background worker at
language activation, including secondary languages at the first lookup request.
Until a cold snapshot finishes loading the normal decoder slate is shown; no
asynchronous callback replaces the bar for an already completed swipe. Lookup on
the provider is memory-only. Legacy replacement and deletion invalidate the cache.

After existing lexical context rescoring only rank 1 is expanded. Both source
surfaces inherit its score and origin; the next distinct decoder key follows.
Sentence capitalization/Shift puts the capitalized form first and retains the
source lowercase choice. A single-form adjective remains single. Explicit Caps
Lock keeps the original all-uppercase slate without expansion. Valid stored user
case is preserved. Selected marked surfaces bypass I-word capitalization, final
autocorrect and typed-prefix capitalization, so the bar and commit agree.

Tests use the exact 24,683-byte producer sidecar, SHA-256
`e0a878249f021eff00eeb11165f600720f9d5fe24f383c9063e1b91c2a589a54`.
CI runs pure tests and targeted mock import, swipe commit and partial replacement
tests via the Gradle guard. Real IME/device verification remains required before
promotion. No language-model ranking quality is claimed by deterministic pairs.

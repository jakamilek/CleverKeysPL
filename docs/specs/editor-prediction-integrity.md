# Editor prediction integrity

Status: debug assembly and 2951 tests PASS on the Polish trial branch; whole CI still blocked by earlier lint/security findings; phone validation pending (2026-10-03). Priority regression: edits, cut/paste and suggestion selection can leave stale tracking or queued results; exact-add used cached deletion lengths before recommitting text.

## Requirements

- Dictionary add changes dictionary storage only. ExactAdd must not issue deletion, commit, key-event deletion or selection changes. It names the full live token at the caret, preserving case. An unreadable editor, selected range or mismatched token rejects a stale ExactAdd chip.
- Explicit add appears first in the typed-word bar, with words/scores/metas aligned. During mid-word editing the add names prefix + suffix, rather than a fragment.
- A committed letter refreshes the actual token from InputConnection; unavailable or excluded editor reads retain tap tracking. Old swipe/autocorrect state is discarded when editing a different token.
- A pending own-edit acknowledgement must not skip the next debounced editor read after cut/paste or a cursor move.
- Queued UI results belong to an editor revision and the submitted prefix/suffix. Cursor notifications (including unchanged caret positions), typing, selection, backspace, swipe results, field finish and private-mode changes invalidate preceding results.
- Old prompts must not block predictions for an edited word. An empty-prefix cursor callback preserves swipe/undo candidates only when the associated word is still present immediately before the caret.
- Password and technical-field prediction exclusions remain in effect. No new plaintext logs; playground marker is editor-sync-v4.

## Implementation

SuggestionHandler owns editorPredictionRevision. Prefix and next-word UI publications recheck it; prefix posts also compare their submitted prefix/suffix. InputCoordinator invalidates immediately before its existing 100ms debounce. CleverKeysService handles every collapsed selection callback and invalidates pending work during range selections.

PredictionContextTracker.refreshCurrentWordFromEditor shares the existing tokenizer and returns false when a live read cannot be used. The regular tap fallback appends the character in that case. synchronizeWithCursor no longer consumes one arbitrary callback based on expectingSelectionUpdate.

ExactAdd verifies the bounded live token/selection and calls dictionary add/refresh/confirmation only; it keeps the typed word pending for normal completion. It does not automatically complete or append a space. This supersedes the inherited exact-add delete/recommit behavior and tests. Ordinary completion suggestions keep their existing replacement and spacing behavior.

## Validation

EditorPredictionRegressionTest drives actual tracker and public typing/cursor entry points, with explicitly queued UI delivery: stale acknowledgement after cut/paste, reversed result delivery, immediate cursor invalidation, paste then type, stale prompt release, cut invalidating swipe preservation, unreadable tap fallback and exact full-token validation. SuggestionTapAddAndIWordTest drives public dictionary-add selection and forbids editor mutations, including Termux.

CI runs both classes through scripts/gradle-guard.sh alongside existing pure, casing, replacement and spacing tests. New results are pending; previous 2931 passes belong to 6122bf9b and do not validate this patch. Android instrumented/emulator/phone testing has not been performed here.

Phone check: type an unknown word and add it (text/caret unchanged); change it, cut/paste another word, append a letter and check the bar; place the caret inside a word and choose a completion; repeat with an equal-length replacement. Verify swipe alternates still work immediately after insertion. Keep a copy of important text during trial testing.

## Deferred work

Latest Shift-at-word-end and short/held Backspace semantics, scroll reset and diagnosis of Ale/Lub remain accepted follow-ups after the editor-integrity regression. No AI/CTC/dictionary format/release/version changes.

## CI follow-up (2026-10-03)

0d7a1744 compiled debug APK and all test sources successfully in run 37143267946. Pure run: 2779 tests, two failures: RELEASE_RECORD retained a renamed test anchor; LearningWiringDriftTest's literal matcher did not recognize the combined generation/revision condition. Mock regression suites were skipped and no APK artifact was uploaded. The follow-up updates the documentation anchor and separates the identical bar-generation guard from the added revision/password guard. Neither protection nor the M6 test is removed or weakened. New CI and phone verification remain pending. Security scan retains the previous four devalue HIGH findings.

## Verified follow-up (2026-10-03)

383137f8a60f372215cb887c57a4c115b46a488f: run 37143960348 compiled debug successfully and passed 2779 pure + 172 focused mock tests (2951 total). The eight EditorPredictionRegressionTest cases and twelve SuggestionTapAddAndIWordTest cases all passed. Existing replacement, capitalization, pointer, import and spacing suites passed as well. APK artifact 11281383313 is attached to that code commit. This supersedes the preceding pending-CI state; phone validation is still required. Full CI fails on the pre-existing ProduceStateDoesNotAssignValue at popover/SubkeyAssignActivity.kt:148 (1 error, 210 warnings) and four HIGH devalue 5.8.1 findings in site/bun.lock. Gates remain active; no device/instrumented or release build verification is claimed.

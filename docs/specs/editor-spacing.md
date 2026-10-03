# Field-aware suggestion and punctuation spacing

Status: device feedback reports weak behavior; investigation remains open. Follow-up c272f9ec1787310c69b681b8ef7dd0a7fdbfa813: debug build and 2907 selected tests PASS (run 37133110865), including five new full-path regressions. Previous build/tests at b373391c88de57edacd7ad2d0ad4ea8b62e23985 did not cover this wrapper. The workflow retains existing lint/security failures; the phone report is not closed.
Created: 2026-10-03. Maintainer approved the behavior after discussion.

## Requirements

- Ordinary text: swipe auto-insert and tapped suggestions obey the same before/after
  preferences. Do not duplicate existing whitespace or insert a space before a closer.
- Password (including visible/web), URI/email, numeric/phone/date and search-action
  fields: no automatic leading/trailing space, punctuation formatting, double-space
  period conversion or owed-space repair. Manual space keys remain literal.
- Search keeps its existing predictions and cursor synchronization; formatting
  eligibility must not be confused with prediction eligibility.
- Smart punctuation normalizes plain spaces at the caret for prose closers and adds
  one trailing space unless whitespace/closing punctuation already follows.
  This deliberately supersedes the old automatic-space-only restriction: a manually
  entered space before a comma is normalized too, when smart punctuation is enabled.
- Opening groups/quotes, lexical apostrophes/hyphens, numeric separators, technical
  tokens, tabs/newlines/indentation and unavailable context remain literal.
- Swipe alternate replacement verifies the real committed suffix and deletes a
  trailing space only if present. Preserve the separator preceding the word.
- Typed-prefix completion is explicitly out of scope: the maintainer withdrew the
  report and confirmed it works. No AI, CTC, dictionary or ranking change.

## Design and limits

EditorSpacingPolicy reads the live EditorInfo in both SuggestionHandler and
KeyEventHandler. Search means (imeOptions & IME_MASK_ACTION) == IME_ACTION_SEARCH;
URI address bars are also excluded. Unknown/missing editor metadata fails closed.
An app that advertises search as ordinary text cannot be identified reliably from
that metadata alone; no package-name guess list is introduced.

SmartAutoSpace remains pure. Its punctuation plan uses bounded text around the
caret, a maximum eight plain-space deletion, straight-double-quote parity and
explicit closer/opening classes (including Polish „ and ”). Numeric ./,/: after a
digit stays literal; this conservative rule also leaves sentence punctuation after
numbers without an added space. This is a deterministic formatter, not a complete
language parser for abbreviations or embedded URLs in arbitrary prose.

Legacy apostrophe attachment after a stamped automatic swipe space is retained without a new trailing space inside lexical forms.

The handler verifies the actual editor text at use, checks deletion success and
falls back to the literal key if context/deletion is unavailable. Field gating
also covers double-space-to-period and previously owed-space repair. Inline IME
panes retain their established routing before app-field formatting.

SuggestionHandler keeps the existing partial replacement and prediction pipeline.
The full swipe wrapper must not commit an independent separator before the shared
suggestion commit. Flush pending typed-word learning in order and clear its tracking,
then let actual surrounding text and the live field/preferences determine spacing.
Cursor synchronization can populate the tracked word without manual typing.
Exact-add and autocorrect-undo taps use the same field/preference separator policy.
Stale swipe alternate text is not erased. Terminal key-event fallback remains a
limited case when surrounding text is unavailable, and needs device verification.

## Validation

- Pure PunctuationSpacingTest: prose, manual spacing, existing separator, clusters,
  groups/quotes, PL opener, numbers/technical tokens, unknown context and replacement.
- EditorSpacingIntegrationTest: real key-up path with an editor buffer, live field
  changes, all password/technical types, search flags, preference/deletion failure,
  inline search and stale owed-space isolation.
- SuggestionTapPartialReplaceTest: retained partial replacement, updated URI result
  without a trailing space, search tap, password swipe/alternate, before preference,
  ordinary alternate and space-disabled alternate preserving preceding text.
- Existing pure, import, swipe-case, cursor Shift, pointer/bridge, slider, double-space and dropped-space repair checks.
- Complete SwipeAutocapCommitTest path with live editor buffer: typed word then
  swipe, before preference off, search, opted-in password and stale tracked word
  with an existing separator. All 12 tests in the suite PASS in run 37133110865.

Follow-up validation: 2773 pure + 134 focused mock tests PASS (2907 total).
Debug lint still fails on the pre-existing ProduceStateDoesNotAssignValue at
SubkeyAssignActivity.kt:148 (1 error, 210 warnings); release lint is skipped.
Security scan still fails on four HIGH devalue 5.8.1 findings in site/bun.lock.
Gates remain enabled; this is not an all-green workflow.

## Device report and diagnostics

The reported log contains joined tracker prefixes (łódźłodzi, łódźłodzijuror) and
omits the expected source casing pairs. It does not identify the active build,
pack/provider, preferences or intervening user actions. Debounced cursor logs can
combine a callback position with later editor text or a skipped/stale tracker sync;
do not infer an exact edit sequence or a proven root cause from them.

Trial marker `swipe-spacing-v2` identifies the new screen and IME. The IME debug
line reports app ID, spacing preferences, live formatting eligibility, top language,
provider presence and exact-form count, without editor text. The explicit playground
field logs bounded EDIT deltas (start, removed/inserted lengths and up to 120 inserted
characters; ␠/⏎/⇥ show spaces/newlines/tabs). This is scoped to the test screen.
Neither diagnostics nor passing mock tests close the real-device report.

Device checklist: ordinary notes/message; search box and browser address bar;
password (opt-in swipe if enabled); comma/colon/semicolon/?!/…/quotes/brackets;
decimal 3,14; both space preferences; alternate after swipe with space off; return
caret to an existing word and toggle łódź/Łódź with Shift.

No merge, tag, version bump or release is part of this trial.

# Cursor word capitalization through Shift

## Feature Overview

Status: phone feedback confirms repeated edits but reports selective cursor-position behavior (2026-10-03). Follow-up 6122bf9b66ef16cc966872883ae5b25c5c108a04 fixes missing batch acknowledgement handling; debug build, 2779 pure and 152 focused mock checks PASS (2931 total, CI 37140569285). Phone verification remains pending.
The workflow still fails on pre-existing SubkeyAssignActivity lint and site devalue security findings.
Motivation: after selecting łódź from a swipe pair, a user can return to the word
and change it to Łódź without retyping or locating the original suggestion slate.

## Requirements

- A plain unused Shift tap after an explicit cursor move into or to the start of
  a word toggles its FIRST letter's case, at every interior position.
- Maintainer correction: immediately AFTER the last letter, Shift keeps ordinary
  behavior. This supersedes the previous end-of-word editing requirement.
- Further taps toggle that letter back; suffix spelling, spaces, punctuation,
  and the cursor's absolute UTF-16 offset remain unchanged.
- This works for manually typed and swiped words independently of langpack metadata.
- During ordinary typing, and with the cursor after whitespace, Shift continues
  to latch for the next letter. Holds, chords, directional subkeys and Caps Lock
  retain their existing behavior.
- Range selections, inline keyboard modes, private/technical input variations,
  terminal editors and editors without usable selection/context APIs fall through
  to ordinary Shift. This stage does not provide a terminal key-event replacement.
- No lexical identity, AI reranking, dictionary metadata or morphology changes.

## Technical Design

Pointers intercepts only an unused released Shift tap before its latch/lock branch.
The view delegates through Config.IKeyEventHandler to KeyEventHandler.
CursorWordCapitalization retains session-only eligibility from full selection
callbacks; position acknowledgements of IME text mutations do not arm editing.
After a case edit, a missing final callback for the unchanged restored caret must
not suppress the next real user movement. The stale pre-commit-read guard applies
to ordinary text mutations only, not to a retained case edit at a known caret.
Unknown mutation positions suppress one callback. Initial input/finished input
clear the state, and further non-modifier typing disarms it.

The handler verifies a collapsed selection at the stored absolute position,
fetches at most 128 UTF-16 units in each direction, and constructs a pure edit
plan. Letter/mark runs and internal apostrophes/hyphens are supported; truncated
words, uncased letters and neighbouring technical identifiers are skipped.
Only one initial Unicode code point is replaced, with equal UTF-16 width. The
selected source character is checked before committing. A batch edit groups
selection, replacement and restoration. Failed pre-commit operations restore the
cursor where possible and fall through. A successfully committed replacement
consumes the Shift even if an uncooperative editor refuses cursor restoration.

IReceiver → KeyEventReceiverBridge → KeyboardReceiver refreshes cursor predictions,
invalidates stale swipe/autocorrect undo and auto-space bookkeeping. Case edits do
not call the typed-word learning funnel or add another dictionary entry.

## Implementation Plan

- [x] Pure plan/state and Shift tap routing.
- [x] Exact initial-code-point replacement and cursor restoration.
- [x] Bridge/receiver prediction refresh and mutation acknowledgements.
- [x] Pure and mock tests registered in CI.
- [x] Debug build and appropriate CI test suites (d64439ac).
- [ ] Real editor and device verification before promotion.

## Testing Strategy

Pure tests cover PL accents, combining marks/supplementary letters, boundaries,
technical tokens, mutation callbacks and repeated edits. Mock editor tests drive
the actual handler, both cursor positions, callback refresh, failed/stale
selection, selected-text mismatch, private fields and inline modes. Pointer tests
drive real release routing, consuming a handled tap and preserving latch/lock,
chord and subkey behavior. Existing importer, swipe commit, partial replacement,
bridge and slider tests remain CI checks.

Device checklist:

1. Swipe/select łódź, type more text, return the cursor to ł|ódź, łó|dź or łód|ź.
2. Tap Shift: Łódź appears, the caret remains in the same place, no spaces change.
3. Tap Shift again: łódź appears; repeat for malina/Malina and warszawska/Warszawska.
4. Type a new word normally and press Shift before its next character; existing
   word spelling must not change as an acknowledgement of IME typing.
5. Cursor at łódź| or after a space: Shift capitalizes the next typed letter normally.
6. Verify Caps Lock, Shift+letter hold, subkeys, field/app switches and editor undo.

## Limitations

Mock/JVM success cannot establish real InputConnection behavior. Editors can
return stale selection positions or refuse APIs; the feature requires a usable
text-document connection. The cursor mutation acknowledgement adds a selection
read after text commits; device latency remains unmeasured. Full-uppercase cycling,
selected-range transformations, AI and Łódźi→Łodzi are outside this change.

Created: 2026-10-03. No merge, release or version bump authorized by this stage.


Spacing integration at b373391c88de57edacd7ad2d0ad4ea8b62e23985: debug build, 2773 pure and all focused mock suites PASS (run 37123257770); device check still pending.

Follow-up test evidence: CursorWordCapitalizationTest covers every interior position,
no final case-edit callback and cursor-key disarming. ShiftWordCapitalizationTest
has 21 passing checks including a live-buffer walk across all positions with no
final batch acknowledgements. This reproduces a code gap; it does not prove that
it was the only cause of the phone symptoms. Baseline lint/security gates remain red.

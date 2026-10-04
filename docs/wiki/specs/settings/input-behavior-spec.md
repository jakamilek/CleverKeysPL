---
title: Input Behavior Settings - Technical Specification
description: Typing, capitalization, spacing, suggestions and configurable Backspace editing in the Polish trial fork.
user_guide: ../../settings/input-behavior.md
status: implemented
version: v1.2.7
---

# Input Behavior Settings Technical Specification

## Overview

The Input Behavior section owns text-processing behavior (capitalization, smart punctuation,
suggestion insertion), the touch thresholds that separate a tap from a swipe from a
long-press, and the number-row/numpad layout selectors. Its controls live in
`ui/settings/sections/InputBehaviorSection.kt`; the runtime consumers are
`Autocapitalisation`, `KeyEventHandler`, `SuggestionHandler` and `Pointers`.

Two neighbouring sections carry closely related keys and are cross-referenced where relevant:
**Gesture Tuning** (`GestureTuningSection.kt` — short-gesture bounds, double-space-to-period,
swipe-detection floors, selection-delete) and **Auto-Correction** (`AutoCorrectionSection.kt`
— autocorrection thresholds; the unified Backspace action is now in Gesture Tuning).

## Key Components

| Component | File | Purpose |
|-----------|------|---------|
| `Autocapitalisation` | `Autocapitalisation.kt` | Shift-state automation driven by the editor's caps mode |
| `KeyEventHandler` | `KeyEventHandler.kt` | Double-space-to-period, smart punctuation, backspace undo |
| `SuggestionHandler` | `SuggestionHandler.kt` | Commit-time capitalization, auto-spacing, exact-typed suggestion |
| `Pointers` | `Pointers.kt` | Touch handling: swipe/short-gesture/long-press classification |
| `Config` | `Config.kt` | Reads the preferences; derives px thresholds from them |
| `InputBehaviorSection` | `ui/settings/sections/InputBehaviorSection.kt` | The Settings UI |

## Auto-Capitalization

`autocapitalisation` is a **boolean**, not a mode enum. There is no per-app override of the
editor's own hint: CleverKeys *defers* to it. `Autocapitalisation.started` reads
`EditorInfo.inputType`, and when the editor requests no caps mode at all the feature turns
itself off for that field regardless of the preference:

```kotlin
// Autocapitalisation.kt:34-53
fun started(info: EditorInfo, ic: InputConnection) {
    this.ic = ic
    capsMode = info.inputType and SUPPORTED_CAPS_MODES
    val autocapEnabled = Config.globalConfig().autocapitalisation

    if (!autocapEnabled || capsMode == 0) {
        enabled = false
        return
    }

    enabled = true
    shouldEnableShift = info.initialCapsMode != 0
    shouldUpdateCapsMode = started_should_update_state(info.inputType)
    callback_now(true)
}
```

From then on the class tracks typed characters, sent key events (`KEYCODE_DEL`,
`KEYCODE_ENTER`) and selection changes, and calls back into the keyboard view to raise or
lower shift. `pause`/`unpause` exist so gesture input can suspend it mid-word.

Word commits take a second, independent path: `SuggestionHandler` re-checks
`Autocapitalisation.shouldCapitalizeAtCursor(ic, editorInfo, config.autocapitalisation)` at
commit time (`SuggestionHandler.kt:792`), because a tapped suggestion bypasses the
per-character flow.

`autocapitalize_i_words` (#72) is separate and unconditional on the editor's caps mode: it
uppercases a committed `i`, `i'm`, `i'll`, `i'd`, `i've` (`SuggestionHandler.kt:321`).

## Double-Space Period

Gated by `double_space_to_period` with a `double_space_threshold` timing window — **both of
which live in the Gesture Tuning section**, not Input Behavior. The implementation is inline
in `KeyEventHandler`, and it verifies the text actually in the field rather than trusting its
own memory of the last keystroke:

```kotlin
// KeyEventHandler.kt:373-392
if (config.double_space_to_period && !isKeyRepeat &&
    text.length == 1 && text[0] == ' ' && lastTypedChar == ' ' &&
    (currentTime - lastTypedTimestamp) < doubleSpaceThresholdMs) {
    // Audit A-5: verify at use that a space ACTUALLY precedes the cursor.
    val textBefore = conn.getTextBeforeCursor(2, 0)
    val spacePrecedesCursor = textBefore?.length == 2 && textBefore[1] == ' '
    val charBeforeSpace = textBefore?.getOrNull(0)
    if (spacePrecedesCursor && charBeforeSpace != null && charBeforeSpace.isLetterOrDigit()) {
        conn.deleteSurroundingText(1, 0)
        textToCommit = ". "
        lastTypedChar = '.'
    }
}
```

The `charBeforeSpace.isLetterOrDigit()` guard is what prevents `". ."` and `", ."` runs; the
`spacePrecedesCursor` re-read is what stopped space→backspace→space from eating a letter
(audit A-5).

## Smart Punctuation

`smart_punctuation` governs **auto-space swallowing**, not quote curling. When the previous
space was inserted automatically (after a swipe or a tapped suggestion) and the user then
types closing punctuation, that space is deleted so the punctuation attaches to the word:

```kotlin
// KeyEventHandler.kt:403-419
val smartPuncEnabled = Config.globalConfig().smart_punctuation
val isPunctChar = isSmartPunctuationChar(char)
val isQuote = isQuoteChar(char)

if (smartPuncEnabled && (isPunctChar || isQuote)) {
    val textBefore = conn.getTextBeforeCursor(500, 0)
    val eligible = SmartAutoSpace.isSwallowEligible(
        autoSpacePending = recv.wasLastSpaceAutoInserted(),
        stampedPosition = recv.getAutoSpaceStampedPosition(),
        actualPrevChar = textBefore?.lastOrNull(),
        actualPosition = PredictionContextTracker.currentCursorPosition(conn)
    )

    if (isPunctChar && eligible) {
        conn.deleteSurroundingText(1, 0)
        // sentence-ending punctuation re-adds a space so autocap can trigger
```

Eligibility is decided by the pure `SmartAutoSpace.isSwallowEligible`, which compares the
cursor position stamped when the auto-space was committed against the live cursor — so a
manually typed space or a cursor move can never be swallowed. There is no `smart_quotes`
preference; quote handling rides the same `smart_punctuation` flag.

## Gesture Thresholds

### Swipe detection

`swipe_dist` is a **stringly-typed** preference (legacy XML-preference compatibility) holding
a number, scaled at read time into a device-independent pixel threshold:

```kotlin
// Config.kt:771-774
val dpi_ratio = maxOf(dm.xdpi, dm.ydpi) / minOf(dm.xdpi, dm.ydpi)
val swipe_scaling = minOf(dm.widthPixels, dm.heightPixels) / 10f * dpi_ratio
val swipe_dist_value = safeGetString(_prefs, "swipe_dist", Defaults.SWIPE_DIST).toFloatOrNull()
    ?: Defaults.SWIPE_DIST_FALLBACK
swipe_dist_px = swipe_dist_value / 25f * swipe_scaling
```

`Pointers` compares an L1 (Manhattan) displacement against it — not Euclidean distance — and
there is no velocity requirement:

```kotlin
// Pointers.kt:971-975
val dx = x - ptr.downX
val dy = adjustedY - ptr.downY
val dist = abs(dx) + abs(dy)

if (dist >= snap.swipe_dist_px && ptr.gesture == null) {
```

`slider_sensitivity` (space-slider) and `circle_sensitivity` (circle gesture) are string
preferences read the same way. The slider percent is floored at 1, never 0: `slide_step_px`
is a divisor in `Pointers.Sliding`, and 0 produced ±Infinity and a cursor that moved the wrong
way (audit F-3, `SettingsRanges.SLIDER_SENSITIVITY_PERCENT`).

### Long press and key repeat

```kotlin
// Config.kt:795-799
longPressTimeout = safeGetInt(_prefs, "longpress_timeout", Defaults.LONGPRESS_TIMEOUT).toLong()
longPressInterval = safeGetInt(_prefs, "longpress_interval", Defaults.LONGPRESS_INTERVAL)
    .coerceIn(SettingsRanges.LONGPRESS_INTERVAL.first, SettingsRanges.LONGPRESS_INTERVAL.last).toLong()
keyrepeat_enabled = _prefs.getBoolean("keyrepeat_enabled", Defaults.KEYREPEAT_ENABLED)
keyrepeat_backspace_only = _prefs.getBoolean("keyrepeat_backspace_only", Defaults.KEYREPEAT_BACKSPACE_ONLY)
```

`longpress_timeout` is the delay before long-press fires; `longpress_interval` is the repeat
period after it does. With `keyrepeat_backspace_only` (default **true**) only backspace and
the navigation keys repeat — letters do not, matching Gboard/SwiftKey (#81).

### Short gestures

The short-swipe/word-swipe boundary is not a three-value enum. It is a pair of percentages
**of the starting key's diagonal**, carried by the `PercentOfKey` value class
(`Units.kt:17-22`, `toPx(keyDiagonalPx) = keyDiagonalPx * v / 100f`):

```kotlin
// Pointers.kt:951-962
if (ptr.key != null && !ptr.hasLeftStartingKey) {
    val keyHypotenuse = _handler.getKeyHypotenuse(ptr.key)
    val maxAllowedDistance = snap.shortGestureMaxDistancePx(keyHypotenuse)
    val distanceFromStart = sqrt((x - keyCenterX) * (x - keyCenterX) + (y - keyCenterY) * (y - keyCenterY))
    if (distanceFromStart > maxAllowedDistance) {
        ptr.hasLeftStartingKey = true
    }
}
```

`short_gestures_enabled`, `short_gesture_min_distance` (default 28%, slider 10-60) and
`short_gesture_max_distance` (default 141%, slider 50-200) are configured in the **Gesture
Tuning** section. See [Short Swipes](../gestures/short-swipes-spec.md).

## Configurable editing behavior (Polish fork trial v9)

The maintainer accepted v9 controls on the phone; runtime 5200 CI passed 3031 tests
and both lint gates. V12 CI passed 3044 tests and both lint checks, and the maintainer accepted the phone
behavior. V13 shares the space slider motion and requires separate CI/phone verification. No application version or release promotion changes here.

Gesture Tuning contains **Backspace**: a tap deletes one character/space by default;
an explicit mode can instead undo the immediately preceding autocorrection or remove
the verified last swiped word. Dragging left starts character selection immediately,
without waiting for a hold, after the shared space-slider step. A stationary
hold previews the preceding word while retaining its preceding separator; after
350 ms it deletes the word, waits 200 ms, then previews the next word while held.
Moving left switches permanently to character dragging for that pointer.
Movement owns the pointer across the whole keyboard.
Release deletes the verified selection by default; switching that option off leaves
the selection for a later edit. The release option does not disable timed word
deletion. Cancellation deletes no pending selection; earlier committed deletions
remain. See [the gesture specification](../gestures/selection-delete-spec.md#polish-trial-v12--direct-drag-and-repeated-words).

V13 supersedes the automatic drag repeat with SliderMotion, shared with Space.
Captured slide_step_px sets activation and character distance; slider_speed_smoothing
and slider_speed_max control response to finger speed. Stopping movement stops selection,
and reversing shrinks it without a pause/resume state. Character counts are batched into
one validated editor update per touch event. The stationary word timer remains unchanged.
See [the current specification](../gestures/selection-delete-spec.md#polish-trial-v13--shared-space-slider-motion).

Turning off drag selection and word deletion uses ordinary character repeat when Key Repeat is enabled.
The older vertical selection-delete sliders describe only the two-axis fallback when
an editor refuses the modern word preview. They do not tune the horizontal gesture.

Input Behavior contains **Text formatting** plus suggestion controls. Existing
auto-space-before/after options apply to both swipes and selected suggestions.
Punctuation attachment and following space can be switched independently under
Smart Punctuation. Search formatting is off by default; passwords, URI/email and
nontext fields remain excluded even when it is on. The numeric-period capital option
controls sentence capitals after e.g. `3. `, preserving unfinished decimals and
explicit editor all-capital hints. Shift word editing requires a user-returned,
collapsed cursor; its word-end behavior is independently configurable.

Source-backed capitalization surfaces remain attributes of one CKDT entry; this
toggle does not add duplicate dictionary keys or semantic descriptions. ExactAdd
can appear first or last with aligned score/provenance arrays. Adding it stays a
storage-only action, and asynchronous editor/cursor guards remain unconditional.

| Control | Key | Default | Range |
|---|---|---|---|
| Short Backspace action | `backspace_tap_mode` | 0 | 0–2 |
| Drag selection and word deletion | `backspace_hold_select` | true | on/off |
| Delete on release | `backspace_release_delete` | true | on/off |
| Remove preceding punctuation space | `punctuation_remove_space` | true | on/off |
| Add following punctuation space | `punctuation_add_space` | true | on/off |
| Format search fields | `format_search_fields` | false | on/off |
| Capital after numeric period | `numeric_period_caps` | true | on/off |
| Shift edits word initial | `shift_word_case` | true | on/off |
| Shift at word end | `shift_word_end` | true | on/off |
| Source case variants | `show_case_variants` | true | on/off |
| Add-word chip first | `exact_add_first` | true | on/off |
| Reset strip after Backspace | `reset_suggestions_on_delete` | true | on/off |

Defaults live in `Defaults`; `readEditBehaviorPreferences` uses safe bounded reads.
An invalid tap-mode enum falls back to character deletion. The immutable
`EditBehaviorOptions` is captured in `ConfigSnapshot` at pointer-down. The Compose
controls use `saveSetting`, preference notifications and `loadCurrentSettings`;
search is generated from their localized resource titles. Polish and base English shared-slider descriptions are current; changed semantics
in other locales are tracked for later translation. Scoped reset removes only its own group's preference keys.

`SETTINGS_DEFAULTS` and `SettingsValidation` include the new typed keys. Historical
`backspace_undo_swipe` / `backspace_undo_autocorrect` are deprecated, omitted from
exports and ignored on import. Their old true/true values do not select an undo mode.
The Config symbols remain solely to preserve historical RELEASE_RECORD anchors.

`BackspaceGestureTest` covers shared motion, acceleration and Unicode counts.
`EditingSettingsPolicyTest` covers punctuation switches and backup boundaries. `EditingSettingsReadTest` covers defaults, old keys, clamping and
immutable reads. Existing pointer, editor, Shift and capitalization suites include
option-specific cases. V13 passed 3049 tests in run 37192552115; the newer v14/v15
runtime results and phone checks remain separately pending here.

## Configuration

### Polish trial v15 — new-word suggestion viewport

`SuggestionBar.resetScrollPosition` posts a reset on its current parent
`HorizontalScrollView`. The posted callback checks that the parent still owns the bar;
theme/view replacement cannot scroll an old detached strip.
`SuggestionHandler` requests it for every nonempty accepted swipe slate, a one-code-point
typed prefix after live-editor synchronization, a typed word separator, and Enter/action
word boundaries outside password mode. A repeated swipe slate still resets even when
the content renderer skips identical suggestions. Longer prefix updates and cursor-sync
prediction updates preserve the viewport. In-word apostrophe/hyphen joiners retain their
existing branch. New-word resets are unconditional; `reset_suggestions_on_delete`
continues to control only the existing Backspace reset.

Three registered `SuggestionStripScrollTest` cases exercise the posted viewport reset,
repeated content and detached/replaced owners. Three additional
`LearningFunnelBookkeepingTest` cases drive real tracker/handler word transitions:
tap → separator → tap, repeated swipe slates, and swipe → tap → Enter. The typed-word
case disables the Backspace reset preference to verify the scopes stay separate.
Host execution and phone validation await the next runtime CI; no local Android
toolchain is available.

The separate Polish function-word correction is language-pack version 4. Installing a
new APK does not replace an already imported pack. Producer commit
`bb55e87f5bfa2c7ce2a3eb2214cf5a7f3031205f` passed run `37188544526`; its
`cleverkeys-pl-function-words-trial` artifact contains the pack ZIP to import.
It corrects source-backed default spellings (including ale/lub), retaining CKDT keys,
ranks and the existing variant sidecar. It adds no word-specific runtime exceptions.
User reports only updating the APK; installed pack bytes remain uninspected.

Every row is a preference key the app actually reads, with the control's own section noted
where it is not Input Behavior. "Range" gives the Settings slider bound; where the import
validator (`backup/SettingsValidation.kt`) accepts a wider band, both are shown.

### Typing and text processing

| Setting | Key | Default | Range |
|---------|-----|---------|-------|
| **Auto-Capitalization** | `autocapitalisation` | true | bool — inert when the editor requests no caps mode |
| **Capitalize "I" Words** | `autocapitalize_i_words` | true | bool (#72) |
| **Smart Punctuation** | `smart_punctuation` | true | bool — auto-space swallowing before punctuation |
| **Word Prediction** | `word_prediction_enabled` | true | bool — gates the whole prediction block below |
| **Suggestion Bar Opacity** | `suggestion_bar_opacity` | 80 | 0-100% |
| **Auto-Space After Suggestion** | `auto_space_after_suggestion` | true | bool (#82) |
| **Auto-Space Before Suggestion** | `auto_space_before_suggestion` | true | bool |
| **Show Exact Typed Word** | `show_exact_typed_word` | true | bool — offers the exact typed string (2+ chars, not already a prediction/dictionary/user word) as a tap-to-add `ExactAdd` suggestion (`SuggestionHandler.kt:2345`; switch added 2026-09-08, F-8/#42) |

### Touch thresholds

| Setting | Key | Default | Range |
|---------|-----|---------|-------|
| **Swipe Distance Threshold** | `swipe_dist` | `"23"` | slider 5-30 (stored as a string; scaled to px at `Config.kt:774`) |
| **Circle Gesture Sensitivity** | `circle_sensitivity` | `"2"` | 1-5 (string) |
| **Space and Backspace Slider Sensitivity** | `slider_sensitivity` | `"30"` | 1-100% (`SettingsRanges.SLIDER_SENSITIVITY_PERCENT`; string). Floor is 1, not 0 — F-3 |
| **Long-Press Timeout** | `longpress_timeout` | 600 | slider 200-1000 ms; validator 50-2000 |
| **Key-Repeat Interval** | `longpress_interval` | 25 | 25-200 ms (`SettingsRanges.LONGPRESS_INTERVAL`) |
| **Key Repeat** | `keyrepeat_enabled` | true | bool |
| **Backspace-Only Repeat** | `keyrepeat_backspace_only` | true | bool (#81) — shown only when key repeat is on |
| **Double-Tap Shift Lock** | `lock_double_tap` | true | bool |
| **Immediate Input Switching** | `switch_input_immediate` | false | bool |

### Layout selectors

| Setting | Key | Default | Values |
|---------|-----|---------|--------|
| **Number Row** | `number_row` | `no_number_row` | `no_number_row` / `no_symbols` / `symbols` |
| **Show Numpad** | `show_numpad` | `never` | `never` / `landscape` / `always` |
| **Numpad Layout** | `numpad_layout` | `default` | `default` (7-8-9 on top) / `low_first` (1-2-3 on top) |
| **Pin Entry Layout** | `number_entry_layout` | `pin` | `pin` / `number` — the switch writes this key directly; the old `pin_entry_enabled` write drove nothing (F-6, 2026-09-06) |

### Word Prediction subsection (added 2026-08-06)

The Advanced Prediction block of the Word Prediction group hosts the context-learning
controls and the Learning & Data manager (`LearningDataSection.kt`):

| Setting | Key | Default | Values |
|---------|-----|---------|--------|
| **Context-Aware Predictions** | `context_aware_predictions_enabled` | true | bool — learns the phrase model; prerequisite for next-word's LEARNED tier only |
| **Next-Word Prediction** | `next_word_prediction_enabled` | true (since 2026-09-26; explicit stored false kept) | bool — always enabled in Settings; the shipped static tier works with learning / context-aware off and in incognito fields |
| **Context Source** | `context_source` | `both` | `both` / `learned_only` / `static_only` |
| **Personalized Learning** | `personalized_learning_enabled` | true | bool |
| **Personalization Strength** | `personalization_weight` | 1.0 | 0.0-2.0 |
| **Learning Aggression** | `learning_aggression` | `BALANCED` | `CONSERVATIVE` / `BALANCED` / `AGGRESSIVE` |
| **Context Boost** | `prediction_context_boost` | 0.5 | 0.5-5.0 |
| **Frequency Scale** | `prediction_frequency_scale` | 100.0 | 100-5000 |
| **Max Learned Words** | `personalization_max_words` | 5000 | 1000-20000 (500 steps; least-value eviction) |

The master learning gate `on_device_learning_enabled` (default false on fresh v2.0 installs;
upgrades seeded true) lives in the Privacy & Data section and overrides all of the above at
the write AND read layers — except the next-word STATIC tier (shipped model, nothing
personal), which is deliberately outside every learning gate. See the
[Next-Word Prediction spec](../typing/next-word-prediction-spec.md) for the two-tier contract.

## Related Specifications

- [Gesture System](../../../specs/gesture-system.md) - Gesture recognition
- [Settings System](../../../specs/settings-system.md) - Preferences
- [Autocorrect](../typing/autocorrect-spec.md) - Text correction
- [Next-Word Prediction](../typing/next-word-prediction-spec.md) - Built-in and learned next-word suggestions

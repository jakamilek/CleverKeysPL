---
title: Selection Delete - Technical Specification
description: Immediate Backspace character selection, stationary word deletion and the legacy joystick fallback.
user_guide: ../../gestures/selection-delete.md
status: implemented
version: v1.4.0
---

# Selection Delete Technical Specification

## Polish trial v14 — ordered editor selection ranges

The maintainer accepts v13 movement in ordinary editors but reports SimpleX stopping
when a drag reaches a space before a word. The inspected SimpleX Android text field
normalizes onSelectionChanged endpoints before writing ComposeState; its AndroidView
update compares that normalized state to the live EditText endpoints and can run
setText/setSelection when they differ. Source inspected at SimpleX stable commit
479548ee53ffb73db73841e77acbeee5a78dbbd5:
[PlatformTextField.android.kt](https://github.com/simplex-chat/simplex-chat/blob/479548ee53ffb73db73841e77acbeee5a78dbbd5/apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/platform/PlatformTextField.android.kt).

The keyboard previously passed anchor before the left endpoint, i.e. reversed ranges.
V14 passes ascending start/end for initial word preview, each drag update and repeated
word re-arm. The fixed anchor remains in BackspaceHold; gesture direction, finger-speed
response, boundaries and word-cycle timing are unchanged. Identity, live selection/text
validation, cancellation and commit guards remain mandatory. No app-specific bypass or
unchecked deletion is added.

Three BackspaceHoldTest regressions model a state bridge that recreates the input
connection when reversed endpoints diverge from its normalized state. They cover word
preview/extension/reversal, direct drag across trailing and preceding spaces, and repeated
word re-arm. This is a compatibility model derived from the source, not evidence that the
device failure has that exact cause. V14 requires CI and a SimpleX phone retest.

## Polish trial v13 — shared space-slider motion

`SliderMotion.move` is shared by `Pointers.Sliding` and the modern Backspace DRAG.
Both capture slide_step_px, slider_speed_smoothing and slider_speed_max at pointer-down.
Distance accumulates a signed fractional character remainder; finger velocity updates
an exponential speed blend after each event, matching the space-slider calculation.
Elapsed time is at least 1 ms. Invalid coordinates are ignored; parameters are bounded
and each event emits at most 256 characters. A stationary event emits zero.

Backspace activation uses the space slider step in physical pixels (already scaled),
with the existing leftward horizontal-to-vertical ratio >2:1 before hold. After hold,
a deliberate left movement of one step switches permanently to DRAG. Vertical motion
is ignored by modern Backspace and does not accelerate its horizontal movement.
Each movement applies one signed Unicode code-point count through stepBackspaceHold:
one live editor validation and one setSelection per event. The buffer and initial
caret bound both directions. The same editor identity, text and acknowledgement guards
used in v12 remain mandatory; refusal never sends an unchecked DEL.

DRAG has no repeat timer. Stopping the finger stops selection without a special brake;
right movement shrinks it immediately once the accumulated character distance is met.
The WORD_PREVIEW / WORD_GAP timer cycle retains 350 / 200 ms and release/cancel behavior.
Transitioning to DRAG invalidates any queued word-cycle message permanently for that
pointer. Crossing other keys does not activate them.

The six old backspace_pause_enabled / pause_dp / resume_dp / speed_percent /
fast_percent / accel_percent keys are no longer read or shown and are classified as
DEPRECATED_KEYS: omitted from export and ignored by import. Reset clears the old keys
only within the Backspace group; it does not change shared Space settings. Shared
slider keys retain their existing backup types and ranges. Polish/base English copy
is updated; other locales are recorded for later translation in memory/todo.md.

V12 CI passed 3044 tests and both lint checks (run 37189695279); the maintainer accepted
its phone behavior. V13 has separate pure motion, real space/BS pointer comparisons,
stationary/word-cycle cancellation, batched Unicode/editor and backup regressions.
V13 CI run 37192552115 passed 3049 tests and both lint checks; the maintainer accepts
its general phone behavior. The reported SimpleX exception is tracked above.

## Historical Polish trial v12 — direct drag and repeated words

The ordinary-text path now starts horizontal selection in `Pointers.onTouchMove`,
before the long-press timeout. It requires the captured holdSelect option, a deferred
pointer starting on Backspace, finite coordinates and left travel of at least the
captured resumeDp scaled by density. Horizontal travel must exceed twice the
vertical travel, reserving diagonal flicks for subkeys. `beginBackspaceDrag` starts
with an empty selection at the caret; the first step selects one Unicode code point.
The pointer owns all keyboard motion and cancels its original long-press timer.

A stationary hold instead enters WORD_PREVIEW with the previous word selected.
The timer commits that verified selection after WORD_PREVIEW_MS (350 ms), enters
WORD_GAP, waits WORD_GAP_MS (200 ms) and previews the next word. It repeats until
release, cancellation, no complete preceding word or failed editor validation.
No unchecked DEL fallback runs when the modern word cycle stops. The cycle does
not depend on ordinary key repeat. The first preview still uses longpress_timeout.
The two cycle intervals are constants, not new settings.

| State | Timer | Deliberate left movement | Release |
|---|---|---|---|
| WORD_PREVIEW | Delete verified word, then WORD_GAP | DRAG with current word preview retained | Apply releaseDelete to pending preview |
| WORD_GAP | Preview next word, then WORD_PREVIEW | DRAG from the post-deletion caret | End without deleting another word |
| DRAG | Existing character-selection repeat | Extend, brake or resume using captured options | Apply releaseDelete to current selection |

DRAG never falls back to the stationary cycle when paused. A timer identifier is
invalidated when cancelled, so queued word-cycle messages cannot delete after
switching to a drag. Ownership remains until pointer-up/clear; release dispatches
neither the original Backspace nor any crossed key. Stale long-press messages do
not replace an active modern gesture.

`deleteBackspaceHoldWord` shares the verified empty-text replacement and prediction/
learning cleanup with release deletion. It installs a collapsed continuation session
before commitText so synchronous selection acknowledgements are captured. Before
`previewPreviousBackspaceWord` re-arms a preview, connection/editor identity, caret,
selected text and the remaining preceding snapshot must still match. A fresh bounded
read replenishes the preceding context without using an old extracted-text caret.
The separator before each deleted word remains; trailing spaces are selected with it.
Cancellation collapses a pending verified preview; prior deletions remain committed.

No preference key, backup format or default changes. holdSelect gates direct drag
and word cycling. releaseDelete governs release only, not timed deletion. resumeDp
is also the initial activation distance; its slider is available even with pause off.
Other drag rates/thresholds retain v9 behavior and the pointer-down ConfigSnapshot.
Polish and baseline English descriptions are current. The equivalent four keys in
20 other locales are recorded in memory/todo.md for later translation.

New regression cases are in the already registered BackspaceHoldTest (editor and
real pointer/timer integration) and PointersBackspaceHoldTest (routing, cycle,
cancelled-message suppression and release). Test execution and device acceptance
remain pending CI; local source/resource/search checks do not establish a phone fix.

## Polish trial v8 — brake and resume

The maintainer accepts v7 hold preview, keyboard-wide extension/reversal and
release deletion. The uploaded v7 runtime trace includes an acknowledged six-unit
word preview followed by successful empty-text replacement, accepted left/right
steps and zero-length release without deletion. This confirms those tested phone
paths, but does not establish why earlier v5/v6 phone trials failed.

The new requested contract inserts a pause between opposite directions. During
active selection, motion of at least 3 physical pixels against the most recent
directional extreme sets direction to zero. That event stops the timer and cannot
shrink/extend selection. Further motion of at least 15 physical pixels from the
pause position resumes left extension or right shrinking. Subthreshold touch
jitter leaves the pause position fixed. The first activation still requires a
leftward drag of 15 pixels. Edge speed after resuming is unchanged. Releasing while
paused commits exactly the current verified selection; cancellation and empty
selection retain their existing behavior.

BackspaceGesture.kt:38 defines Drag. Pointers.handleSelectionDeleteRepeat returns
without scheduling another modern timer while direction is zero; onTouchMove
cancels a pending timer at each pause/resume transition. Editor mutation, safe live
selection checks, tap deletion, Shift, spacing and unsupported-editor fallback are
unchanged. Five new pure cases and two pointer/timer cases cover braking, jitter,
both resume directions, stopped timer delivery and release. Existing lagging-editor
integration now checks that the brake leaves its selected text unchanged before
resumption. V8 debug assembly and 2795 pure + 211 focused mocks passed (3006) in run 37154899155; BackspaceHoldTest 23 and PointersBackspaceHoldTest 11 passed. APK artifact 11285860899 is available; phone acceptance is pending.

## Polish trial v7 — phone diagnostics, issue unresolved

Historical report before v7: the maintainer tested v6 and reported the same failure: hold previews a word, but
motion does not change the selection and lifting the finger does not delete it.
V6's 2992 passing host checks therefore do not establish a phone fix. The cause is
not confirmed; neither editor lag nor missing pointer events is assumed.

V7 adds playground-only `BACKSPACE` traces for the actual runtime/package marker,
input lifecycle, view reset/cancel, pointer hold ownership, first move, direction
changes, release/cancel, editor acknowledgements, rejected validation and the
boolean result of empty-text replacement. Offsets, counts, enum/integer key kind
and exception class are logged, never editor text or exception messages. Repeated
pointer and editor progress is capped at 24 entries per session; terminal outcomes
remain visible. Sink exceptions cannot alter editing. No deletion/casing/spacing
policy changes are introduced. V7 debug assembly and 2790 pure + 209 focused mocks passed (2999) in run 37153176431; BackspaceHoldTest 23 and PointersBackspaceHoldTest 9 passed. APK artifact 11284274382 is available; phone trace remains pending.

Device procedure: use `backspace-diagnostic-v7`, enable playground debug, clear
the log, type `olej mleko`, hold Backspace and release; then repeat with left motion,
right reversal and release. Copy the complete log including the `BACKSPACE RUNTIME`
line. The subsequent v7 phone log and maintainer report accept the tested hold/drag/release paths; no earlier cause is proven.

## Polish trial v6 — editor acknowledgement follow-up

Phone feedback accepts v5 tap deletion, sentence capitalization and Shift at word
end. Hold produces the word preview but dragging and release deletion fail; those
checks are not accepted. V5's 2985 passing CI checks did not model editor read lag.

KeyEventHandler.kt:99 installs its hold session before setSelection so a synchronous
acknowledgement is captured. Full selection_updated callbacks confirm absolute
ranges. A temporarily stale extraction no longer discards the physical hold. The
pointer timer can retry when the editor catches up. A callback supplements only a
missing extraction, the original caret or one of the last eight requested ranges;
unrelated live positions still block editing. The live selected text and editor
identity must still match before deletion. No release sends an unchecked DEL.

Seven BackspaceHoldTest regressions cover lag/recovery, release with an acknowledged
preview, extension/reversal, unexpected positions or changed text, synchronous
callbacks and real Pointers routing into the editor handler. V6 debug assembly and
2790 pure + 202 focused mock checks passed (2992) in run 37150909557; all 19
BackspaceHoldTest checks passed. Phone retest failed: drag and release deletion still do not work. The release contract and speed policy are unchanged.

## Polish trial v5 — CI checks passed; device validation pending

The modern path precedes navigation subkeys in Pointers.handleLongPress. A deferred
Backspace asks the view/Config.IKeyEventHandler for a word preview. The pointer owns
all subsequent motion until release or cancellation, independent of key repeat.
Its timer uses the full keyboard width, bounded 30–200 ms intervals, leftward
extension and rightward shrinking determined by movement direction. Selection is
bounded by the initial caret. Releasing after shrinking to zero sends no DEL.

KeyEventHandler captures the current connection, editor info, absolute caret and at
most 4096 UTF-16 units before it. It checks both live selection and selected text
before movement or deletion, and commits an empty replacement only for a verified
nonempty selection. Cancellation collapses the verified selection without deletion.
Truncated whole-word previews are skipped. Unicode steps preserve surrogate pairs.
A held word is deleted only on release, preserving its preceding separator.

Components: KeyEventHandler.kt:99 (beginBackspaceHold), BackspaceGesture.kt:7
(previousWord) and Pointers.kt (handleLongPress/handleSelectionDeleteRepeat).
BackspaceHoldTest and PointersBackspaceHoldTest drive the real handlers with mocked
editors/pointers; BackspaceGestureTest covers pure word boundaries and speed policy.
Guarded CI run 37148132597 passed debug assembly, 2790 pure and 195 focused mock checks; BackspaceHoldTest passed 12 and PointersBackspaceHoldTest passed 6. Real-device acceptance remains pending.

The historical implementation below remains the unsupported-editor fallback.

## Overview

Selection-Delete Mode is a gesture that enables text selection by swiping and holding on the backspace key. When activated, horizontal finger movement selects characters (left/right), vertical movement selects lines (up/down), and releasing the finger deletes all selected text. This provides a single fluid gesture for rapid text correction.

## Key Components

| Component | File | Purpose |
|-----------|------|---------|
| Pointers | `src/main/kotlin/tribixbite/cleverkeys/Pointers.kt` | Selection mode state machine, `handleSelectionDeleteRepeat()` |
| KeyEventHandler | `src/main/kotlin/tribixbite/cleverkeys/KeyEventHandler.kt` | Shift+Arrow key simulation |
| Config | `src/main/kotlin/tribixbite/cleverkeys/Config.kt` | Vertical threshold/speed settings |
| SettingsActivity | `src/main/kotlin/tribixbite/cleverkeys/activities/SettingsActivity.kt` | Threshold and speed sliders |
| VibratorCompat | `src/main/kotlin/tribixbite/cleverkeys/VibratorCompat.kt` | `TRACKPOINT_ACTIVATE` haptic on mode entry |

## State Flag

```kotlin
// Pointers.kt:1716
const val FLAG_P_SELECTION_DELETE_MODE = 1 shl 11  // = 0x800 = 2048
```

## Architecture

```
User Input (backspace key short swipe + hold)
       |
       v
+------------------+
| onTouchDown()    | -- Defers backspace for gesture detection
+------------------+
       |
       v
+------------------+
| Short Swipe      | -- Detects initial swipe direction
| Detection        |
+------------------+
       |
       v (if hold detected after swipe)
+------------------+
| FLAG_P_SELECTION | -- Sets mode flag, records activation center
| _DELETE_MODE     |
+------------------+
       |
       v
+------------------+
| handleSelection  | -- Timer-based repeat, sends Shift+Arrow keys
| DeleteRepeat()   |
+------------------+
       |
       v (on finger release)
+------------------+
| Delete selected  | -- Sends DEL key to remove selection
| text             |
+------------------+
```

## Data Flow

1. **Activation**: Short swipe on backspace key, then hold (don't lift finger)
2. **Selection**: Finger movement from activation center determines direction
   - Horizontal (dx): Shift+Left or Shift+Right for character selection
   - Vertical (dy): Shift+Up or Shift+Down for line selection (if threshold met)
3. **Speed Scaling**: Further from center = faster selection rate
4. **Release**: Sends DEL key to delete all selected text

## Activation Flow

```
Touch backspace
    ↓
Short swipe detected (any direction)
    ↓
Hold (don't release)
    ↓
After long-press timeout, set FLAG_P_SELECTION_DELETE_MODE
    ↓
Haptic feedback (TRACKPOINT_ACTIVATE)
    ↓
Selection mode active
```

## Activation Code

```kotlin
// Pointers.kt:1199 (inside handleLongPress)
val isBackspace = isBackspaceKey(ptr.value)
if (_config.keyrepeat_enabled && isBackspace) {
    if (movementDist >= _config.short_gesture_min_distance) {
        // User did a short swipe then held - enter selection-delete mode
        // Determine direction based on horizontal movement (left/right selection)
        val direction = if (dx > 0) 1 else -1  // 1 = right, -1 = left
        ptr.flags = (ptr.flags and FLAG_P_DEFERRED_DOWN.inv()) or FLAG_P_SELECTION_DELETE_MODE
        ptr.selectionDirection = direction
        // Store current position as reference for speed calculation
        ptr.keyCenterX = ptr.lastX
        ptr.keyCenterY = ptr.lastY
        // Vibrate to indicate selection mode activation
        _handler.onPointerFlagsChanged(HapticEvent.TRACKPOINT_ACTIVATE)
        // Start selection repeat timer
        startSelectionDeleteRepeat(ptr)
        return
    }
    // No significant movement - fall through to normal key repeat handling below
}
```

## Joystick Movement

```kotlin
// Pointers.kt:979 - handleSelectionDeleteRepeat()
private fun handleSelectionDeleteRepeat(ptr: Pointer) {
    if (!ptr.hasFlagsAny(FLAG_P_SELECTION_DELETE_MODE)) {
        return
    }

    // Calculate X and Y distances from activation center (like TrackPoint)
    val dx = ptr.lastX - ptr.keyCenterX
    val dy = ptr.lastY - ptr.keyCenterY
    val absDx = abs(dx)
    val absDy = abs(dy)

    // Get key dimensions for thresholds
    val keyHypotenuse = _handler.getKeyHypotenuse(ptr.key)
    // Approximate key height from hypotenuse (typical keyboard keys are wider than tall)
    val keyHeight = keyHypotenuse * 0.7f
    val maxDistance = keyHypotenuse * 0.5f

    // Vertical dead zone is configurable (% of key height)
    val verticalDeadZone = keyHeight * (_config.selection_delete_vertical_threshold / 100.0f)

    // Create modifiers with SHIFT for selection
    val shiftMod = KeyValue.makeInternalModifier(KeyValue.Modifier.SHIFT)
    val shiftedMods = ptr.modifiers.with_extra_mod(shiftMod)

    // Check horizontal movement (X axis) - select left/right
    // Uses standard TrackPoint dead zone for responsive horizontal selection
    if (absDx > TRACKPOINT_DEAD_ZONE) {
        val arrowKeyCode = if (dx > 0) {
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT
        } else {
            android.view.KeyEvent.KEYCODE_DPAD_LEFT
        }
        // Symbol codes from KeyValue.kt: left=0xE008, right=0xE006
        val symbolCode = if (dx > 0) 0xE006 else 0xE008
        val arrowKey = KeyValue.keyeventKey(symbolCode, arrowKeyCode, 0)
        _handler.onPointerDown(arrowKey, false)
        _handler.onPointerUp(arrowKey, shiftedMods)
    }

    // Check vertical movement (Y axis) - select up/down (line-by-line)
    // Uses larger configurable dead zone to prevent accidental line selection
    if (absDy > verticalDeadZone) {
        val arrowKeyCode = if (dy > 0) {
            android.view.KeyEvent.KEYCODE_DPAD_DOWN
        } else {
            android.view.KeyEvent.KEYCODE_DPAD_UP
        }
        val symbolCode = if (dy > 0) 0xE007 else 0xE005
        val arrowKey = KeyValue.keyeventKey(symbolCode, arrowKeyCode, 0)
        _handler.onPointerDown(arrowKey, false)
        _handler.onPointerUp(arrowKey, shiftedMods)
    }
}
```

## Vertical Threshold Calculation

The vertical activation threshold is computed against the **key height**, not the horizontal distance — this prevents accidental line selection while still allowing diagonal multi-axis movement.

```kotlin
// Pointers.kt:997
val verticalDeadZone = keyHeight * (_config.selection_delete_vertical_threshold / 100.0f)
```

This means a configured value of `40` requires the finger to travel 40% of the key height vertically before line selection begins.

## Independent Axis Handling

X and Y axes fire independently within the same repeat cycle, allowing diagonal selection. Both are checked each repeat tick using their own dead zones (X uses `TRACKPOINT_DEAD_ZONE`, Y uses `verticalDeadZone` derived from config).

## Bidirectional Movement

Selection direction is determined each repeat cycle from the sign of `dx`/`dy` relative to the activation center stored at mode entry:

- Move finger left of center → Shift+Left
- Move finger right of center → Shift+Right
- Return to opposite side → selection shrinks/extends the other way

## Repeat-Delay Calculation

```kotlin
// Pointers.kt:1052
val repeatDelay = when {
    movedHorizontal && movedVertical -> {
        // Both axes moved - use faster of the two (horizontal dominates)
        val horizDelay = TRACKPOINT_MAX_DELAY - (maxHorizNormalizedDist * (TRACKPOINT_MAX_DELAY - TRACKPOINT_MIN_DELAY))
        val vertDelay = (TRACKPOINT_MAX_DELAY - (maxVertNormalizedDist * (TRACKPOINT_MAX_DELAY - TRACKPOINT_MIN_DELAY))) / _config.selection_delete_vertical_speed
        minOf(horizDelay, vertDelay).toLong()
    }
    movedHorizontal -> {
        // Horizontal only - standard speed
        (TRACKPOINT_MAX_DELAY - (maxHorizNormalizedDist * (TRACKPOINT_MAX_DELAY - TRACKPOINT_MIN_DELAY))).toLong()
    }
    movedVertical -> {
        // Vertical only - slower speed using multiplier (lower multiplier = slower)
        ((TRACKPOINT_MAX_DELAY - (maxVertNormalizedDist * (TRACKPOINT_MAX_DELAY - TRACKPOINT_MIN_DELAY))) / _config.selection_delete_vertical_speed).toLong()
    }
    else -> {
        // Finger is in dead zone - check again after a short delay
        TRACKPOINT_MAX_DELAY
    }
}
```

`TRACKPOINT_MIN_DELAY = 30L` (Pointers.kt:1728) and `TRACKPOINT_MAX_DELAY = 200L` (Pointers.kt:1731) define the fastest and slowest repeat rates.

## Configuration

| Setting | Key | Default | Range | Description |
|---------|-----|---------|-------|-------------|
| **Vertical Threshold** | `selection_delete_vertical_threshold` | 40 | 20-80 | % of key height required for vertical selection |
| **Vertical Speed** | `selection_delete_vertical_speed` | 0.4 | 0.1-1.0 | Speed multiplier for vertical selection |

Defaults are declared in `Config.kt:526-527`:

```kotlin
@JvmField var selection_delete_vertical_threshold = 40  // % of key height to trigger vertical selection
@JvmField var selection_delete_vertical_speed = 0.4f    // Speed multiplier for vertical (0.1-1.0)
```

## Shift+Arrow Simulation

Selection requires holding Shift while sending arrow keys. The handler builds an internal SHIFT modifier and merges it into the pointer's modifier set before posting the arrow keyevent:

```kotlin
// Pointers.kt:1000
val shiftMod = KeyValue.makeInternalModifier(KeyValue.Modifier.SHIFT)
val shiftedMods = ptr.modifiers.with_extra_mod(shiftMod)
// ... shiftedMods is passed to _handler.onPointerUp(arrowKey, shiftedMods)
```

## Release and Delete

When the finger lifts while `FLAG_P_SELECTION_DELETE_MODE` is set, the pending selection is committed and a DEL key event removes the selected text via the active `InputConnection`.

## Error Handling

- Invalid threshold values: clamped to 20-80% by the settings UI
- Invalid speed values: clamped to 0.1-1.0
- No text to select: arrow key commands are sent but have no visible effect (harmless)

## Related Specifications

- [TrackPoint Mode](trackpoint-mode-spec.md) - Similar joystick pattern on nav keys
- [Cursor Navigation](cursor-navigation-spec.md) - Arrow key and slider handling

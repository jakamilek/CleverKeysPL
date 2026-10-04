---
title: Selection Delete
description: Select and delete text with backspace gesture
category: Gestures
difficulty: advanced
related_spec: ../specs/gestures/selection-delete-spec.md
---

# Selection Delete

## Polish trial v12 (2026-10-04)

| Action | Result |
|---|---|
| Tap Backspace | Use the selected tap action; the default deletes one character or space |
| Drag left from Backspace without waiting | Start selecting characters from the caret |
| Hold without moving | Preview the previous word, then delete it after a short pause |
| Keep holding | Preview and delete successive previous words |
| Release after a word was deleted | Stop; the next word stays untouched |
| Move left during a word preview or between words | Switch to character selection |
| Make a small reverse movement during selection | Pause; move farther to extend or shrink the selection |
| Release during a preview or drag | Delete the current selection, or keep it when Delete selection on release is off |

The first word appears after the configured long-press timeout. Its visible preview
lasts 350 ms; a 200 ms gap follows deletion before the next word is selected.
The space before the deleted word remains. The next deletion also consumes any
spaces following that word. Releasing or cancelling ends the cycle. Cancellation
does not delete a pending preview and cannot restore words already deleted.

Character selection starts after a deliberate left movement, without a long press.
**Start or resume distance** controls that movement (24 dp by default), as well as
resuming after a pause. Diagonal subkey flicks retain their existing route. Once
selection starts, the finger can cross other keyboard keys without activating them.
The existing pause, speed and acceleration settings continue to control dragging.
Holding still after pausing a drag does not switch back to word deletion.

**Drag selection and word deletion** enables both gestures. Turning it off restores
the earlier key-repeat fallback. **Delete selection on release** only controls the
pending selection when the finger lifts; stationary holding still deletes successive
words automatically. Passwords, terminals and unsupported editors retain their fallback.
This implementation requires CI and phone verification; it is not a released change.

## Historical Polish trial v8 (2026-10-03)

This behavior applies to ordinary text editors that support selection APIs. The maintainer has tested and accepted the v7 hold and release behavior. V8 adds a pause after a small movement in the opposite direction; its debug build and 3006 CI checks passed (run 37154899155), while phone checks remain pending. It is not a released change.

| Action | Result |
|--------|--------|
| Tap Backspace | Delete one character or space, including after swipe/autocorrection |
| Hold, then release | Preview and delete the preceding word; keep the space before it |
| Hold, then move left | Extend the preview selection towards preceding text |
| Make a small move opposite to the current direction | Pause with the current selection unchanged |
| After pausing, move farther left without lifting | Resume extending the selection |
| After pausing, move farther right without lifting | Start shrinking the selection, even in the left half |
| Release while paused | Delete exactly the current selection |
| Release with an empty selection | Delete nothing |

The finger can move across all keyboard keys without activating them. Moving closer
to the left edge speeds up extension; moving closer to the right edge speeds up
shrinking. Selection cannot cross the original caret to the right. The word preview
includes trailing spaces, so deleting “mleko ” from “olej mleko ” leaves “olej ”.
Deletion happens on release; holding alone does not repeatedly delete more words.
A cancelled touch restores the original caret when the editor session is still valid.

Passwords, terminal editors, inline search/edit panes and editors without usable
selection APIs retain their existing fallback. Legacy backspace-undo preferences
do not intercept ordinary taps in this trial. After deletion the suggestion strip
returns to its beginning.

## Legacy fallback

The description below documents the older joystick fallback; its vertical controls
and key-relative speeds do not apply to the ordinary-text trial path above.

Selection Delete mode lets you select text by holding backspace and moving your finger like a joystick. When you release, the selected text is deleted.

## Quick Summary

| What | Description |
|------|-------------|
| **Purpose** | Quickly select and delete text |
| **Activation** | Short swipe + hold on backspace |
| **Movement** | Joystick-style text selection |

## How It Works

1. Do a short swipe on backspace and hold
2. Move your finger to select text (like TrackPoint)
3. Release to delete the selected text

The gesture combines selection and deletion in one fluid motion.

## How to Use

### Step 1: Short Swipe on Backspace

Touch backspace and do a small swipe in any direction, then **hold** without lifting.

### Step 2: Selection Mode Activates

After holding briefly, selection mode activates. You'll feel a haptic pulse.

### Step 3: Move to Select

Move your finger in the direction you want to select:

| Direction | Selection |
|-----------|-----------|
| **Left** | Select characters to the left |
| **Right** | Select characters to the right |
| **Up** | Select lines above |
| **Down** | Select lines below |

### Step 4: Release to Delete

Lift your finger. All selected text is deleted.

> [!WARNING]
> This action cannot be easily undone. Select carefully!

## Bidirectional Selection

You can change direction while selecting:

1. Start selecting left
2. Move finger right to reduce selection
3. Move further right to select in the other direction

The selection follows your finger movement continuously.

## Vertical Selection

Moving up/down selects by lines:

| Setting | Description |
|---------|-------------|
| **Vertical Threshold** | How much vertical movement triggers line selection |
| **Vertical Speed** | How fast line selection moves |

Configure in Settings > Gesture Tuning > Selection-Delete Mode.

## Speed Control

Like TrackPoint, speed depends on distance:

- **Near center**: Slow selection (precise)
- **Far from center**: Fast selection
- **Vertical**: Slower than horizontal (configurable)

## Tips and Tricks

- **Small deletions**: Don't move far, quick release
- **Large deletions**: Swipe to edge for fast selection
- **Change direction**: Move opposite way to deselect
- **Cancel**: Move back to center and release (selects nothing)

> [!TIP]
> Practice with non-important text first. The gesture takes some getting used to.

## Difference from Regular Backspace

| Action | Regular Backspace | Selection Delete |
|--------|-------------------|------------------|
| **Gesture** | Tap or hold | Short swipe + hold |
| **Deletes** | One char / word at a time | Selected region at once |
| **Direction** | Backwards only | Any direction |
| **Speed** | Fixed repeat rate | Variable |

## Settings

| Setting | Location | Description |
|---------|----------|-------------|
| **Vertical Threshold** | Gesture Tuning | % of key height for vertical activation |
| **Vertical Speed** | Gesture Tuning | Speed multiplier for line selection |

Default values:
- Vertical Threshold: 40%
- Vertical Speed: 0.4x (slower than horizontal)

## Related Features

- [TrackPoint Mode](trackpoint-mode.md) - Similar joystick for cursor
- [Cursor Navigation](cursor-navigation.md) - Basic cursor movement
- [Short Swipes](short-swipes.md) - Regular short swipe on backspace deletes word

## Technical Details

See [Selection Delete Technical Specification](../specs/gestures/selection-delete-spec.md) for implementation details including:
- FLAG_P_SELECTION_DELETE_MODE flag
- Shift+Arrow key simulation
- Axis tracking algorithm

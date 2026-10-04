---
title: Input Behavior Settings
description: Configure typing behavior and text processing
category: Settings
difficulty: intermediate
---

# Input Behavior Settings

Configure how CleverKeys processes your input, including capitalization, punctuation, and gesture behavior.

## Quick Summary

| What | Description |
|------|-------------|
| **Purpose** | Control typing behavior |
| **Access** | Settings > Input (and related sections) |
| **Options** | Auto-cap, double-space, smart punct, gestures |

## Word Prediction Section

### Starting a new word

The Polish fork trial returns the suggestion strip to its first candidate for a new
swipe, the first typed letter of a word, and a word-ending separator or Enter.
Continuing to type the same word preserves your scroll position. The Backspace reset
switch continues to affect deletion only.

The Polish dictionary's ale/lub spelling correction requires importing the updated
language pack version 4; updating the keyboard APK alone does not replace an imported
dictionary. After extracting the GitHub artifact, import its inner
`cleverkeys-pl-function-words-trial.zip` through the language-pack manager. Test ale/lub
in the middle of a sentence; sentence-start capitalization and explicit Shift still apply.

### Auto-Space After Suggestion

Automatically add a space after a swiped word or a tapped suggestion:

| Setting | Result |
|---------|--------|
| **Enabled** | "hello" → "hello " (space added) |
| **Disabled** | "hello" → "hello" (no space) |

### Capitalize I Words

Automatically capitalize "I" and its contractions:

| Words Affected |
|----------------|
| I, I'm, I'll, I'd, I've |

### Show Exact Typed Word

Offer the letters you actually typed as an extra suggestion when they aren't already a
dictionary word or a prediction. Tapping it adds the word to your dictionary. **Default: On.**

| Setting | Result |
|---------|--------|
| **Enabled** | Typing an unknown word (2+ letters) shows it as a tap-to-add suggestion |
| **Disabled** | Only dictionary predictions appear |

### Next-Word Prediction

Suggest the next word before you type a letter. **Default: On.** Uses a built-in phrase model
(for English, built from public text corpora), so it works even with Privacy & Data > Learn
From My Typing off; with learning and Context-Aware Predictions on, your own learned phrases
are added and ranked first.
See [Next-Word Prediction](../typing/next-word-prediction.md) for a full walkthrough.

### Context Source

Which phrase model boosts predictions while you type:

| Option | Behavior |
|--------|----------|
| **Both** (default) | Best of the built-in phrase model and your learned patterns |
| **Learned only** | Only your own learned phrase patterns |
| **Built-in only** | Only the shipped phrase model |

### Personalization Strength

How strongly your personal word usage boosts predictions, from 0.0 (off) to 2.0
(double strength). Default: 1.0.

### Learning & Data

Shows what the keyboard has learned on this device — per-language phrase-pattern counts
and word-usage stats — with **Browse phrases**, **Browse words**, **Forget phrases**, and
**Forget words** controls. Individual learned entries can be deleted from the browse
dialogs. Learned data is included in dictionary exports (Backup & Restore).

#### Max Learned Words

Caps how many words the personalization vocabulary keeps (1000–20000, default 5000).
When the vocabulary is full — or you lower the cap below the current word count — the
least-valuable words (rarely and least-recently used) are evicted first.

## Input Section

### Autocapitalization

Automatically capitalize letters after sentence-ending punctuation:

| Setting | Behavior |
|---------|----------|
| **Enabled** | Capitalize after . ! ? |
| **Disabled** | Never auto-capitalize |

### Smart Punctuation

Automatic punctuation formatting:

| Feature | Example |
|---------|---------|
| **Auto-space after punct** | "hello," → "hello, " |
| **Remove space before punct** | "hello ," → "hello," |

### Long Press Timeout

Time before long press activates:

| Duration | Use Case |
|----------|----------|
| **Shorter** | Fast access to long-press actions |
| **Longer** | Avoid accidental activation |

### Long Press Interval

Repeat rate when holding a key:

| Setting | Effect |
|---------|--------|
| **Shorter** | Faster key repeating |
| **Longer** | Slower key repeating |

### Double Tap Shift for Caps Lock

Double-tap shift key to enable caps lock mode.

## Gesture Tuning Section

### Double-Space to Period

Insert period and space when tapping space twice:

| Setting | Result |
|---------|--------|
| **Enabled** | "hello  " → "hello. " |
| **Disabled** | "hello  " → "hello  " |

### Double-Space Timing

Adjust the timing window for double-space detection.

### Swipe Distance Threshold

How far to swipe before recognizing a short swipe gesture:

| Level | Use Case |
|-------|----------|
| **Lower** | More sensitive, easier activation |
| **Higher** | Requires more intentional swipes |

## Tips and Tricks

- **Fast typing**: Lower thresholds and shorter timeouts
- **Precision**: Higher thresholds, longer timeouts
- **Error-prone**: Raise swipe distance threshold

> [!TIP]
> If you're getting accidental short swipes, increase the swipe distance threshold.

## Common Questions

### Q: Why isn't autocapitalization working?

A: Check if it's enabled in Settings > Input section. Some apps may override keyboard behavior.

### Q: How do I disable double-space period?

A: Settings > Gesture Tuning > Double-Space to Period > Off.

## Related Features

- [Short Swipes](../gestures/short-swipes.md) - Gesture configuration
- [Accessibility](accessibility.md) - Haptic feedback settings
- [Next-Word Prediction](../typing/next-word-prediction.md) - Next-word suggestions (built-in + learned)
- [Privacy Settings](privacy.md) - The Learn From My Typing master switch


## Editing controls in the Polish trial fork (v9)

The maintainer accepted the v9 controls on the phone. The v12 gesture update below
requires separate build and phone verification.
In **Gesture Tuning → Backspace**, choose a short-tap action. The default deletes
one character or space. The optional alternatives undo the last autocorrection or
delete the last swiped word while it is still the verified token before the cursor.

Drag left directly from Backspace to start character selection without waiting.
Hold without moving to preview and delete successive preceding words. The preview
lasts 350 ms, followed by a 200 ms gap before the next preview. Lift to stop.
Moving left during the word cycle switches to character selection.
Drag left across the keyboard to extend selection, or right to shrink it. Stop your
finger to stop selection; slow motion gives precision and quick motion accelerates.
Other keys do not activate. Release deletes by default; switch **Delete selection on
release** off to keep the selected range. This does not disable automatic word deletion
while holding still. Disabling **Drag selection and word deletion** restores ordinary
repeat when Key Repeat is on.

Space and Backspace share **Space and Backspace Slider Sensitivity** in Input Behavior
and speed response / maximum acceleration in **Gesture Tuning → Cursor and Selection
Sliding**. Lower sensitivity values mean less travel per character. The former
Backspace brake, resume, edge speed and travel acceleration controls are retired;
old backup values are ignored. Resetting Backspace does not reset shared Space settings.

The older vertical controls belong to the fallback gesture for unsupported editors.
Each editing group has its own reset button, and the options are searchable and
included in settings backup/restore.

In **Input Behavior → Text formatting**, independently control spaces before/after
punctuation, formatting in search boxes (initially off), numeric-period sentence
capitalization, and Shift changing a word's first letter, including at its end.
Passwords and technical fields always keep literal input. Shift still needs a cursor
you returned to an existing word; after whitespace it has its ordinary function.

The prediction group lets you show dictionary case alternatives, place the add-word
chip first or last, and return the strip to its start after Backspace. **Show Exact
Typed Word** remains the master for the add-word chip. Adding a word never deletes
or rewrites the text already in the editor.

See the [technical specification](../specs/settings/input-behavior-spec.md#configurable-editing-behavior-polish-fork-trial-v9)
for keys, ranges, field guards and the trial's verification status.

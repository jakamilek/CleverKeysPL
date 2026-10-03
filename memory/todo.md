# Current work queue

## Polish trial v5 (2026-10-03)

- [x] Implement character-only Backspace taps and non-destructive word hold preview.
- [x] Implement keyboard-wide reversible selection with edge-dependent speed.
- [x] Restore explicit Shift word-end editing and reset suggestion scroll after deletion.
- [x] Add live Android sentence-rule fallback after punctuation; preserve search/private exclusions.
- [x] Register focused regression tests through scripts/gradle-guard.sh.
- [ ] Confirm v5 compilation, pure and focused mock results in Actions (no polling beyond 60 s).
- [ ] Test hold/reversal/cancel/field switch and occasional missing capitals on the phone.
- Prior editor-sync-v4: 2951 CI checks passed; maintainer says editor behavior works OK.


Updated: 2026-09-30. Full execution state and test evidence: [HANDOFF.md](HANDOFF.md).
Campaign plan: [`docs/plans/2026-08-30-full-backlog-campaign.md`](../docs/plans/2026-08-30-full-backlog-campaign.md).

The September 1 campaign baseline was `5fb58037`; subsequent work through `79f0b464`
was pushed with maintainer authorization on September 27. Preserve shared-tree work.

## Polish trial — editor integrity regression (2026-10-03)

- Priority: maintainer reports stale/missing typed-word suggestions after edits/cut/paste and text disappearing on dictionary add. Supplied log still identifies v2; the exact phone sequence is not captured.
- [x] Implement dictionary-only exact add with live token/selection validation; never delete or recommit editor text. Offer it first and use the full token at a mid-word caret.
- [x] Implement prediction revision guards, immediate invalidation on cursor notifications, live post-key token refresh, prompt dismissal during edits and validation before preserving swipe/undo slates.
- [x] Add eight real-pipeline regression tests and update dictionary-add tests; register both in guarded CI.
- [x] 0d7a1744 debug assembly and test compilation PASS; run 37143267946: 2779 pure, 2 failures (stale RELEASE_RECORD test anchor and literal M6 guard matcher). Regression/mock suites skipped, no uploaded APK.
- [x] Repair test anchor and split the added editor-revision guard from the unchanged bar-generation guard; preserve both protections and the M6 test.
- [x] 383137f8 debug assembly + 2779 pure + 172 focused mock PASS (2951 total), run 37143960348. New editor regressions 8 and dictionary-add route 12 PASS. APK artifact 11281383313.
- [ ] Phone validation for editor-sync-v4; whole CI remains blocked by the earlier SubkeyAssignActivity lint and four devalue HIGH findings. No local Android SDK/Gradle toolchain.
- [ ] Resume latest Shift-at-word-end and short/held Backspace changes after the integrity regression. Resetting scroll and Ale/Lub casing remain pending.
- Monitoring policy: maximum 60 seconds TOTAL per Actions build, then user reports status; no idle polling loop.
- Spec: [editor-prediction-integrity.md](../docs/specs/editor-prediction-integrity.md).

## Polish trial — cursor word capitalization (2026-10-03)

- Maintainer reports that the on-device source casing pairs work quite well; this is qualitative feedback, not a complete acceptance checklist.
- [x] Implement Shift toggling the first letter after returning the caret to an existing word; preserve ordinary typing, pointer modifiers and cursor position.
- [x] Shift commit d64439ac: debug build, pure and focused mock checks pass (run 37114260076).
- [x] Latest phone report: edits work selectively. Fixed/tested missing final batch callback before the next cursor move; every interior position must work. That stage excluded the position after the last letter; the latest request re-enables it, pending after the integrity regression.
- [ ] Validate cursor-caps-v3 before promotion; whole CI still has pre-existing lint/security failures.
- Spec: [cursor-word-capitalization.md](../docs/specs/cursor-word-capitalization.md).

## Polish trial — field-aware spacing (2026-10-03)

- [x] Agreed shared swipe/tap preferences, search/password/technical field exclusions and punctuation rules.
- [x] Implement actual-suffix alternate replacement, manual-space punctuation and field guards.
- [x] b373391c88de57edacd7ad2d0ad4ea8b62e23985: debug build, 2773 pure + 129 focused mock checks PASS (run 37123257770).
- [x] New phone log identifies debug v2, PL provider and łodzi/Łodzi pair; maintainer reports much better behavior. Previous missing-pair/spacing report not fully reproducible from old logs.
- [x] Remove unconditional independent pre-swipe space; route whole swipe through shared field/preferences policy. Add five complete-path buffer regressions and trial/EDIT diagnostics.
- [x] c272f9ec1787310c69b681b8ef7dd0a7fdbfa813: debug build, 2773 pure + 134 focused mock PASS (2907 total), run 37133110865; APK artifact 11277678542.
- [x] Numeric sentence follow-up: after 3. + next word use shared gated autocap boundary; preserve 3.4 and search/password exclusions. Playground advertises sentence caps. 6122bf9b: debug build, 2779 pure + 152 focused mock PASS (2931 total), run 37140569285, APK 11280003851.
- [ ] Verify cursor-caps-v3 on phone; no claim that all selective-Shift causes are established or resolved.
- Typed-prefix completions are working per maintainer; explicitly withdrawn from scope.
- Spec: [editor-spacing.md](../docs/specs/editor-spacing.md).

## September 27 follow-through

- [x] Prior 54 commits through `79f0b464` pushed; commit-specific CI, site deployment,
  APK build and UI/performance workflows succeeded.
- [x] Existing `langpacks` release updated: Norvig asset removed; 22 replacements and
  description published; all 22 downloaded SHA-256 hashes verified.
- [x] Local Astro build: 84 pages pass under Bun using the current `bin/astro.mjs` entry.
  Legacy wiki HTML URLs are deployment-generated redirects, not stale published bodies.
- [x] Site TypeScript check and `build:termux` pass; Android Rollup is optional and the
  lockfile is synchronized. No dependency versions changed.
- [x] Builder now rejects unsupported `--lang` before any reads/writes; mypy passes after
  correcting fractional-count annotations/callback typing. CKLM encoding matches the previous
  implementation across all six corpus-selection weights on a controlled fixture.
- [x] Final local gates: Kotlin compilation, Android lint, 2,610 pure tests and 859 mock
  tests pass (guarded run 7m51s). Site build: 84 pages, 104 affected-page links resolve;
  TypeScript and Python mypy pass. Saga connected read-only; no app/settings changes.
- [x] Translation structural audit: 936/936 resources in all 21 locales; indexed arguments
  and plural items match. Expanded translation guard passes 6/6 focused tests.
- [x] Multilingual LM pilot (2026-09-29): per-language builder configs (en byte-identical),
  language-parameterised S1 eval + drift test. Spanish FAILED S1 (prefix-1 +4.69 < +5), so
  nothing beyond `en` ships; de/fr/it measured passing, pt/sv failing —
  `docs/eval/2026-09-29-static-lm-multilingual.md`.
- [x] Static LM contraction-lookup fix + per-language shipping (2026-09-29): REPLACE keys
  (`dont`, `cest`) now resolve to the display form the model names (was backoff only — en
  `i → dont` 0.51 vs `i → don't` 27.5). One re-evaluation, unchanged gates: de +5.16, fr +6.75,
  it +7.48 pt prefix-1 → SHIPPED (with Tatoeba contributor lists, NOTICE, PROVENANCE);
  es/pt/sv still fail, unshipped. `docs/eval/2026-09-29-static-lm-multilingual.md`.
- [x] Next-word allow check admits contraction display forms through their apostrophe-free
  dictionary key (`NextWordContractionAllowTest`, 2026-09-29).
- [x] es/pt/sv LM retry (2026-09-29, pre-registered, dev-selected second Leipzig corpus + Tatoeba
  weight, one test look): pt +5.41 and sv +5.98 prefix-1 → SHIPPED; es +4.96 → FAILED, unshipped.
- [x] Legacy hardcoded tables no longer penalise unlisted pairs or apply English's tables to other
  languages (es `static_only` prefix-1 −22.65 → +0.04 pt). `docs/eval/2026-09-29-static-lm-multilingual.md`.
- [ ] TODO: es LM needs a NEW stated reason before another attempt (e.g. a larger test population).
- [x] LM ratio shape measured (2026-09-29, pre-registered): dev rule kept `RAW` (FLOOR_ONE −0.15
  en prefix-1 on dev); test read once, FLOOR_ONE ahead there (+0.46 mean) — recorded only.
  `both` never applies a static penalty (learned boost ≥ 1). `docs/eval/2026-09-29-static-lm-multilingual.md`.
- [ ] TODO: FLOOR_ONE for `static_only` — needs a fresh pre-registration with new dev evidence.
- [x] Pack-attribution UI: Settings → Multi-Language → Language Packs → Manage shows each pack's
  licence, credit, source links and full NOTICE.txt (`269d8bb1`/`6fed1b12`, 2026-09-29).
- [x] `privacy_forget_learned_body` names swipe corrections in all 22 locales (`535a2a28`);
  terminology unified per locale with `TranslationGlossaryTest` (`4871c7e5`, `a91aad5a`).
- [ ] TODO: native-speaker translation review and device visual verification remain distinct
  from automated structural checks; preserve the maintainer's manual-checklist edits.
- [x] i18n follow-ups closed (2026-09-30): FAQ content `721c757d`, RTL pane arrows `213e8d52`,
  localized I/O failure reasons `cfc0eed2`, command catalog in 21 locales `19d64857`, localized
  settings search + id-keyed scroll `607da6df`. `docs/i18n/2026-09-29-hardcoded-ui-sweep.md`.
- [ ] TODO: fa/hu device check of the above (pane arrows, search scroll) and native review of the
  465 command-catalog strings.

## Maintainer/release gates

- [ ] ARC-053: soak the minified release APK; ARC-062/096 implementation is already present.
- [ ] ARC-054: decide whether v1.6 release notes announce ru and synthesis-holdout-only el.
- [ ] ARC-063: narrow blanket R8 keeps only after the first minified soak.
- [ ] Decide any nonzero `finger_occlusion_offset` default only from device-trace A/B evidence.

## Agent-executable backlog

- [ ] ARC-067: translate the common 384 missing resources into all 21 locale files. Preserve
  placeholders and plurals shapes; do not use English copies. ARC-066/087 are complete.
- [x] Finish Wave E: ARC-073 citation/doc drift (`d20ed3b5`), ARC-098 phantom-`keyboard2`
  tooling sweep (`f482faf4`), the four verified doc-claim repairs, and the
  `contraction_pairings_cleaned.json` gate run (the file was already deleted in `030265ee`).
  ARC-076 and ARC-089 are complete. ARC-098's source-tree half (`gesture/`,
  Bridges/Initializers→`wiring/`) remains under ARC-072 slice 3 below.
- [ ] ARC-072 slice 3 composition-root/reorg work, folded with the gesture portion of ARC-098.
- [ ] ARC-027/028/029 geometric experiments, evidence-gated on non-regressing corpus replay.
- [ ] ARC-071 migration is superseded by the installed Astro 7 site; reconcile the remaining
  ARC-046 web regression gate/Tailwind vendoring evidence.
- [ ] ML-side ARC-060/061 and the documented verb-inversion feasibility work. (ARC-056
  uk/bg/mk/he lexicons/langpacks CLOSED 2026-09-01 — `538a1633`/`86156ea3`.)
- [ ] ARC-044 remaining assertion-strengthening batch (no Truth dependency in androidTest).

## Verification backlog

- [x] Final guarded host gates on implementation commit `5fb58037`: `runPureTests` 2,087 and
  `runMockTests` 343, both passing on 2026-09-01.
- [ ] Wave J: full ew-cli instrumented run, including ARC-058/064/074/077/091/092/095.
- [ ] Wave K: both authorized phones per the campaign protocol; restore IME/properties and
  never framework-restart Saga. Capture ARC-068/069/070 evidence.
- [ ] Wave L remainder: update the ARC ledger and maintainer-input report. HANDOFF, backlog, and
  campaign-plan state were consolidated on 2026-09-01.

## Release authority

Do not commit, tag, push, publish, or open external issues without explicit user authorization.

# Current work queue

## Live CI repairs after run37510369362/37510378362 (2026-10-06)

- [x] Android compilation and original native conformance PASS; standalone APK not uploaded.
- [x] BS45, pointers26, haptic dispatch1 and editor/source mocks PASS (159 total).
- [ ] Full pure suite: old signature scanners fixed (2857 run, 2 FAIL before fix).
- [ ] Learning mock fixture primary language populated; password no-space expectation corrected.
  29 run/19 FAIL before fix, 18 null-language failures; rerun required.
- [x] Live pure gate now has explicit pipefail; earlier tee masked nonzero Gradle.
- [x] site source-map-js resolved1.2.2 with registry/archive SHA512 verification and mapping smoke.
- [ ] Fresh Trivy, all pure/mock/lint/APK gates and phone check still pending.
- Evidence: docs/HERBERT_LIVE_CI_2026-10-06.md. Do not claim overall CI/APK ready.


## Immediate typed-autocorrect BS restore (2026-10-06)

- First short BS after a verified typed correction restores original + kept space, including default character mode.
- Unknown original gets guarded first ExactAdd; explicit adding stays storage-only; second BS normal.
- Same caret/editor acknowledgements preserve the offer; edits/cut-paste/field/selection/move disarm it.
- Seven registered mock cases added; local Android execution unavailable, CI/phone pending.
- Prior live run 37507317429 FAILED compile: enabled setter/setEnabled JVM signature collision.
  Corrected by naming the activity action setLiveEnabled; full gates must rerun, no APK success claimed.
- [ ] Translate edit_typed_autocorrect_restore_help in other locales later (base/Polish complete).
- [ ] After updated live APK preparation, global dictionary remains next.

## Current priority — HerBERT live test and BS haptics (2026-10-06)

- User explicitly authorizes isolated original HerBERT FP32 live case-pair test; SI default off.
- Implementation and phone steps: docs/HERBERT_LIVE_TRIAL_V1.md; branch trial/herbert-live-v1.
- [ ] Mandatory Android compilation, pure original-tokenizer conformance, editor/BS/mock/lint/APK CI.
- [ ] Phone: pair choice, next-touch/timeout fallback, stale edits, private/search skip, BS haptic gates.
- [ ] After app preparation, return to GLOBAL swipe dictionary coverage: dodam, grzeje, kasami,
  nawilżane, odpowiadam, patrzysz, poczekaj, podpowie, pozdrawiam. Exact producer drop causes pending.
- High FP32 PSS and historical INT8 / distilHerBERT safe-margin quality failures remain evidence;
  no production promotion, no release/version change. Earlier v3 queue below is historical.
- [ ] Other locales: translate all 14 herbert_live.xml keys later; Polish/base complete, no native review.
- Three stationary BS timing preferences remain later work, without changing accepted space-like drag.

## Fresh Polish case v3 launched (2026-10-05)

- Frozen producer c1e9d3a7895f2a380bc75772985c88a53db32a4b, branch experiment/polish-mlm-fresh-v3,
  draft PR10; run 37363474711 queued; [protocol](https://github.com/jakamilek/CleverKeys-langpack-pl/blob/c1e9d3a7895f2a380bc75772985c88a53db32a4b/experiments/polish_mlm_fresh_v3/PROTOCOL.md).
- 64 fresh authored cases (4 balanced strata, 8 known/8 new keys; short/21–26 words),
  64 immutable regression cases; both original HerBERT/distilHerBERT rerun 256 requests.
- 27 local pure gate tests, 8 AST PASS; original distil tokenizer 256 requests/512 targets,
  max39 tokens PASS before weights. All uploaded blobs verified.
- [ ] After run completion: verify original native loads/parity, all complete artifacts,
  exact collector recomputation, fresh/historical regressions and 16/32 pairs separately.
- Authored fresh gold is not an external independent/human blind benchmark.
  No default16/production approval, punctuation, weights, ONNX/APK/live SI or INT8 changes.
- License review deferred at user request; no teacher-license assumption or author contact.
  Actions monitoring <=60 seconds TOTAL/run, no waits until completion.

## Smaller Polish MLM v2 — completed, next evidence needed (2026-10-05)

- [x] Run 37359525824 SUCCESS: exact original tokenizer, strict full weights and head parity;
  384/384 requests each. 19 contract tests; three ZIP identities and exact collector recomputation.
- [x] Archive [complete mixed results](https://github.com/jakamilek/CleverKeys-langpack-pl/blob/a729fd780241a581f9cac153c0a6d926d8bfa1c1/experiments/polish_mlm_compare_v2_results/RESULTS.md), raw traces and provenance.
  distilHerBERT natural 25/32 vs HerBERT 24/32; older forms 46/64 vs 50/64;
  new punctuation 16/24 vs 20/24. Exploratory case screen PASS; production not approved.
  Geotrend natural 21/32, screen FAIL. Keep regressions, no method changes after results.
- [x] Freeze fresh authored diagnostic case v3 with longer 16/32 inputs and all known failures.\n- [ ] External independent/human-reviewed context quality remains distinct from authored v3.
- [ ] Deferred per maintainer 2026-10-05: clarify distilHerBERT weight license before redistribution; no teacher-license assumption
  or author contact authorization. Source revision 7276461b7a8fd668aaf30313c03a68bd11aad642.
- [ ] Only then justified original FP32 export/parity, exact native fixtures and Nubia PSS/latency.
  81,967,184 parameters and host 984.55 MiB are not phone RAM evidence.
- Natural <=14 words produce identical 16/32 inputs. Default 32/max64 remains;
  distance stress 16=16/32, 32=19/32 separate. No APK/live SI/punctuation activation.
- Source screening preserved: 122480 surfaces; distilHerBERT full coverage,
  Geotrend standalone ą/ę unknown but all pairs/requests pass; no gate exception.
  Geotrend BERT12/ORIS reserve, previous HerBERT INT8 FAIL immutable.
- Actions monitoring <=60 seconds total per run; three stationary Backspace timing controls backlog.


## Smaller Polish SI comparison v1 completed (2026-10-05)

- Run 37355290211 FAILURE: contract 11 tests PASS, HerBERT 384/384 PASS;
  DistilRoBERTa failed unknown-target tokenizer gate, collector correctly failed.
- Reject sdadas/polish-distilroberta revision849b664fa3134beae84095d28a184c145c6a3aa5:
  original uppercase Ł in Łódź/Łotysz becomes ID3; 26 spans in 22/384 requests.
  No quality or mobile-cost comparison for that model; no gate/dataset change.
- Evidence producer 9162d9a9f2ac3b94f4212a773cf9be80e1cb632a:
  experiments/polish_mlm_compare_v1_results/RESULTS.md, raw HerBERT predictions,
  verified artifact SHA and exact report recomputation, reproducible tokenizer diagnosis.
- HerBERT new natural 24/32 at both limits, contexts 6–14 words (same input).
  Artificial distance 16/32 versus 18/32; not independent evidence to change default.
- Next: screen other smaller models with exact source-form/case/Polish Unicode tokenizer
  coverage before loading weights; then separately frozen quality/mobile measurements.
  No model selected for deployment, no new APK, keep default32/max64 and live SI off.
- Review: docs/eval/2026-10-05-polish-ai-alternatives.md (initial candidate superseded).
  Mapped-vs-path HerBERT remains reserve; failed INT8/model trust intact.


## Phase memory/timing trial v2 (2026-10-05)

- Prepared: fresh-session two-form workload first, mandatory conformance after timings,
  finally close + immediate PSS observation; model/arena/threads/live IME unchanged.
- Six case rows (30 samples each), retained words/B/S/T, first request and full duration;
  PSS baseline/load/first, warmup/timed per case/window, conformance, close and deltas.
- Progress/copyable partial report, code/base-APK/model hashes, PID and trial ordinal;
  base/Polish 20 added keys with non-Polish backlog. Closed scorer drops mapped reference.
- Nine registered metric/lifecycle regressions PASS in CI run 37348519387:
  2851 pure tests +83 focused editor tests, real original conformance, lint/APK gates PASS.
- Phone phase-v2 native conformance PASS and identity matched; load dominates PSS:
  baseline 346.2 → loaded 2121.0 → workload max 2132.1 → closed 1336.8 MiB.
  Same model ZIP; no new producer export. Single run, not isolated model memory.
- Raw phone report: docs/eval/2026-10-05-herbert-fp32-nubia-phase-v2.md.
- Next planned: isolated mapped-vs-private-path loading experiment, streaming SHA,
  pre/post-hash/session PSS and component breakdown; unchanged model/score/rank gates.
  Not implemented yet, no claimed memory saving. Compare smaller models if needed.
- Fix Polish report plural “1 próbek” in next UI change.
- Step guide: docs/HERBERT_FP32_PHASE_TRIAL_V2.md. No live activation/version bump/release.


## FP32 phone import repair (2026-10-05)

- Phone import failed with only a generic message; exact device exception unknown.
- Reproduced ZipInputStream rejection of GitHub compression-level 0 STORED entries
  with trailing descriptors; the same tiny fixture succeeds with ZipFile.
- Implemented bounded private container copy + bounded central directory + ZipFile,
  unchanged seven SHA/size identities, extraction and cancellation cleanup.
- Base/Polish fixed reason codes and copyable failure report; no URI/path/raw exception.
  Import preflight about 1.33 GB; UI requests 1.4 GB free, temporary ZIP removed.
- Run 37280833641 at 884a29b SUCCESS: compile, JUnitCore OK(2842), mandatory original
  token/feed conformance, 83 focused tests, debug/vital lint, assembly and APK audit/upload PASS.
  APK artifact 11331993610; updated phone guide. Seven new import regressions registered.
- Phone report on nubia NX721J /Android 15 /arm64-v8a confirms import and full native parity:
  2471 vectors /232 batches, max displayed error 0.000062. Timings p50/p95: 32=82.2/229.0 ms,
  64=81.7/326.9 ms. Load 1256.2 ms; max sampled process PSS 2699.3 MiB.
  Raw report and limitations: docs/eval/2026-10-05-herbert-fp32-nubia-phone-v1.md.
- Next: baseline/phase PSS and intended two-form workload before conformance, per-context
  timings and report identity. Independent quality/energy and production gate remain open.
- Same producer/model artifact; only runtime APK rebuild. No live SI/version/release change.


## Polish SI verified FP32 package and diagnostic APK (2026-10-04)

- Producer 37230171787 at d831e17: SUCCESS, byte-identical FP32, zero archived rank changes;
  2471 token vectors and 4352 exhaustive scalar-block portable reference comparisons PASS.
  Metadata artifact 11312569320 fully locally verified; model ZIP 11312693984 external.
- Runtime 37230173951 at 63524ecd: compile, JUnitCore OK(2834) +83 focused tests passed;
  real-fixture test still assumption-skipped then. Debug lint failed on 19 untranslated
  trial-only strings; release vital lint/assembly skipped. No APK from that run.
- Current changes: pin seven trusted file hashes; mandatory real 2471-token/232-batch JVM test,
  compressed test-only original fixtures; local trial resource MissingTranslation ignore
  with other-locale backlog, global lint unchanged. Context default 32 words, max 64/4096 UTF16;
  explicit 32/64 benchmark timing comparison. New test covers default vs cap/short text.
- Workflow will assemble and publish ARM64 diagnostic debug APK only after required gates;
  checks that FP32 weights and test fixtures are absent. No version bump or live IME changes.
- Run 37231774451 at 90615f0: actual compile and JUnitCore OK(2835), mandatory original
  2471-token/232-batch/532-candidate five-input conformance PASS. Workflow then failed
  solely because rg marker-check command was absent (exit 127); later gates skipped.
- Replaced marker search with Python stdlib assertion, preserving all gates and source.
- Repaired run 37232458354 at 73627fa SUCCESS: compile, JUnitCore OK(2835), real
  original tokenizer/feed conformance, 83 focused tests, debug/vital lint, assembly
  and APK audit/upload PASS. ARM64 artifact 11314463703; steps/hashes in
  docs/HERBERT_FP32_PHONE_TRIAL_V1.md. Raw APK not locally verified; CI evidence only.
- Pending: install
  on Nubia, import external ZIP, check native scores/ranks and compare warm/cold timings/PSS.
  No current phone/JNI/independent accuracy claim. No editor history, logging or network.
- Further: independent slates/contexts; opt-in live dispatcher/settings. Preserve both forms,
  geometric scores and single SuggestionHandler commit pipeline. BS timing controls backlog.
- Max 60 seconds TOTAL Actions monitoring/build. No merge/release/version bump/subagents.


## New-word suggestion viewport v15 (2026-10-04)

- Maintainer reports only updating the APK, not importing function-word pack v4.
  Producer run 37188544526 SUCCESS for bb55e87f5bfa2c7ce2a3eb2214cf5a7f3031205f;
  artifact 11297484236 contains cleverkeys-pl-function-words-trial.zip. Import still
  requires phone action; existing pack bytes and user case overrides are uninspected.
  No new hardcoded Ale/Lub exceptions or duplicate dictionary entries.
- Implemented: strip resets for every nonempty accepted swipe, first typed code point,
  word separators and Enter/action boundaries; continuing a prefix keeps its viewport.
  Existing reset_suggestions_on_delete remains scoped to Backspace. No new setting.
- Three posted-view regressions plus three real tracker/handler transition regressions
  registered in focused CI, including identical swipe slates and BS-reset disabled.
- Local Android/Kotlin execution unavailable; runtime CI and phone validation pending.
  V14 SimpleX retest still pending; v15 includes that change without modifying BS motion.
- Do not monitor ongoing runtime runs this turn. No merge/release/tag/version bump.

## Backspace ordered editor ranges v14 (2026-10-04)

- Maintainer accepts v13 generally; SimpleX freezes selection at a space before a word.
- Source-backed compatibility hypothesis: SimpleX normalizes selection endpoints in its
  Compose state and can resync/rebuild when live reversed endpoints differ. Our three
  Backspace selection requests were reversed; now send ascending start/end and retain
  the fixed anchor in the session. No weaker text/identity/deletion guards.
- Three registered editor regressions model resync on reversed ranges and cover hold,
  direct drag across word separators/reversal and successive word previews.
- CI compile/test/lint and SimpleX phone retest pending. The source/model does not prove
  the exact device failure; if the retest fails, collect lifecycle/selection diagnostics.
- Source: SimpleX stable 479548ee53ffb73db73841e77acbeee5a78dbbd5,
  PlatformTextField.android.kt; details in the canonical selection-delete spec.
- Preserve max 60 seconds TOTAL monitoring per build. No merge/tag/release/version bump.

## Backspace space-slider motion v13 (2026-10-04)

- Implemented: SliderMotion shared by Space and modern Backspace; distance/finger-speed
  controls selection, stationary finger emits nothing, one batched Unicode-safe selection
  update per move. Stationary word previews/deletion keep v12 timing and guards.
- Retired six old Backspace repeat/brake controls; ignored at runtime/import, omitted
  from export. Shared slider preferences and scoped resets retain existing types.
- Polish/base English shared sensitivity, speed help, group and Backspace descriptions
  updated. Deferred in cs de es fa fil fr hu in it ja ko lv nl pt ro ru tr uk vi zh-rCN:
  input_space_slider_title/desc, gesture_slider_key_header, gesture_speed_smoothing_desc,
  gesture_max_speed_multiplier_desc and edit_backspace_help still describe Space alone
  or the old motion. Evidence: values-*/strings.xml vs shared SliderMotion consumers.
- V12 CI run 37189695279 passed 3044 tests + both lint; maintainer accepts v12 on phone.
- V13 CI run 37192552115 passed 3049 tests + both lint; general phone acceptance
  received, SimpleX-specific selection freeze tracked in v14. No local Android toolchain.
- Preserve max 60 seconds TOTAL monitoring per build; no merge/tag/release/version bump.

## Backspace direct drag and repeated words v12 (2026-10-04)

- Implemented: direct left drag selects characters from caret without holding;
  stationary hold cycles word preview (350 ms), verified deletion, gap (200 ms),
  next preview. Release stops; a drag never resumes the stationary cycle after braking.
- Added 13 regressions in the existing registered pointer/editor suites, including
  real pointer/timer-to-editor integration, Unicode, synchronous callbacks, stale
  timers, release during gap, mutation/connection guards and rejected commits.
- Verified: Actions 37189695279 passed 3044 tests, assembly and both lint checks.
  Maintainer accepts v12 phone behavior and requests Space-like movement in v13.
- Polish and baseline English four titles/help strings updated; no new preference
  keys or backup changes. Other 20 resource locale sets deferred: cs de es fa fil fr hu
  in it ja ko lv nl pt ro ru tr uk vi zh-rCN. Keys edit_backspace_hold_select_title,
  edit_backspace_help, edit_backspace_resume_dp_title, edit_pause_help still describe
  only the old hold/resume gesture. Evidence: values-*/strings.xml at runtime 5200,
  new direct-start/cycle semantics in Pointers. Translate later per maintainer priority.
- Preserve max 60 seconds TOTAL Actions monitoring per build. No merge/release/tag.

## Backspace brake/resume v8 (2026-10-03)

- [x] Maintainer accepts v7 on phone; supplied runtime trace confirms preview, left/right steps, deletion on release and empty release. Earlier v5/v6 cause remains unproven.
- [x] Pause on small opposite movement from active extreme (3 px); resume only after further motion (15 px) from the pause point, left or right.
- [x] Cancel repeat while paused; preserve current selection for release, edge speed and keyboard-wide pointer ownership.
- [x] Add five pure and two pointer/timer regressions; retain test names and update lagging-editor integration to assert unchanged selection during brake.
- [x] V8 debug assembly, 2795 pure + 211 focused mocks PASS (3006), run 37154899155; APK artifact 11285860899. Existing lint/security blockers remain; phone checks pending.
- [ ] Phone acceptance for backspace-pause-v8: pause, stable hold, both resume directions and release while paused.
- Accepted tap deletion/casing/editor integrity preserved; producer unchanged. Maximum 60 seconds TOTAL Actions monitoring per build.

## Backspace phone diagnostics v7 (2026-10-03)

- Phone rejects v6 too: word preview works, horizontal extension/reversal and release deletion do not.
- [x] Add bounded playground-only pointer/editor/lifecycle traces, actual runtime package marker and failure reasons; log offsets/counts only, no editor text.
- [x] Add seven diagnostic regressions for failure reasons, text exclusion, bounded output, cancellation and sink failures.
- [x] V7 debug assembly, 2790 pure + 209 focused mocks PASS (2999), run 37153176431; APK artifact 11284274382. Existing lint/security failures remain; v7 phone trace received and accepted below.
- [x] V7 phone log supplied; maintainer accepts hold/drag/release. Trace confirms tested paths. Proceed with requested brake/resume UX.
- Cause remains unconfirmed; v7 changes diagnostics, not the editing policy. Preserve accepted tap/casing behavior.
- Monitoring: maximum 60 seconds TOTAL per build, then maintainer reports completion.

## Backspace acknowledgement follow-up v6 (2026-10-03)

- Phone confirms v5 tap deletion, sentence capitals and Shift at word end.
- Phone rejects v5 hold drag/release: preview appears, extension and release deletion fail.
- [x] Retain gesture through temporarily stale reads; use full selection callbacks to confirm own requests while extraction lags; keep exact live-text and editor guards.
- [x] Add seven editor/pointer regressions for lag, reversal, synchronous callbacks and unrelated changes.
- [x] V6 debug build, 2790 pure + 202 focused mock checks PASS (2992), run 37150909557; artifact 11284341482.
- [x] Retest hold/drag/release on phone with backspace-gesture-v6: FAILED, same drag/release symptoms.
- Existing 2985 passing v5 checks did not establish real editor timing.

## Polish trial v5 (2026-10-03)

- [x] Implement character-only Backspace taps and non-destructive word hold preview.
- [x] Implement keyboard-wide reversible selection with edge-dependent speed.
- [x] Restore explicit Shift word-end editing and reset suggestion scroll after deletion.
- [x] Add live Android sentence-rule fallback after punctuation; preserve search/private exclusions.
- [x] Register focused regression tests through scripts/gradle-guard.sh.
- [x] V5 debug assembly, 2790 pure and 195 focused mock checks PASS (2985), CI 37148132597; artifact 11282318484. Existing lint/security failures remain.
- V5 phone: tap/capitals/Shift accepted; hold drag/release failed. Follow-up tracked under v6 above.
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


## Polish fork editing settings v9 — 2026-10-04

- Implemented: 18 typed controls, larger dp pause/resume band, configurable normal/fast
  rates and screen-travel threshold, unified tap action, release policy, Shift/formatting/
  suggestion options; 22 resource sets, backup classification and scoped reset.
- V9 runtime 897edfa3: debug assembly + 2804 pure + 227 focused tests PASS (3031),
  run 37183355586; APK 11295888906. Maintainer accepts introduced changes on phone
  (2026-10-04, qualitative feedback, not an itemized backup/editor checklist).
  No local Android SDK, Gradle or Kotlin compiler here.
- Preserve the 60-second TOTAL Actions monitoring cap. No merge/tag/release/version bump.
- Pack/producer/AI/CTC unchanged. Existing lint/security issues, instrumented/minified/
  performance checks remain open. Ale/Lub producer correction passed CI; pack v4
  import on the phone remains pending (see v15).

## CI stabilization after accepted v9 — 2026-10-04

- Prepared: remembered mapping state + keyed LaunchedEffect in SubkeyAssignActivity;
  devalue 5.9.3 exact override and Bun-generated lockfile (no other package changes).
- Pending: guarded Android compile/pure/focused/debug lint/release vital lint and
  unchanged HIGH/CRITICAL security gate in the next Actions run. Do not claim green CI
  from the earlier 3031 passing tests.
- Polish UI translation is the priority. Record non-Polish locale defects by key,
  symptom and evidence for later; native review remains deferred.
- Local Bun audit no longer reports devalue. It still reports http-cache-semantics
  GHSA-ch52-4w7c-c8xp (HIGH; advisory lists no patched version), four moderate Svelte
  findings and one low esbuild finding. These are recorded separately; the unchanged
  CI gate excludes unfixed findings. A newer npm http-cache-semantics release exists,
  but its repair of this advisory is not verified here.


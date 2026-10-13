# Source metadata checkpoints through v10

Current closure: v9 Android64943d82 runs38173067500/38173069844 SUCCESS;
APK a9b22644 and exact v9 ZIP delivered/accepted. V10 d2a45358 runs38301095587/
38301100893 SUCCESS;204463 words/114299 entries, all source views/ranks retained.
ZIP11dee57b delivered; maintainer confirms works very well. v11 remains next.
Phone PSS unmeasured; private duplicate cleanup and other-locale RAM translation
remain separate. The preparation-time notes below are historical.

## Shared metadata v9 — 2026-10-11
- [x] Maintainer accepted v7 and v8 import/recognition; v8 CI38167348861 verified.
- [x] Lossless exact source-header/attribute sharing; API/schema2 reader with v1 compatibility.
- [x] 190813 words/100649 views unchanged;28 actual parser tests and71 producer tests PASS.
- [x] Real cold v9/v8→v9/repeated-v9 +16MiB ballast PASS at192MiB; no bounds raised.
- [ ] Current-head Android/import MockK/editor/lint CI and verified APK+ZIP delivery.
- [ ] Phone import/restart/SI/source-case/Manager regression and PSS check.
- [ ] Then expand deferred groups within measured capacity; final private duplicate cleanup local-only.
- No model change, pruning, merge/release/version bump; Actions<=60s total/run.

## v7 import “unknown error” — 2026-10-11
- [x] OOM→UNKNOWN reproduced (no phone trace); share validated equal immutable entries;
  rebuild index/header, typed Polish/base RAM reason, strict rollback/bounds/data.
- [x] Local23parser/full actual consumer PASS; real v6/v7 and representative91K→98K
  replacement +16MiB ballast PASS at192MiB. No GC/largeHeap/pruning.
- [x] 141f27e7 Android/import MockK/pure/localization/lint CI38142499529/38142502456 PASS; APK delivered.
- [x] Phone v7 import/recognition accepted; host result remains separate from phone PSS.
- [ ] Other values-* (except pl): pack_error_intelligence_memory absent/English fallback (scoped lint/test exemption); deferred.

## Polish v6 — accepted import, slow Dictionary Manager 2026-10-11
- [x] v6 import/Manager speed accepted;2f4735dd live38133738323/CI38133740554 PASS;
  2918pure/192MiB/rows/editor/lint;APKe69041ae delivered. Evidence in dictionary spec.
- [ ] Phone early-switch crash/search/sort/recreation retest still pending; load-speed acceptance alone does not confirm those paths.
- [x] v7 thematic and v8 names/abbreviations accepted on phone.
- [ ] Further deferred groups and final exact private-export duplicate check.
- Phone PSS/ranking remain; private cleanup local-only (import cannot remove duplicates).
  Actions <=60s TOTAL/run; no merge/release; all dictionary/SI/editor/gesture bounds retained.


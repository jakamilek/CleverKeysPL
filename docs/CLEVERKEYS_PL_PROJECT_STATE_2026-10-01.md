# CleverKeys PL — wspólny stan projektu

Data migracji: 2026-10-01

## Model projektu

Aplikacja/runtime oraz polski pakiet językowy są od tego punktu traktowane jako jeden wspólny, zależny projekt eksperymentalny.

## Aktywne repozytorium integracyjne

jakamilek/CleverKeysPL

Gałąź:
exp/unified-pl-project-2026-10-01

Finalny HEAD stanu migracji:
d4876a406a7d3b37075d7295e0ec443d49711287

## Podstawa runtime przed migracją

6d101df98e28dbcb762cff39f193ebd3e1d644dc

Migracja ma dwa commity:
cf7c2bc52db64f4e7cabfadff5f506704d3ecdfd — integracja snapshotu
d4876a406a7d3b37075d7295e0ec443d49711287 — zachowanie trybu wykonywalnego skryptów

## Snapshot pakietu PL

Źródło:
jakamilek/CleverKeys-langpack-pl

Gałąź źródłowa:
ops/baseline-sync-2026-09-20

Dokładny source commit:
8862b31044695f0f28a8667ec851d1ba5b278dd3

Umiejscowienie w monorepo:
langpack-pl/

Snapshot zawiera 123 pliki. Drzewo snapshotu zachowuje identyczny SHA drzewa:
1756a24210cf1a9c3593caa0415557986a7c4b40

Treść plików pakietu nie została zmieniona podczas migracji.

## Zasada aktywnego rozwoju

Dalszy rozwój wspólnego eksperymentu powinien odbywać się w jakamilek/CleverKeysPL na odpowiednich gałęziach eksperymentalnych. Oryginalne repozytorium pakietu PL pozostaje zachowane jako historyczne źródło pochodzenia.

Nie należy utrzymywać dwóch rozchodzących się aktywnych kopii projektu.

## GitHub Actions

Workflowy pakietu zostały zachowane w langpack-pl/.github/workflows/ jako część snapshotu. Zagnieżdżone workflowy nie są automatycznie wykonywane przez GitHub Actions. Ich przeniesienie/adaptacja do głównego .github/workflows/ jest osobnym zadaniem.

## Zero domysłów

Obowiązuje dokument:
langpack-pl/docs/PROJECT_RULE_NO_GUESSING_2026-10-01.md

Nie wolno zastępować weryfikacji aktualnego GitHubu pamięcią rozmowy ani przypuszczeniem.

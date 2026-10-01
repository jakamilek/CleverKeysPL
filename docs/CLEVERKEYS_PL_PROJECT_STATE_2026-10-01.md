# CleverKeys PL — wspólny stan projektu

Data migracji: 2026-10-01

## Model projektu

Od tego punktu eksperyment jest traktowany jako jeden wspólny projekt: aplikacja CleverKeys + polski pakiet językowy. Obie części są zależne od siebie.

## Repozytorium aktywne

jakamilek/CleverKeysPL

Bieżąca gałąź integracyjna:
exp/unified-pl-project-2026-10-01

Gałąź została utworzona z runtime HEAD:
6d101df98e28dbcb762cff39f193ebd3e1d644dc

## Snapshot pakietu PL

Źródło:
jakamilek/CleverKeys-langpack-pl

Źródłowa gałąź:
ops/baseline-sync-2026-09-20

Dokładny źródłowy commit:
8862b31044695f0f28a8667ec851d1ba5b278dd3

Snapshot został umieszczony w tym repozytorium pod:
langpack-pl/

Nie zmieniono treści plików pakietu podczas migracji.

## Reguła ważności

Ten snapshot jest kopią stanu z dokładnego SHA powyżej. Dalszy rozwój tego wspólnego eksperymentu powinien odbywać się w aktywnym repozytorium jakamilek/CleverKeysPL.

Oryginalne repozytorium pakietu PL pozostaje zachowane jako historyczne źródło pochodzenia snapshotu. Nie należy tworzyć dwóch aktywnych, rozchodzących się wersji tych samych zmian.

## Ważne

Pakietowe workflowy GitHub zachowano wewnątrz langpack-pl/.github/workflows/ jako część snapshotu. GitHub Actions nie uruchamia automatycznie workflowów znajdujących się w zagnieżdżonym katalogu. Ich integracja z głównym .github/workflows/ jest osobnym krokiem i nie została tutaj założona jako wykonana.

## Zero domysłów

Obowiązuje:
langpack-pl/docs/PROJECT_RULE_NO_GUESSING_2026-10-01.md

Nie wolno traktować pamięci rozmowy jako źródła faktów tam, gdzie można zweryfikować aktualny Git.

## Provenance

Migracja zachowuje:
- pełny bieżący snapshot plików pakietu PL
- źródłowe ścieżki i zawartość
- dokładny source commit SHA
- możliwość porównania z oryginalnym repozytorium

Nie próbowano tworzyć sztucznej historii Git z parentem pochodzącym z innego repozytorium.

# Kamień milowy — dłuższy kontekst i prototyp wariantów (2026-10-02)

**Status:** eksperyment offline, bez integracji Android.
**Data weryfikacji:** 2026-10-02, Europe/Warsaw.
**Raport kanoniczny:** [CleverKeys-langpack-pl](https://github.com/jakamilek/CleverKeys-langpack-pl/blob/c5e77a6b9c4f18b0b1a2b2e615d56f1985583e98/docs/PROJECT_MILESTONE_2026-10-02_CONTEXT_SURFACE_PROTOTYPE.md).
**Rekord cross-repo:** ten dokument ma odpowiednik o tej samej nazwie w langpacku.

## Baseline przed tym zapisem

- Runtime main: 56e7e90d3a669f2eb28e65257ce6851aa7b5d925.
- Langpack main przed milestone’em: 0832bf6f1353b0a2bc3042dbdd26f330ddc53e33.
- Langpack commit milestone’u: c5e77a6b9c4f18b0b1a2b2e615d56f1985583e98.
- Eksperyment: experiment/context-surface-window-v1, commit 2733671896b6d9a13af9907217011fdc20f0066e, [Draft PR #4](https://github.com/jakamilek/CleverKeys-langpack-pl/pull/4).

## Doprecyzowanie użytkownika

Dwa poprzednie lowercase słowa są za krótkim kontekstem. Użytkownik zatwierdził dalszą pracę nad dłuższą historią i wariantami. Parametry eksperymentu 64 słowa / 4096 znaków są konfigurowalnym punktem startowym, nie zatwierdzonym optimum ani ustawieniem aplikacji.

## Zrealizowane

W langpacku dodano izolowane experiments/context_surface_v1: dokładny suffix tekstu przed kursorem, case/diakrytyka/interpunkcja/nowe linie, podzbiór sidecara v1, wybór wariantu wewnątrz klucza i dostępne alternatywy. Kontekst może obejmować wcześniejsze zdania w limicie.

Żądania porównują 2 słowa z dłuższym oknem i nie zawierają gold labels. Model response jest powiązany z request hash, odrzuca stare/niepełne/wymyślone warianty. Fixture jest testowy, nie produkcyjnym sidecarem ani wynikiem audytu 100k.

30/30 testów lokalnie PASS. CLI prepare/evaluate PASS. Powtórny output bytes/SHA identyczny. [Push CI eksperymentu](https://github.com/jakamilek/CleverKeys-langpack-pl/actions/runs/37044611353): completed, success.

Neutralny baseline 14 przykładów: poprawny klucz top-1 13/14, poprawna powierzchnia top-1 10/14, oczekiwana powierzchnia dostępna 13/14. Oba okna dają to samo, ponieważ baseline nie interpretuje kontekstu. Wyniki controlled_test nie są inferencją AI.

## Architektura i odrzucone skróty

Oddzielne repozytoria, wspólny runtime, immutable 100k, CKDT V2 i legacy pozostają wiążące. Prototyp nie zmienia lexical rank/engineScore ani API v1. To narzędzie offline, nie drugi produkcyjny polski reranker.

Nie zastosowano dwóch wpisów CKDT, keyword heurystyk udających AI ani surowych logitów jako pewności zmiany rank 1. Nie promowano eksperymentu do main. Główny runtime nadal ma dwutokenowy kontekst — dłuższe okno istnieje w prototypie, nie w aplikacji.

## Niewdrożone i niezweryfikowane

Rzeczywisty model cased i adapter inferencji, pomiary jakości AI oraz koszt telefonu pozostają do wykonania. Nie zmieniono generatora preview, membership, ZIP, ustawień ani kodu Android. Nie wykonano builda APK/testu urządzenia. Nie rozwiązano CTC ł.

Hash jest ochroną spójności offline, nie pełnym mechanizmem aktualności kursora. Boundary cues to pozycje interpunkcji, nie segmenter skrótów. Słowa Unicode != subword tokens modelu. Brakującego słowa w slate nie odzyskuje sam resolver.

## Historia i luki

Branche docs/architecture-runtime-langpack-separation-2026-10-02 i docs/architecture-langpack-plugin-model-2026-10-02 zachowują wcześniejszą rolę; bez merge. Niespójność referencji ADR, brak pl.cklm i provider/parser v1 pozostają otwarte. FAIL historycznego S3 angielskiego statycznego LM nie został unieważniony tym eksperymentem.

## Delta i kolejny krok

Od CONTEXT_AND_SURFACE_AUDIT dodano wymaganie dłuższego kontekstu, wykonywalny prototyp, 30 testów, baseline, workflow i Draft PR. Zmiany tego repozytorium są dokumentacyjne.

Następnie rzeczywisty adapter gotowego modelu z przypiętą rewizją, porównanie długości kontekstu i casing-u, potem większy niezależny zbiór rzeczywistych slates i koszt Androida. Dopiero wyniki uzasadniają integrację wspólnego runtime i test commit/tap-to-replace.

Bieżący sukces CI dotyczy wyłącznie eksperymentu langpacku. Nie oznacza green runtime; poprzedni audyt raportował failure CI baseline’u 83c8f6fe (lint/security) obok sukcesu APK. Status nowych dokumentacyjnych commitów trzeba odczytać oddzielnie.

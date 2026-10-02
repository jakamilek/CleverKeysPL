# Kamień milowy — audyt kontekstu i wariantów powierzchni (2026-10-02)

**Status:** ukończony audyt statyczny; propozycje do eksperymentu, bez zmian runtime.
**Data:** 2026-10-02, Europe/Warsaw.
**Raport kanoniczny:** [CleverKeys-langpack-pl / audyt](https://github.com/jakamilek/CleverKeys-langpack-pl/blob/0832bf6f1353b0a2bc3042dbdd26f330ddc53e33/docs/PROJECT_MILESTONE_2026-10-02_CONTEXT_AND_SURFACE_AUDIT.md).
**Powiązany dokument w drugim repozytorium:** ten sam plik milestone’u.
**Commit raportu kanonicznego:** 0832bf6f1353b0a2bc3042dbdd26f330ddc53e33.

## Snapshot przed zapisaniem milestone’u

- Runtime main: 83c8f6fe201fd1db3de9365a3d1c6c9bea6fdb69.
- Langpack main, audytowane źródła: 16a6835bdb3ed415667c63ecee6cc6b1a283c1fb.
- Upstream main: 01b6212d92d96dd8943145a7b6cef2be53c9fe3c.
- HEAD-y ponownie sprawdzono przed zapisem; bez zmian względem baseline’u rekonstrukcji.

## Ustalenia

1. Kontrakt v1 już przewiduje jeden surfaceKey i wiele casing variants. Użytkownik doprecyzował potrzebę wyboru zapisu w kontekście i zachowania alternatywy; nie wymaga dwóch wpisów CKDT.
2. BigramModel / StaticContextLm wpływa na scoring tap i next-word. Reranking swipe pobiera evidence z lokalnego ContextModel; default swipe_context_rescoring=false.
3. UnifiedScore.combine jest w SuggestionProvenance.kt; both wybiera max(static, learned).
4. Aktualne statyczne i uczone n-gramy oraz historia dwutokenowa tracą casing, więc nie rozróżniają Łódź/łódź.
5. SwipeContextRescorer permutuje istniejące indeksy, nie tworzy wariantów. Po leksykalnej deduplikacji potrzebny jest resolver powierzchni i zachowanie dokładnego wariantu przy commit.
6. S3 statycznego angielskiego LM dla swipe zakończyło się FAIL; to nie jest dowód porażki polskiego modelu, ale blokuje obietnicę prostego przeniesienia tego LM jako zweryfikowanej poprawy.
7. CtcAzProjection nie mapuje ł -> l: łódź/Łódź są odrzucane z tej projekcji trie. Eligibility całego ZIP PL nie było mierzone. AccentNormalizer ma takie mapowanie, ale nie jest użyty przez CtcAzProjection.
8. Gotowe NKJP n-gramy (1..5) istnieją, lecz są lowercase. Gotowe polskie modele cased/denoising są kandydatami do badania offline; nie sprawdzono inferencji, eksportu ani kosztu Androida.

## Architektura wiążąca i rekomendacje

Pozostają oddzielne repozytoria, immutable 100k, CKDT V2, legacy fallback i wspólny runtime. Langpack dostarcza dane/model; runtime je wykonuje i ocenia wyłącznie kandydatury dekodera. Atrybuty nie są same w sobie modelem intencji.

Propozycja, nie wdrożona decyzja: ocena dopuszczalnych wariantów w kontekście, osobny wynik klucza i zapisu, opcjonalne analizy powiązane z wariantami, osobny kontekst zachowujący case i granice zdania. lookup(surface) z v1 dostarcza wiedzę, lecz nie jest kontekstowym scorerem.

Odrzucone jako wystarczające: dwie pozycje CKDT różniące się case; same flagi common/proper; sam lowercase CKLM; ponowne wpięcie statycznego LM do swipe bez walidacji; utożsamienie model.onnx (encoder swipe) z modelem kontekstu.

## Kolejny krok

Izolowany prototyp wariantów dla istniejących kandydatur i zbiór ewaluacyjny z lewym kontekstem. Porównać neutralny metadata resolver i adapter gotowego cased modelu, mierząc słowo/casing oddzielnie, fixed/broken oraz osiągalność alternatywy. Najpierw rozwiązać osiągalność łódź w wybranym dekoderze. Bez treningu dużego modelu od zera.

## Luki i rzeczy niezweryfikowane

Parser/provider v1, sidecar, resolver, cased scorer i uczenie casing-u są niewdrożone. Nie zmieniono scoringu, ZIP ani generatora. Brak pl.cklm. Nie wykonano testów Kotlin, builda lokalnego lub pomiarów telefonu. Potwierdzono zachowanie Unicode NFD i prześledzono źródła/testy. Nie ma nowych pomiarów skuteczności.

Historyczne branche docs/architecture-runtime-langpack-separation-2026-10-02 i docs/architecture-langpack-plugin-model-2026-10-02 pozostają opisane w poprzednim milestone’ie; bez merge/promocji. Niespójność referencji do dwóch ADR pozostaje otwarta.

## Actions baseline’u i delta

Pełne Actions runs z filtrem head_sha, obejmujące push:
- Langpack [build](https://github.com/jakamilek/CleverKeys-langpack-pl/actions/runs/37032314426): success.
- Runtime [APK build](https://github.com/jakamilek/CleverKeysPL/actions/runs/37031983557): success.
- Runtime [CI](https://github.com/jakamilek/CleverKeysPL/actions/runs/37031983558): failure. Jobs API wskazuje lint oraz Security Scan / fixed HIGH/CRITICAL gate; logów przyczyn nie diagnozowano.

Zmiana od STATE_RECONSTRUCTION: audyt kodowych punktów integracji i wariantów, korekta uproszczonego opisu statycznego LM dla swipe, potwierdzenie źródeł gotowych modeli, identyfikacja ł w CTC i weryfikacja push CI. Wyłącznie dokumentacja. Nowe commity przesuwają HEAD; ich CI trzeba sprawdzać osobno.

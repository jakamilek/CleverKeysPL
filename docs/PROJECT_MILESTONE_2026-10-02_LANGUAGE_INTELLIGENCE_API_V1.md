# Kamień milowy — finalizacja Language Intelligence API v1

**Data:** 2026-10-02  
**Typ:** decyzja architektoniczna + audyt danych  
**Poprzedni kamień:** `docs/PROJECT_MILESTONE_2026-10-02_RUNTIME_LANGPACK_AUDIT.md`  
**Następny etap:** implementacja minimalnego providera i transportu danych

## 0. Zweryfikowany stan

Po zapisaniu finalnego kontraktu bieżący `main`:

- `jakamilek/CleverKeysPL`: HEAD `e26d70da385e20a175ecf9e1884233746587ca3d`
- `jakamilek/CleverKeys-langpack-pl`: HEAD `168f1f83a7df49e572518f1c87037485bc5bee5d`

W tym etapie zmieniono wyłącznie dokumentację. Kod runtime i generatora nie został zmieniony.

## 1. Wynik audytu rzeczywistych danych

Potwierdzono, że:

- CKDT V2 zawiera kanoniczne powierzchnie oraz rangę częstotliwości;
- obecny loader runtime udostępnia WordPredictor głównie jako `Map<String, Int>`;
- pipeline PL oblicza dodatkową wiedzę podczas CI: Morfeusz, kapitalizacja, nazwy własne, miejsca, TERC, państwa, stolice, proweniencja modułów;
- ta dodatkowa wiedza nie jest obecnie członkiem instalowanego artefaktu;
- bieżące `source/` zawiera starszy, mały zestaw plików i nie jest równoważne z produkcyjnym pipeline 100k/preview;
- UserDictionary pozostaje odrębnym źródłem runtime.

## 2. Zaakceptowany kontrakt v1

Finalny dokument:

`docs/LANGUAGE_INTELLIGENCE_API_V1_FINAL_2026-10-02.md`

Kontrakt logiczny:

`capabilities() -> packageInfo() -> lookup(surface)`

Provider:

- nie generuje kandydatów;
- nie wykonuje I/O w `lookup`;
- jest tylko do odczytu;
- zwraca brak informacji jako brak/nullable, a nie sztuczne `false`;
- może łączyć dane CKDT i dodatkowy człon inteligencji.

Model logiczny obejmuje:

- `surfaceKey`;
- `canonicalForm`;
- `frequencyRank`;
- `morphology`;
- `capitalization`;
- `commonNoun`;
- `properName`;
- `metadata`.

## 3. Zaakceptowany transport v1

Opcjonalny człon pakietu:

`language-intelligence.json`

Manifest nowego pakietu może deklarować:

- `apiVersion: 1`;
- `capabilities`;
- `languageIntelligence.file`;
- `languageIntelligence.schemaVersion`;
- SHA256 pliku inteligencji.

CKDT V2 pozostaje bez zmian.

Człon inteligencji jest **uzupełnieniem**, nie kopią całego słownika. Rekordy mogą być rzadkie/sparse („rzadkie, tylko tam, gdzie jest dodatkowa wiedza”).

## 4. Kapitalizacja i homonimia

Utrwalono:

- jeden `surfaceKey` może mieć wiele wariantów powierzchni;
- `Łódź` / `łódź` nie są dwiema pozycjami CKDT;
- common noun ma pierwszeństwo przed proper name;
- przymiotnik nie jest automatycznie kapitalizowany przez pochodzenie od nazwy własnej;
- brak wiedzy nie jest równoznaczny z `false`.

## 5. Kompatybilność

### Legacy

Pakiet bez `apiVersion` i bez członu inteligencji działa jako pakiet legacy.

### v1

Pakiet deklarujący człon inteligencji musi:

- zawierać wskazany plik;
- przejść walidację;
- przejść kontrolę SHA256.

Nieznane capability można ignorować.

Nieobsługiwany główny `apiVersion` nie może być po cichu traktowany jako stary format.

## 6. Istotne elementy istniejącego runtime

Pozostają bez zmian:

- `LanguagePackManager` jako punkt instalacji pakietów;
- `BinaryDictionaryLoader` jako obsługa CKDT V1/V2;
- `Predictor` jako istniejąca granica runtime;
- `SwipeContextRescorer` jako jedyny context reranker;
- UserDictionary jako osobne źródło.

## 7. Odrzucone kierunki

Nadal odrzucone:

- monorepo;
- hurtowne scalanie gałęzi eksperymentalnych;
- drugi polski reranker;
- API jako generator słów;
- wkładanie UserDictionary do oficjalnego provider-a;
- duplikowanie kluczy CKDT tylko dla casing-u.

## 8. Niezaimplementowane

Nie zostały jeszcze wykonane:

1. klasy/model runtime;
2. legacy provider;
3. parser i walidator `language-intelligence.json`;
4. rozszerzenie LanguagePackManager;
5. generator inteligencji PL;
6. wpięcie metadanych do przepływu kandydatów;
7. testy kompatybilności i dual-casing.

## 9. Ograniczenia

- brak jeszcze produkcyjnego sidecaru inteligencji;
- brak jeszcze potwierdzonej wydajności i zużycia pamięci;
- zakres coverage poszczególnych capability nie został jeszcze zmierzony na nowym formacie;
- historyczny artifact/run 382 pozostaje historyczny, a nie bieżący.

## 10. Następny krok techniczny

Implementować etap minimalny:

`LanguageIntelligence` model -> legacy provider -> manifest/file validation -> PL sidecar generator -> runtime lookup adapter -> testy.

Najpierw testy i kompatybilność; dopiero potem użycie danych inteligencji w scoringu.

## 11. Różnica względem poprzedniego kamienia

Poprzednio istniała tylko propozycja API i lista luk.

Teraz:

- luka danych została potwierdzona na bieżącym `main`;
- kontrakt logiczny v1 jest zamknięty;
- ustalony jest opcjonalny sidecar `language-intelligence.json`;
- ustalony jest manifestowy mechanizm capability + SHA;
- można rozpocząć implementację bez powrotu do historycznego audytu gałęzi.

## 12. Wejście dla następnej instancji

Przeczytać najpierw:

1. `docs/PROJECT_CONTINUITY_RULE_MILESTONES.md`;
2. ten kamień milowy;
3. `docs/LANGUAGE_INTELLIGENCE_API_V1_FINAL_2026-10-02.md`.

Następnie zweryfikować bieżące HEAD-y obu `main`. Nie analizować historycznych gałęzi bez wskazania konfliktu lub potrzeby implementacyjnej.

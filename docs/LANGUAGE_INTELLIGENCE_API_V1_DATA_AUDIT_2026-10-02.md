# Language Intelligence API v1 — runtime/data integration audit 2026-10-02

## Zweryfikowany stan

- repozytorium: `jakamilek/CleverKeysPL`
- audytowany ref: `main`
- HEAD: `c6fa9f0b3b8c8976cefa2b0b34e39504f121bec4`
- branch dokumentacyjny: `docs/language-intelligence-api-v1-2026-10-02`

## 1. Aktualny importer pakietów

`LanguagePackManager` przyjmuje ZIP, którego wymagane elementy to:

- `manifest.json`
- `dictionary.bin`

Importer dodatkowo rozpoznaje:

- `unigrams.txt`
- `contractions.json`
- `prefix_boost.bin`
- `NOTICE.txt`
- opcjonalny deklarowany `model.onnx`.

Manifest jest mapowany do `LanguagePackManifest` z polami:

- `code`
- `name`
- `version`
- `author`
- `wordCount`
- `hasPrefixBoost`
- opcjonalnie model i attribution.

Brak pól API v1 dla:

- `apiVersion`
- `capabilities`
- `provenance`
- morphology
- capitalization
- commonNoun
- properName.

## 2. Istniejący mechanizm kontekstowy

`SuggestionHandler.rescoreWithContext()` już posiada integrację z:

`WordPredictor.getSwipeContextEvidence()` → `SwipeContextRescorer.rescoreOrder()`.

Mechanizm:

- działa na kandydacie już wygenerowanym przez runtime,
- może zmienić kolejność istniejącej listy,
- nie generuje nowych słów,
- jest niezależny od formalnego API językowego.

Nie należy budować drugiego, równoległego mechanizmu rerankingu.

## 3. Integracja słownika z langpack

`WordPredictor` pobiera z `LanguagePackManager`:

`getDictionaryPath(language)`

i ładuje wskazany `dictionary.bin` przez istniejący loader CKDT.

Runtime posiada również:

`getUnigramsPath(language)`

dla `unigrams.txt`.

To oznacza, że obecny punkt integracji jest artefaktowy: runtime konsumuje już pliki z zewnętrznego ZIP-a, ale zna wyłącznie istniejące formaty.

## 4. Niezgodność z aktualnym langpack main

Zweryfikowany `jakamilek/CleverKeys-langpack-pl/main` używa obecnie:

- `scripts/build_pl.sh`,
- `source/*.dic`,
- `source/frequency.csv`,
- `source/priorities.csv`,
- `source/bigrams.csv`,

a nie produkuje `dictionary.bin` w schemacie wymaganym przez `LanguagePackManager`.

Dodatkowo root manifest langpack ma inny schemat niż manifest oczekiwany przez runtime.

**Wniosek:** przed implementacją API v1 musi powstać zweryfikowany most pomiędzy źródłami PL a istniejącym formatem artefaktu runtime.

## 5. Mapowanie odpowiedzialności

| Funkcja | Runtime main | Langpack main |
|---|---|---|
| import ZIP | tak | nie |
| walidacja CKDT `dictionary.bin` | tak | nie |
| ładowanie słownika | tak | źródła słownika |
| unigrams | tak, jeśli pakiet je zawiera | nie w obecnym `build_pl.sh` |
| morphology | brak | brak gotowego eksportu |
| capitalization | brak formalnego API | reguły w dokumentacji, brak artefaktu |
| common/proper-name | brak formalnego API | brak artefaktu na `main` |
| context reranking | istnieje | nie należy do langpack |
| provenance | manifest/NOTICE dla istniejących pakietów | częściowo opisana w dokumentacji źródeł |

## 6. Konsekwencja architektoniczna

Docelowy model pozostaje:

`CleverKeysPL` → Language Intelligence API → wersjonowany artefakt → `CleverKeys-langpack-pl`.

Jednak przed implementacją należy najpierw zbudować rzeczywisty kontrakt danych na podstawie zweryfikowanych źródeł PL. Runtime nie powinien zgadywać brakujących informacji ani wykonywać analizy Morfeusza/Hunspell zamiast pakietu.

## 7. Następny krok

Nie implementować jeszcze parsera API v1.

Najpierw:

1. zidentyfikować i zweryfikować docelowy generator artefaktu PL,
2. zmapować istniejące źródła do rekordu v1,
3. określić minimalny zestaw capability,
4. określić fallback dla brakujących capability,
5. dopiero wtedy dodać parser i testy runtime.

Audyt nie zmienia `main`.

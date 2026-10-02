# Language Intelligence API v1 — finalny kontrakt runtime ↔ langpack

**Data:** 2026-10-02  
**Status:** zaakceptowany kontrakt logiczny do implementacji  
**Zakres:** kontrakt runtime, dane językowe i kompatybilność; bez zmiany kodu w tym commicie

## 1. Wynik audytu wejściowego

Audyt bieżącego `main` potwierdził:

- `CleverKeysPL` już importuje zewnętrzne pakiety ZIP;
- CKDT V2 przechowuje kanoniczne powierzchnie i rangi częstotliwości;
- bieżący `BinaryDictionaryLoader` materializuje dla WordPredictor przede wszystkim `Map<String, Int>`;
- obecny manifest runtime zna `code`, `name`, `version`, `author`, `wordCount`, `hasPrefixBoost` oraz opcjonalne `model` i dane licencyjne;
- audyty kapitalizacji, nazw własnych, miejsc i Morfeusz generują dodatkową wiedzę podczas budowy pakietu PL, ale ta wiedza nie jest obecnie członkiem instalowanego ZIP-a;
- bieżący ZIP PL ma przez to charakter słownikowy, a nie pełnej warstwy inteligencji językowej.

Bieżące źródła kodu zweryfikowane na `main`:

- `LanguagePackManager.kt` — SHA `afb0b4b4a8525a5f8f147c0e6e7ad4d2dc4e2597`
- `BinaryDictionaryLoader.kt` — SHA `2f887680cd9cc913648921c385ea3b9acf67b75a`
- `WordPredictor.kt` — SHA `70b6ffe89c64d971ccb4c6ad2b9f731324340399`
- `PredictionResult.kt` — SHA `3584ee89b0e0f9ebeb55dceb8a6112eae244e117`

## 2. Zasada kontraktu

Language Intelligence API **nie generuje kandydatów**.

Przepływ pozostaje:

`decoder -> kandydaci -> LanguageIntelligenceProvider -> istniejący context reranking -> prezentacja/commit`

Provider jest dostawcą wiedzy o już istniejącym kandydacie. Nie jest drugim predyktorem, korektorem ani polskim rerankerem.

## 3. Minimalny interfejs logiczny

Runtime otrzymuje trzy operacje logiczne:

```text
capabilities() -> Set<Capability>
packageInfo() -> PackageInfo
lookup(surface) -> LanguageIntelligence?
```

Wymagania:

- `lookup` jest szybkim, synchronicznym odczytem z już załadowanej struktury;
- provider jest tylko do odczytu i bezpieczny dla równoległych odczytów;
- `lookup` nie wykonuje I/O, nie uruchamia Morfeusza/Hunspell i nie pobiera danych sieciowych;
- brak rekordu oznacza „brak informacji”, a nie `false`;
- capability opisują możliwości pakietu, nie zobowiązują do pełnego pokrycia każdego słowa;
- provider może łączyć dane z CKDT z dodatkowym plikiem inteligencji.

## 4. Model informacji kandydata

Logiczny model v1:

```text
LanguageIntelligence
  surfaceKey: String
  canonicalForm: String?
  frequencyRank: Int?
  morphology: MorphologyInfo?
  capitalization: CapitalizationInfo?
  commonNoun: Boolean?
  properName: Boolean?
  metadata: Map<String, String>
```

### 4.1 surfaceKey

`surfaceKey` jest kluczem porównania powierzchniowego używanym przez provider:

- Unicode;
- case-insensitive przez `lowercase()`;
- zachowuje polskie znaki diakrytyczne;
- nie jest tym samym co odarcie akcentów stosowane wewnętrznej części CKDT.

Nie wolno traktować różnych przypadków tego samego klucza jako dwóch niezależnych tożsamości leksykalnych.

### 4.2 canonicalForm

Opcjonalna kanoniczna forma leksykalna, np. forma hasłowa dla odmiany.

Brak pola nie jest błędem.

### 4.3 frequencyRank

Logicznie część wiedzy API, ale nie musi być duplikowana w dodatkowym pliku. Dla bieżącego PL provider może pobierać rangę bezpośrednio z CKDT V2.

### 4.4 morphology

Model neutralny językowo:

```text
MorphologyInfo
  pos: String?
  features: Map<String, String>
```

Runtime nie uzależnia API od Morfeusza, Hunspell ani konkretnego tagsetu. Pakiet może przechowywać tagi własne lub mapowanie do wspólnego zestawu.

### 4.5 capitalization

```text
CapitalizationInfo
  defaultSurface: String?
  variants: List<SurfaceVariant>
  
SurfaceVariant
  surface: String
  casePolicy: lowercase | capitalized
```

Warianty są wariantami powierzchni jednego klucza leksykalnego.

Dla homonimów dopuszczalne jest równoczesne:

```text
commonNoun = true
properName = true
```

Przykładowo `łódź` i `Łódź` nie stają się dwiema pozycjami CKDT.

### 4.6 commonNoun / properName

Pola są trójstanowe logicznie:

- `true` — potwierdzono;
- `false` — potwierdzono brak;
- brak pola — nie wiadomo.

Obowiązuje reguła polska:

**rzeczownik pospolity ma pierwszeństwo przed sygnałem nazwy własnej.**

Przymiotnik nie jest automatycznie kapitalizowany tylko dlatego, że pochodzi z nazwy własnej. Pakiet dostarcza wynikową politykę powierzchni.

### 4.7 metadata

Dodatkowe metadane są mapą tekstową, np. moduł, źródło, typ wpisu lub proweniencja.

Runtime nie zakłada konkretnego zestawu kluczy.

## 5. Transport danych v1

Nowy pakiet może zawierać opcjonalny człon:

```text
language-intelligence.json
```

Jego logiczny dokument:

```json
{
  "schemaVersion": 1,
  "languageCode": "pl",
  "entries": [
    {
      "surfaceKey": "łódź",
      "canonicalForm": "łódź",
      "capitalization": {
        "defaultSurface": "łódź",
        "variants": [
          {"surface": "łódź", "casePolicy": "lowercase"},
          {"surface": "Łódź", "casePolicy": "capitalized"}
        ]
      },
      "commonNoun": true,
      "properName": true,
      "morphology": {
        "pos": "noun",
        "features": {}
      },
      "metadata": {
        "module": "city"
      }
    }
  ]
}
```

Powyższy zapis jest kontraktem danych, a nie wymaganiem konkretnego sposobu implementacji indeksu w pamięci.

Producent może generować zapis tylko dla kluczy posiadających dodatkową wiedzę; brak wpisu oznacza brak dodatkowych informacji.

## 6. Manifest v1

Nowy pakiet deklarujący inteligencję zawiera dodatkowo:

```json
{
  "apiVersion": 1,
  "capabilities": [
    "lexicon",
    "frequency",
    "morphology",
    "capitalization",
    "common_noun",
    "proper_name",
    "metadata"
  ],
  "languageIntelligence": {
    "file": "language-intelligence.json",
    "schemaVersion": 1,
    "sha256": "<64 hex>"
  }
}
```

Istniejące pola manifestu pozostają bez zmian.

`apiVersion` wersjonuje kontrakt. `version` pozostaje wersją konkretnego pakietu danych. `schemaVersion` wersjonuje fizyczny dokument danych.

## 7. Kompatybilność

### Stary pakiet

Jeżeli `apiVersion` i `languageIntelligence` nie występują:

- pakiet nadal działa;
- provider udostępnia tylko wiedzę, którą runtime już posiada z CKDT;
- opcjonalne pola pozostają nieznane.

### Nowy pakiet

Jeżeli manifest deklaruje `languageIntelligence`:

- plik musi istnieć;
- SHA musi się zgadzać;
- nieprawidłowy format oznacza odrzucenie pakietu.

### Przyszłe capability

Nieznana capability nie może spowodować awarii. Runtime może ją zignorować.

Nieznany główny `apiVersion` nowszy od obsługiwanego nie powinien być po cichu traktowany jako v1; import musi zostać odrzucony jako nieobsługiwany kontrakt.

## 8. Odpowiedzialność runtime

Runtime:

- importuje i waliduje pakiet;
- ładuje provider;
- wzbogaca tylko istniejących kandydatów;
- udostępnia metadata istniejącemu rankingowi/context rerankingowi;
- wybór kolejności pozostawia istniejącemu mechanizmowi rankingowemu;
- nie uruchamia źródeł budowania PL.

`SwipeContextRescorer` pozostaje jedynym istniejącym mechanizmem context rerankingu.

## 9. Odpowiedzialność langpacku PL

Pakiet PL:

- wyznacza canonicalForm;
- dostarcza zweryfikowaną morfologię;
- dostarcza wynikową politykę kapitalizacji;
- dostarcza common-noun/proper-name;
- dostarcza warianty powierzchni;
- zapisuje proweniencję i metadane;
- buduje deterministyczny artefakt.

Morfeusz, Hunspell, wordfreq, NKJP1M i AOSP pozostają narzędziami/source oracles („narzędzia/źródła referencyjne”) procesu budowy. Nie są zależnościami runtime.

## 10. Czego nie robimy

Nie:

- dublujemy słownika w dwóch formatach tylko po to, aby przenieść frequency;
- nie tworzymy drugiego generatora kandydatów;
- nie tworzymy polskiego rerankera;
- nie umieszczamy UserDictionary w oficjalnym providerze;
- nie rozwiązujemy dual-casing przez dwie identyczne pozycje CKDT;
- nie wymagamy pełnego coverage („pokrycia”) morfologii jako warunku działania pakietu.

## 11. Warunki implementacji

Następna faza może już obejmować kod, ale w kolejności:

1. model `LanguageIntelligence` + capability;
2. provider legacy oparty o CKDT;
3. parser/walidator opcjonalnego `language-intelligence.json`;
4. rozszerzenie `LanguagePackManager` o nowy człon;
5. adapter PL generujący dane z istniejących audytów;
6. wpięcie wzbogacenia kandydatów przed istniejącym context rerankingiem;
7. testy starego pakietu, nowego pakietu, fallbacku i wspólnego klucza casingowego;
8. dopiero po testach ewentualna optymalizacja transportu na binarny zapis bez zmiany kontraktu logicznego.

## 12. Kryterium gotowości

Implementacja v1 jest kompletna, gdy:

- stary pakiet nadal działa;
- nowy pakiet deklaruje `apiVersion=1`;
- provider zwraca dane tylko dla istniejących kandydatów;
- `Łódź` / `łódź` mają jedną tożsamość i różne warianty powierzchni;
- common noun nie jest nadpisywany przez proper name;
- przymiotnik pochodzący od nazwy własnej nie jest automatycznie kapitalizowany;
- brak capability nie powoduje wyjątku;
- UserDictionary pozostaje osobnym źródłem;
- wszystkie nowe dane są weryfikowalne z artefaktu i źródeł budowy.

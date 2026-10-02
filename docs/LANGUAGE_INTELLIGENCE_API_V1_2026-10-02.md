# Language Intelligence API v1 — kontrakt runtime ↔ langpack

**Status:** propozycja architektoniczna do implementacji po akceptacji kontraktu
**Data:** 2026-10-02

## 1. Cel
Minimalny kontrakt między uniwersalnym runtime CleverKeys a zewnętrznym pakietem językowym. Architektura pozostaje rozdzielona:
CleverKeysPL → Language Intelligence API → wersjonowany artefakt pakietu → CleverKeys-langpack-pl

## 2. Granice odpowiedzialności
Runtime: Android/UI, przepływ wejścia, swipe/decoder, generowanie kandydatów, ranking, końcowe sortowanie oraz istniejący mechanizm context rerankingu.
Langpack: słownik, częstotliwości/priorytety, morfologia, kapitalizacja, wiedza common noun („rzeczownik pospolity”) / proper name („nazwa własna”), nazwy własne i miejsca, metadane oraz wersjonowany artefakt danych.

## 3. Zasada nadrzędna
API nie generuje nowych kandydatów. Runtime najpierw tworzy kandydatów, a Language Intelligence dostarcza wiedzę potrzebną do ich interpretacji i rerankingu. Istniejący mechanizm SwipeContextRescorer pozostaje mechanizmem runtime.

## 4. Wersjonowanie
Pakiet deklaruje: languageCode, languageTag (opcjonalnie), packageVersion, apiVersion, identyfikację/proweniencję artefaktu oraz listę capability („obsługiwanych możliwości”). apiVersion wersjonuje kontrakt, packageVersion konkretny zestaw danych.

## 5. Minimalny model informacji kandydata
Logiczne pola: surface, canonicalForm (jeśli istnieje), frequency lub jawnie zdefiniowany sygnał leksykalny, morphology (jeśli dostępna), capitalization (jeśli dostępna), commonNoun (jeśli dostępne), properName (jeśli dostępne), metadata.
Dokładny format binarny/JSON nie jest jeszcze ustalony.

## 6. Kapitalizacja
Dla polskiego musi być możliwe wyrażenie preferencji lowercase („małe litery”), uppercase („wielkie litery”) oraz wieloznaczności wynikającej z homonimii.
Rzeczownik pospolity ma pierwszeństwo przed sygnałem proper-name. Przymiotniki nie mogą być automatycznie kapitalizowane tylko dlatego, że pochodzą z nazwy własnej.

## 7. Morfologia
API pozwala pakietowi przekazać wiedzę o formie fleksyjnej bez uzależniania runtime od konkretnego analizatora morfologicznego.

## 8. Context reranking
Warstwa kontekstowa runtime może otrzymać kontekst, listę już wygenerowanych kandydatów i informacje językowe o tych kandydatach, a następnie zmienić ich kolejność. API nie jest nieograniczonym generatorem słów.

## 9. Brak danych / kompatybilność
Brak opcjonalnej capability nie może powodować awarii. Starszy pakiet bez nowych capability ma działać przez fallback. Runtime nie zakłada capability, której pakiet nie deklaruje.

## 10. UserDictionary
Dane pakietu i Android UserDictionary/custom words są odrębnymi źródłami. API nie raportuje UserDictionary jako części oficjalnego langpacku.

## 11. Poza zakresem v1
Nie ustalamy jeszcze nazw klas Kotlin, konkretnego JSON schema, nowych plików, wag scoringowych, trenowania modelu ani konkretnej implementacji scorerów. Wymaga to audytu.

## 12. Warunek implementacji
Przed implementacją należy potwierdzić: obecny format artefaktu, manifest i kompatybilność, rzeczywiste dane morfologiczne/capitalization/proper-name, punkty integracji z aktualnym main runtime oraz testy kontraktu i fallbacku.

## 13. Reguły procesu
GitHub jest źródłem prawdy. ZERO DOMYSŁÓW. ACTION FIRST. Nie scalać repozytoriów. Nie przenosić eksperymentalnych branchy do main bez osobnej decyzji.
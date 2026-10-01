# POLISH CONTEXT CANDIDATE BENCHMARK

## Cel

Ten dokument definiuje pierwszy właściwy benchmark dla nowej eksperymentalnej warstwy kontekstowej języka polskiego.

Benchmark ma rozdzielać trzy pytania:

1. Czy dekoder swipe wygenerował właściwy kandydat?
2. Czy model kontekstowy potrafi wybrać właściwego kandydata spośród kandydatów, które rzeczywiście wygenerował dekoder?
3. Czy wynik można zastosować w czasie i przy zasobach odpowiednich dla klawiatury?

Model kontekstowy NIE jest generatorem leksykonu. Nie wolno tworzyć kandydatów wyłącznie przez model i następnie uznawać ich za wynik swipe.

## Obowiązujący przepływ danych

swipe trace
  ↓
rzeczywisty dekoder swipe
  ↓
PredictionResult(words, scores[, languages])
  ↓
zamknięty candidate set
  ↓
context scorer / reranker
  ↓
wybór kandydata
  ↓
morphology / surface
  ↓
casing
  ↓
sugestia / auto-insert

Punkt pomiarowy candidate-set musi znajdować się PRZED kontekstowym rerankingiem. Nie wolno używać końcowej listy paska jako substytutu surowego slate'u dekodera, ponieważ późniejsze etapy mogą dołączać lub przestawiać elementy.

## Stan przypiętego runtime

Przypięty runtime: tribixbite/CleverKeys @ 263bd0abc03dec420f60fa073a9d2c5e25a176b5.

W tym dokładnym runtime istnieje:

- GeometricSwipeEngine, który prowadzi ślad przez preprocessing → pruning → scoring → ranking i zwraca PredictionResult;
- PredictionResult(words, scores, languages?);
- SuggestionHandler.handleSwipePredictionResults jako wspólny punkt przetwarzania wyników swipe;
- SwipeContextRescorer, który przestawia już wygenerowany slate na podstawie kontekstu;
- gated accessor w WordPredictor dla dowodów kontekstowych;
- domyślne wyłączenie funkcji swipe_context_rescoring.

Wniosek: nie należy tworzyć drugiego konkurencyjnego rerankera. Najpierw trzeba zbudować polski benchmark offline i dostarczyć mu rzeczywiste slates z dekodera.

## Zasada „zero domysłów”

Brak danych oznacza NIEZNANE.

W szczególności nie wolno:

- traktować odległości edycyjnej jako prawdziwego wyniku dekodera;
- uznawać ręcznie zbudowanego candidate set za wynik produkcyjnego swipe;
- przenosić wyników angielskich na polski;
- uznawać braku słowa w core za częstotliwość zero;
- uznawać większego modelu za lepszy bez pomiaru.

Fallback edit-distance może służyć wyłącznie do testów samej warstwy rerankingu i musi być oznaczony jako przybliżenie.

## Format pojedynczego przypadku

Minimalny rekord:

| Pole | Znaczenie |
|---|---|
| id | stabilny identyfikator przypadku |
| context | słowa znajdujące się przed przewidywanym słowem |
| input | znormalizowany tekst/ślady wejściowe użyte do testu |
| candidate_set | kandydaci faktycznie zwróceni przez decoder; przed rerankingiem |
| candidate_scores | oryginalne score dekodera, równoległe do candidate_set |
| expected_candidate_key | poprawna tożsamość leksykalna |
| expected_surface | oczekiwana powierzchnia po morfologii i kapitalizacji |
| tags | np. casing, morphology, semantic |
| candidate_set_status | pending_real_decoder albo verified_real_decoder |

Dla dual-casing jedna tożsamość case-insensitive może mieć wiele legalnych powierzchni. Benchmark nie powinien tworzyć z nich dwóch niezależnych kandydatów rankera.

## Metryki

Dla wyboru spośród legalnego candidate set:

- top-1 — poprawny kandydat jest pierwszy;
- top-3 — poprawny kandydat znajduje się w pierwszych trzech;
- top-5 — poprawny kandydat znajduje się w pierwszych pięciu;
- MRR — odwrotność pozycji poprawnego kandydata, uśredniona po przypadkach;
- candidate coverage — odsetek przypadków, w których oczekiwany kandydat w ogóle znalazł się w surowym candidate set;
- OOV — przypadki, w których wymaganej tożsamości nie ma w używanym leksykonie/candidate generatorze;
- surface accuracy — poprawna finalna powierzchnia, niezależnie od tego, czy poprawna tożsamość została znaleziona;
- latency — czas samego rerankingu oraz osobno czas całego przepływu, jeżeli będzie mierzony;
- RAM i koszt energetyczny — dopiero dla implementacji lokalnej.

Kluczowe jest raportowanie coverage osobno. Reranker nie może naprawić przypadku, którego dekoder nigdy nie umieścił w candidate set.

## Populacje benchmarku

### A. Casing / homonimia kapitalizacyjna

Przypadki jednej tożsamości z konkurencyjną powierzchnią lowercase/uppercase, np.:

malina / Malina
łódź / Łódź
warszawa / Warszawa

### B. Morfologia

Kontekst, który prowadzi do właściwej formy fleksyjnej, np.:

Jadę do + lodz → Łodzi
Mieszkam w + warszawa → Warszawie

### C. Semantyka

Kontekst rozstrzygający znaczenie, np. miasto vs rzeczownik pospolity:

Mieszkam w + lodz → Łodzi
Wiosłuję przy + lodz → łodzi

### D. Kontekst wielowyrazowy

Dwa lub więcej poprzednich słów; późniejsze pomiary powinny porównać różną długość okna kontekstu.

## Candidate-set capture

Najważniejszy brak do zamknięcia eksperymentalnie:

1. wykonać rzeczywisty swipe na przypiętym runtime;
2. zapisać dokładny PredictionResult.words i scores przed rescoreWithContext;
3. zachować ten sam przypadek wejścia, kontekst, język i wersję leksykonu;
4. dopiero wtedy uruchomić benchmark modeli.

Nie wolno używać listy po augmentPredictionsWithPossessives, finalnym rerankingu ani po prezentacji jako dowodu surowego candidate set.

## Porównywane klasy modeli

Benchmark powinien umożliwić porównanie:

- n-gram / unigram-bigram;
- mały wyspecjalizowany model klawiaturowy;
- mały Transformer;
- Bielik 1.5B oraz wariant kwantyzowany, gdy będzie dostępny w środowisku testowym;
- ewentualne dodatkowe lokalne modele polskojęzyczne.

Nie ma z góry ustalonego zwycięzcy. Decyzja architektoniczna ma wynikać z pomiarów.

## Etapy

### Etap 1 — przygotowanie
Schemat danych + ręczny seed cases. To nie jest jeszcze dowód jakości.

### Etap 2 — rzeczywisty candidate set
Eksport surowych wyników dekodera dla polskiego leksykonu.

### Etap 3 — replay offline
Te same przypadki i te same slates dla każdego modelu; mierzone top-1/3/5, MRR, coverage i OOV.

### Etap 4 — porównanie zasobowe
RAM, rozmiar modelu, latency, stabilność, a później koszt energii.

### Etap 5 — dopiero potem runtime
Wyłącznie model/architektura, która przejdzie etap offline bez naruszania immutable core pakietu PL.

## Ważne rozdzielenie od pakietu PL

Repozytorium językowe jakamilek/CleverKeys-langpack-pl pozostaje osobnym źródłem leksykonu, morfologii, kapitalizacji i provenance.

Ten eksperyment runtime nie zmienia:

- immutable 100k;
- aktywnych modułów;
- reguł kapitalizacji;
- dual-casing w pakiecie.

## Status na 2026-10-01

Benchmark pilot 01 istnieje lokalnie poza GitHubem i jest metodologiczny, z 24 ręcznie przygotowanymi przypadkami oraz syntetycznym candidate setem. Jego wyniki nie są dowodem jakości polskiego.

Ten commit dostarcza tylko specyfikację właściwego benchmarku i seed przypadków. Nie włącza funkcji kontekstowego rerankingu i nie zmienia main.

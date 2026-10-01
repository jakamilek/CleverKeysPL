# POLISH CONTEXT RUNTIME AUDIT — 2026-10-01

## Zweryfikowany stan

Repozytorium eksperymentalne runtime: jakamilek/CleverKeysPL

Gałąź: exp/context-reranking-runtime-2026-10-01

Początek gałęzi: 263bd0abc03dec420f60fa073a9d2c5e25a176b5

Bieżące porównanie gałęzi do tego SHA: identyczne przed tym commitem.

## Najważniejsze odkrycie

Przypięty runtime już zawiera zamierzony mechanizm kontekstowego rerankingu.

Zweryfikowane komponenty:

- GeometricSwipeEngine tworzy kandydatów z rzeczywistego śladu swipe i zwraca PredictionResult;
- InputCoordinator.handlePredictionResults przekazuje wynik do SuggestionHandler.handleSwipePredictionResults;
- SuggestionHandler wywołuje rescoreWithContext po dekodowaniu i przed augmentacją powierzchni;
- SwipeContextRescorer wykonuje re-ranking przez stabilną permutację listy;
- kontekst jest pobierany przez WordPredictor.getSwipeContextEvidence;
- PredictionResult.languages może przenosić etykietę źródłowego języka, więc permutacja może zachować powiązanie słowa z metadanymi.

## Konsekwencja projektowa

Nie implementujemy nowego rerankera na tej gałęzi.

Najpierw musimy dostarczyć prawdziwy polski candidate set przed rescoreWithContext.

Dopiero wtedy warto badać, czy obecna funkcja kontekstowa:

- poprawia top-1;
- poprawia top-3/top-5;
- zwiększa MRR;
- nie pogarsza przypadków, które były już poprawne;
- daje akceptowalne opóźnienie.

## Czego jeszcze nie mamy

Na 2026-10-01 nie mamy w tym środowisku zweryfikowanego, trwałego eksportu polskich surowych slate'ów PredictionResult.words + scores z przypiętego runtime.

Nie mamy też podstaw, aby twierdzić, że lokalne dane pilota 01 są prawdziwymi wynikami dekodera produkcyjnego.

Dlatego ten etap zapisuje benchmark i schemat capture, ale nie raportuje jakości polskiego rerankingu jako wyniku produkcyjnego.

## Zakaz przypadkowego włączenia

Funkcja swipe_context_rescoring pozostaje domyślnie OFF.

Ten commit nie zmienia zachowania użytkowego aplikacji.

# HerBERT FP32 — pierwszy raport faz v2 z Nubii

Otrzymano od użytkownika 2026-10-05 o 19:49:10 Europe/Warsaw. Jedna próba, PID 10016;
numer 1 na ekranie. Nie jest to dowód świeżego procesu ani wielokrotnej odtwarzalności.
Raport surowy zachowany bez poprawiania zaokrągleń i fleksji.

## Tożsamość i zgodność

Commit db88fd28cca21ba2aa1e99b38e3886f1f147b5d6, bazowy APK SHA256
c3d15aaf1ba26b8f2534199dda5a6b8835cb72e5233038f4e6f1292b57ed3dd9 i model SHA256
f851436ba9ca35d0c7313ff873cd869b744b295a8a16fc95b4571d5e11b299f2 zgadzają się
z CI run 37348519387 / producentem 37230171787. Urządzenie nubia NX721J,
Android 15, arm64-v8a; ORT 1.21.1 CPU 2/1.
2471 przykładów tokenizera i 232 paczki przeszły; maksymalny wyświetlony błąd
0,000062 < 0,001. Udany raport wymaga również oryginalnej zgodności pięciu wejść,
sum/średnich i rankingu wszystkich referencji. To potwierdza implementację,
nie niezależną trafność na nowych zdaniach.

## Czasy całego przygotowania i inferencji

| Kontekst | Limit / zachowane słowa | p50 / p95 |
|---|---|---|
| krótki 1 | 32 / 7 | 66,5 / 69,4 ms |
| krótki 1 | 64 / 7 | 66,7 / 69,0 ms |
| krótki 2 | 32 / 8 | 82,4 / 84,2 ms |
| krótki 2 | 64 / 8 | 82,8 / 84,2 ms |
| długi 3 | 32 / 32 | 228,0 / 229,7 ms |
| długi 3 | 64 / 48 | 327,5 / 329,2 ms |

30 próbek na zestaw, 90 na limit, 180 łącznie. Pierwsza krótka analiza 70,8 ms;
load + hash 1544,7 ms; cały test 52,5285 s, obejmujący 431 wywołań, rozgrzewki,
pełną zgodność i PSS. Ten ostatni czas nie jest opóźnieniem pojedynczej podpowiedzi.
Długi kontekst z 32 słowami ma p95 o około 30,2% niższe niż ten sam kontekst z 48
słowami. Limit 64 nie oznacza tu 64 wykorzystanych słów. Wspólna sesja i kolejność
etapów pozostają ograniczeniem; nie zmierzono jakości po skróceniu kontekstu.

## Pamięć: koszt powstaje już podczas ładowania

| Etap | PSS całego procesu | Względem punktu bazowego według raportu |
|---|---|---|
| po imporcie / przed load | 346,2 MiB | 0,0 MiB |
| po load + hash | 2121,0 MiB | +1774,9 MiB |
| po pierwszej parze | 2121,7 MiB | +1775,5 MiB |
| największa próbka workload | 2132,1 MiB | +1785,9 MiB |
| ostatnia próbka zgodności | 2086,8 MiB | — |
| natychmiast po close | 1336,8 MiB | +990,7 MiB |

Cały mierzony workload zwiększył największą próbkę o około 11,1 MiB względem
odczytu po load. Już krótka para ma duży PSS; samo obcięcie historii nie rozwiązuje
obserwowanego kosztu ładowania FP32. To nie jest izolowane porównanie RAM limitów
32 i 64 ani pamięć samych wag. Plik modelu ma 651798883 bajty, około 621,6 MiB.

Close poprzedza spadek od ostatniej próbki zgodności o 750,0 MiB; natychmiastowy
odczyt jest nadal około 990,7 MiB ponad baseline. To nie dowodzi wycieku:
mapowania/alokatory/GC i inne części procesu mogą zachować pamięć.
Spadek z 2132,0 do 1995,1 MiB już na początku zgodności pokazuje zmienność.
Największa próbka jest niższa niż v1 (2699,3 MiB), ale te próby różnią się kolejnością
i stanem procesu; nie przypisujemy tej różnicy optymalizacji ani zmianie grafu.

## Kontrola kodu i następny kontrolowany eksperyment

Obecny open(bundle) mapuje model READ_ONLY, hashuje asReadOnlyBuffer i przekazuje
mapped buffer do createSession; trzyma go do udanego native close. Nie robi
jawnego readBytes/ByteArray całego pliku. Po load próbka łączy koszt hashowania,
mapowania, parsowania/optimizacji ORT i alokacji; nie wskazuje ich udziałów.

Dokumentacja [OrtEnvironment](https://onnxruntime.ai/docs/api/java/ai/onnxruntime/OrtEnvironment.html)
opisuje zarówno wejście ByteBuffer, jak i ścieżkę pliku.
[Dokumentacja pamięci ORT](https://onnxruntime.ai/docs/performance/tune-performance/memory.html)
opisuje opcje alokatorów. Samo mapowanie nie jest dowodem bezkopiowego użycia wag.
Aktualna dokumentacja nie stanowi testu kompatybilności przypiętej biblioteki 1.21.1.

Najpierw porównać obecne ładowanie z createSession(privateModelPath, options) przy
tych samych bajtach, pełnej weryfikacji SHA i bramkach; SHA dla ścieżki strumieniowo
w małym buforze, bez dodatkowego mapowania aplikacji. Każdy wariant w oddzielnym
procesie/próbie, najlepiej powtórzony, nigdy file po mapped w jednej sesji/procesie.
Dodać granice przed hash, po hash, po session creation, po pierwszej parze i close;
osobne Java/native/file PSS oraz czas, bez wymuszania GC. Ścieżka prywatna,
niezmienny snapshot, serialized open/score/close/delete, bez raportowania ścieżki.

To protokół dalszej próby, jeszcze NIE zaimplementowana optymalizacja. Nie
obiecujemy spadku pamięci. Opcje arena/optimizacji dopiero jako kolejne osobne
zmienne; po zmianie wymagane wszystkie oryginalne score/rank gates.
Jeśli koszt pozostanie duży, porównać lżejszy model z niezależną oceną polskiego
kontekstu. Obecny INT8 pozostaje FAIL. FP32 pozostaje referencją jakości;
live SI wyłączona. Domyślnie nadal 32 słowa, max 64/4096 UTF16.
Przed integracją pozostają niezależna jakość, budżety pamięci/opóźnienia i energia.
Drobny błąd polskiego raportu „1 próbek” zapisać do poprawy w następnej zmianie UI.

## Raport surowy

```text
HerBERT FP32 — pomiar etapów v2
nubia NX721J; Android 15; arm64-v8a
Tokenizer: 2471 przykładów; wyniki: 232 paczki kandydatów
Największy błąd wyniku: 0,000062
Wczytanie modelu wraz ze sprawdzeniem sumy: 1544,7 ms
Próbki pomiarowe: 180 (dwie formy, trzy długości kontekstu)
Tokenizacja i przygotowanie wejścia p50/p95: 1,1 / 3,0 ms
Inferencja p50/p95: 81,5 / 325,0 ms
Całość p50/p95: 82,7 / 328,1 ms
Największa próbka PSS całego procesu: 2132,1 MiB
Commit kodu: db88fd28cca21ba2aa1e99b38e3886f1f147b5d6
SHA-256 zainstalowanego bazowego APK: c3d15aaf1ba26b8f2534199dda5a6b8835cb72e5233038f4e6f1292b57ed3dd9
SHA-256 modelu: f851436ba9ca35d0c7313ff873cd869b744b295a8a16fc95b4571d5e11b299f2
Numer próby na tym ekranie: 1; PID: 10016
ORT 1.21.1 CPU; wątki 2/1. Nowa sesja; proces może zachowywać alokacje z wcześniejszych prób.

Pierwsza krótka analiza dwóch form wraz z przygotowaniem: 70,8 ms
Cały test wraz z rozgrzewką, zgodnością i odczytami PSS: 52528,5 ms

Limit 32 słów — 90 próbek
Tokenizacja/wejście p50/p95: 1,1 / 2,3 ms
Inferencja p50/p95: 81,3 / 226,8 ms
Całość p50/p95: 82,4 / 228,8 ms
Limit 64 słów — 90 próbek
Tokenizacja/wejście p50/p95: 1,2 / 3,2 ms
Inferencja p50/p95: 81,7 / 325,6 ms
Całość p50/p95: 82,8 / 328,5 ms

Kontekst 1, limit 32 słów, zachowano 7 słów; B/S/T=2/11/1; 30 próbek
Przygotowanie p50/p95: 0,7 / 0,9 ms
Inferencja p50/p95: 65,7 / 68,6 ms
Całość p50/p95: 66,5 / 69,4 ms
Kontekst 1, limit 64 słów, zachowano 7 słów; B/S/T=2/11/1; 30 próbek
Przygotowanie p50/p95: 0,8 / 0,9 ms
Inferencja p50/p95: 65,9 / 68,3 ms
Całość p50/p95: 66,7 / 69,0 ms
Kontekst 2, limit 32 słów, zachowano 8 słów; B/S/T=2/14/2; 30 próbek
Przygotowanie p50/p95: 1,1 / 1,2 ms
Inferencja p50/p95: 81,3 / 83,2 ms
Całość p50/p95: 82,4 / 84,2 ms
Kontekst 2, limit 64 słów, zachowano 8 słów; B/S/T=2/14/2; 30 próbek
Przygotowanie p50/p95: 1,2 / 1,3 ms
Inferencja p50/p95: 81,7 / 83,1 ms
Całość p50/p95: 82,8 / 84,2 ms
Kontekst 3, limit 32 słów, zachowano 32 słów; B/S/T=2/42/1; 30 próbek
Przygotowanie p50/p95: 2,0 / 2,4 ms
Inferencja p50/p95: 226,0 / 227,3 ms
Całość p50/p95: 228,0 / 229,7 ms
Kontekst 3, limit 64 słów, zachowano 48 słów; B/S/T=2/61/1; 30 próbek
Przygotowanie p50/p95: 2,8 / 3,5 ms
Inferencja p50/p95: 324,6 / 327,5 ms
Całość p50/p95: 327,5 / 329,2 ms

PSS etapami — MiB całego procesu. Punkt bazowy obejmuje zaimportowany tokenizer. Różnica porównuje największą próbkę etapu z punktem bazowym. Pomiar dwóch form poprzedza pełną zgodność w jednej nowej sesji; późniejsze etapy mogą zachowywać wcześniejsze alokacje. Odczyt po zamknięciu jest natychmiastowy; mapowania pliku i alokatory procesu mogą pozostać. Bez wymuszania GC i bez pomiaru dokładnego szczytu ani samego modelu.
Po imporcie, przed wczytaniem modelu: pierwsza/ostatnia/największa 346,2 / 346,2 / 346,2 MiB; różnica 0,0 MiB; 1 próbek
Po wczytaniu modelu: pierwsza/ostatnia/największa 2121,0 / 2121,0 / 2121,0 MiB; różnica 1774,9 MiB; 1 próbek
Po pierwszej krótkiej analizie dwóch form: pierwsza/ostatnia/największa 2121,7 / 2121,7 / 2121,7 MiB; różnica 1775,5 MiB; 1 próbek
Rozgrzewka — kontekst 1, limit 32 słów: pierwsza/ostatnia/największa 2121,6 / 2121,6 / 2121,6 MiB; różnica 1775,5 MiB; 2 próbek
Rozgrzewka — kontekst 1, limit 64 słów: pierwsza/ostatnia/największa 2121,7 / 2121,7 / 2121,7 MiB; różnica 1775,5 MiB; 2 próbek
Analizy mierzone — kontekst 1, limit 32 słów: pierwsza/ostatnia/największa 2121,7 / 2123,3 / 2123,3 MiB; różnica 1777,1 MiB; 2 próbek
Analizy mierzone — kontekst 1, limit 64 słów: pierwsza/ostatnia/największa 2121,8 / 2123,2 / 2123,2 MiB; różnica 1777,1 MiB; 16 próbek
Rozgrzewka — kontekst 2, limit 32 słów: pierwsza/ostatnia/największa 2123,3 / 2123,4 / 2123,4 MiB; różnica 1777,2 MiB; 2 próbek
Rozgrzewka — kontekst 2, limit 64 słów: pierwsza/ostatnia/największa 2123,4 / 2123,5 / 2123,5 MiB; różnica 1777,4 MiB; 2 próbek
Analizy mierzone — kontekst 2, limit 32 słów: pierwsza/ostatnia/największa 2123,6 / 2126,0 / 2126,0 MiB; różnica 1779,8 MiB; 12 próbek
Analizy mierzone — kontekst 2, limit 64 słów: pierwsza/ostatnia/największa 2123,6 / 2126,0 / 2126,0 MiB; różnica 1779,8 MiB; 6 próbek
Rozgrzewka — kontekst 3, limit 32 słów: pierwsza/ostatnia/największa 2126,1 / 2126,3 / 2126,3 MiB; różnica 1780,1 MiB; 3 próbek
Rozgrzewka — kontekst 3, limit 64 słów: pierwsza/ostatnia/największa 2126,5 / 2126,7 / 2126,7 MiB; różnica 1780,6 MiB; 4 próbek
Analizy mierzone — kontekst 3, limit 32 słów: pierwsza/ostatnia/największa 2126,8 / 2132,1 / 2132,1 MiB; różnica 1785,9 MiB; 16 próbek
Analizy mierzone — kontekst 3, limit 64 słów: pierwsza/ostatnia/największa 2127,0 / 2132,0 / 2132,0 MiB; różnica 1785,8 MiB; 30 próbek
Pełna kontrola zgodności: pierwsza/ostatnia/największa 1995,1 / 2086,8 / 2086,8 MiB; różnica 1740,6 MiB; 52 próbek
Zaraz po zamknięciu sesji ONNX: pierwsza/ostatnia/największa 1336,8 / 1336,8 / 1336,8 MiB; różnica 990,7 MiB; 1 próbek
PSS jest próbkowane pomiędzy wywołaniami; nie oznacza dokładnego szczytu ani pamięci samego modelu. Tylko gotowe przykłady; bez niezależnej oceny trafności i bez uruchamiania SI w podpowiedziach.
```

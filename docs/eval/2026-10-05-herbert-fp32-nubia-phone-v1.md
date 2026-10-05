# HerBERT FP32 na Nubia NX721J — pierwszy raport telefonu

Raport przekazany przez użytkownika 2026-10-05 o 18:52:50 Europe/Warsaw. Czas rozpoczęcia i całkowity czas testu nie zostały podane. Jedno zgłoszone wykonanie; nie znamy temperatury, obciążenia innych aplikacji ani stanu systemowego cache.

## Tożsamość próby

Telefon raportuje nubia NX721J, Android 15, arm64-v8a. Użytkownik wcześniej podał Nubia Z60 Ultra LV, 12 GB RAM /512 GB.
Bieżąca zalecana instalacja: code head 884a29b72e8673ad57e506c3d3d2d174e5cd329f, run 37280833641, APK artifact 11331993610.
Raport v1 nie zawiera hash APK/modelu ani runu; powiązanie z tą instalacją wynika z przebiegu rozmowy, a nie z niezależnej identyfikacji APK na urządzeniu. Import sprawdza przypięte siedem tożsamości.
Model: allegro/herbert-base-cased, rev 50e33e0567be0c0b313832314c586e3df0dc2297; FP32 model.onnx 651798883 bajty, SHA256 f851436ba9ca35d0c7313ff873cd869b744b295a8a16fc95b4571d5e11b299f2, ORT Android 1.21.1 CPU, intra/inter threads 2/1.

## Wynik i interpretacja

Import oraz pełny pomiar zakończyły się sukcesem na telefonie. HerbertConformance przed zwróceniem raportu wymaga identycznej tokenizacji, wszystkich pięciu feeds, zgodności średniej/sumy oraz niezmienionego pełnego rankingu każdego z 232 zapisanych batchy.
2471 wektorów tokenizera, 232 batche /532 kandydatów, największy raportowany błąd 0.000062 — poniżej progu 0.001. Wyświetlany błąd jest zaokrąglony do sześciu miejsc.
To potwierdza przeniesienie obliczeń na Androida dla przygotowanych danych. Nie jest niezależną oceną, czy SI poprawnie rozumie nowe zdania użytkownika.

| Limit słów | Próbek | Mediana całości | p95 całości | Mediana inferencji | p95 inferencji |
|---|---:|---:|---:|---:|---:|
| 32 | 90 | 82,2 ms | 229,0 ms | 81,5 ms | 227,8 ms |
| 64 | 90 | 81,7 ms | 326,9 ms | 81,1 ms | 325,7 ms |

p95 dla 32 jest o 97,9 ms /około 29,9% niższe od 64 w tym zestawie. Mediany są podobne; nie dowodzą, że dłuższy kontekst jest równie tani.
Kod testu używa trzech kontekstów: 7, 8 i 48 słów, po dwie formy. Dwa krótkie konteksty są takie same w obu oknach, a ostatni zostaje przycięty tylko przy 32. Test 64 nie wypełnia wszystkich 64 słów. Agregat po trzech długościach nie jest rozkładem typowego pisania i nie pokazuje oddzielnie każdego kontekstu.
Tokenizacja/wejście 0,5 ms p50 i 1,2/1,6 ms p95 stanowią mały koszt; przeważa inferencja. Wczytanie wraz z ponownym SHA trwało 1256,2 ms. Nie obejmuje pobierania/importu, nie jest pełnym czasem startu aplikacji i nie oznacza potwierdzonego cold-start.

## Pamięć i ograniczenie obecnego benchmarku

Największa próbka PSS całego procesu: 2699,3 MiB, około 2,64 GiB. To znaczący koszt do zbadania przed włączeniem SI w klawiaturze.
Activity próbkuje PSS przed testem, po otwarciu sesji i między wywołaniami, z odstępem >=250 ms. Raport podaje jeden maksimum, bez wartości bazowej i rozbicia na fazy/okna.
Ten sam scorer/sesja wykonuje najpierw 232 testy zgodności, potem warmup i 180 pomiarów. Oryginalne feedy mają 49 różnych B/S/T, maksymalne B=8, S=21, T=3 (lokalna inspekcja przypiętych metadanych); nie są to ogromne sekwencje 512-tokenowe. Nie przypisujemy 2,64 GiB wyłącznie testom zgodności ani samym wagom.
ORT domyślnie może zachowywać zaalokowaną pamięć arena; w kodzie brak specjalnej konfiguracji zwalniania. To hipoteza, nie ustalona przyczyna wyniku:
https://onnxruntime.ai/docs/get-started/with-c.html#features
https://onnxruntime.ai/docs/performance/tune-performance/memory.html
Eksport już wybiera hidden states pozycji celu przed głowicą MLM; nie proponować tej istniejącej optymalizacji jako nowej naprawy.

## Decyzja i kolejny pomiar

Utrzymać 32 jako domyślny limit, geometric i obie źródłowe formy oraz wyłączone live SI. Nie aktywować FP32 domyślnie na podstawie jednego raportu. INT8 pozostaje FAIL; warunki niezmienione.
Kolejny diagnostyczny etap: PSS bazowe po imporcie, po wczytaniu, przy dwóch formach w krótkim i 32-słownym kontekście, oddzielnie po pełnej zgodności i po zamknięciu. Pomiar intended workload powinien poprzedzać szeroką zgodność w świeżej sesji, bez utraty osobnego obowiązkowego wyniku zgodności. Raportować długość/tokeny/B/T i p50/p95 per kontekst zamiast wyłącznie agregatu; dodawać hash/run do tożsamości raportu.
Porównać polityki alokacji dopiero jako jawne oddzielne eksperymenty, z niezmienioną numeryczną/rankingową bramką oraz pomiarem kosztu czasu. Obecny kod/parametry/model nie zostały zmienione w tym etapie.
Następnie niezależna ocena jakości i decyzja o lżejszym modelu lub opt-in kolejności form. Czas oczekiwania musi być ograniczony; model ładowany poza ścieżką pojedynczego swipe, bez blokowania UI i zmian już zatwierdzonego tekstu.
Brak wyniku energii, throttlingu, dokładnego szczytu, retencji po zamknięciu lub niezależnej jakości.

## Raport źródłowy

```text
HerBERT FP32 — pomiar v1
nubia NX721J; Android 15; arm64-v8a
Tokenizer: 2471 przykładów; wyniki: 232 paczki kandydatów
Największy błąd wyniku: 0,000062
Wczytanie modelu wraz ze sprawdzeniem sumy: 1256,2 ms
Próbki pomiarowe: 180 (dwie formy, trzy długości kontekstu)
Tokenizacja i przygotowanie wejścia p50/p95: 0,5 / 1,4 ms
Inferencja p50/p95: 81,4 / 325,1 ms
Całość p50/p95: 82,0 / 326,6 ms
Największa próbka PSS całego procesu: 2699,3 MiB
Limit 32 słów — 90 próbek
Tokenizacja/wejście p50/p95: 0,5 / 1,2 ms
Inferencja p50/p95: 81,5 / 227,8 ms
Całość p50/p95: 82,2 / 229,0 ms
Limit 64 słów — 90 próbek
Tokenizacja/wejście p50/p95: 0,5 / 1,6 ms
Inferencja p50/p95: 81,1 / 325,7 ms
Całość p50/p95: 81,7 / 326,9 ms
PSS jest próbkowane pomiędzy wywołaniami; nie oznacza dokładnego szczytu ani pamięci samego modelu. Tylko gotowe przykłady; bez niezależnej oceny trafności i bez uruchamiania SI w podpowiedziach.
```

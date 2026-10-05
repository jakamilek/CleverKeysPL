# HerBERT FP32 — pomiar pamięci etapami v2

Run [37348519387](https://github.com/jakamilek/CleverKeysPL/actions/runs/37348519387) zakończył się sukcesem.
Pobierz [ZIP z APK ARM64](https://github.com/jakamilek/CleverKeysPL/actions/runs/37348519387/artifacts/11361288863),
rozpakuj i zainstaluj CleverKeys-v2.0.0-arm64-v8a.apk jako aktualizację.
Zachowaj ten sam ZIP modelu. Najbardziej przydatna będzie pierwsza próba po aktualizacji aplikacji.

1. Otwórz Plac testowy gestów → Test polskiej SI.
2. Importuj dotychczasowy ZIP modelu (co najmniej 1,4 GB wolnego miejsca na czas importu).
3. Wybierz Uruchom test i pozostań na ekranie. Widzisz aktualny etap oraz postęp pomiarów
   lub kontroli zgodności. Test zaczyna od dwóch form, następnie sprawdza pełną zgodność.
4. Po zakończeniu Kopiuj raport i przekaż całość, w szczególności PSS etapami i sześć
   zestawów czasów. Raport ma nagłówek „pomiar etapów v2” i identyfikatory APK/modelu.

Import modelu i sam model pozostają bez zmian. SI w podpowiedziach pozostaje wyłączona.
Kopiowanie jest dostępne po powstaniu raportu; anulowanie/błąd również daje raport
częściowy z pamięcią, jednoznacznie bez potwierdzonej zgodności tej próby.

Raport pokaże punkt bazowy po imporcie, wczytanie, pierwszą krótką analizę dwóch form,
rozgrzewkę/pomiary dla trzech kontekstów i dwóch limitów, pełną kontrolę zgodności oraz
pamięć zaraz po zamknięciu sesji. Konteksty źródłowe mają 7, 8, 48 słów — dla długiego
przy limicie 32 pozostaje 32, przy 64 pozostaje 48. Każdy zestaw ma 30 próbek.

PSS dotyczy całego procesu. Różnica względem punktu bazowego nie jest pamięcią samych
wag. Jedna sesja zachowuje wcześniejsze alokacje między etapami; po zamknięciu mapowanie
pliku może czekać na GC, a alokator procesu może zachowywać pamięć. Nie wymuszamy GC
ani zmiany ustawień alokacji. Próbki między wywołaniami nie wskazują dokładnego szczytu.
Sesja jest nowa przy każdym uruchomieniu testu, ale proces może pozostawać rozgrzany.

Poprzedni raport telefonu jest zachowany w
[analizie v1](eval/2026-10-05-herbert-fp32-nubia-phone-v1.md).
Ten etap ma wyjaśnić koszt pamięci, nie ocenia niezależnej trafności nowych zdań.

## Dowód CI i tożsamość APK

Kod: db88fd28cca21ba2aa1e99b38e3886f1f147b5d6; job 111893158683.
Kompilacja Android, JUnitCore 2851 testów, oryginalne 2471 wektorów tokenów /
232 paczki / 532 kandydatów / pięć wejść, 83 testy edytora (45+16+8+11+3),
debug/vital lint, assembly i audyt APK bez FP32/fixture PASS.
To nie zastępuje nowego natywnego pomiaru telefonu ani niezależnej oceny trafności.

APK: 35 742 138 bajtów, SHA256
c3d15aaf1ba26b8f2534199dda5a6b8835cb72e5233038f4e6f1292b57ed3dd9.
ZIP artefaktu 11361288863: 35 743 280 bajtów, SHA256
05883670b94c815be141f98c993eed7f337dc4407daeb66b419693bd8305ed70;
ważny do 2026-10-19T17:40:07Z.
[Raporty CI](https://github.com/jakamilek/CleverKeysPL/actions/runs/37348519387/artifacts/11362182090):
22 355 bajtów, SHA256 e9006a2add28f93d9bd0a7b67dfba23ec89976dd9e773c860669e663334847f7.
Sumy APK pochodzą z logu CI, ZIP z metadanych GitHub; APK nie pobierano ani nie skanowano lokalnie.
Wersja aplikacji pozostaje 2.0.0, dlatego potwierdzeniem nowej próby jest nagłówek
„pomiar etapów v2”, commit i SHA APK w skopiowanym raporcie.

## Wynik pierwszej próby v2 na telefonie

[Pełny raport i analiza](eval/2026-10-05-herbert-fp32-nubia-phase-v2.md):
identyczny commit/APK/model, native conformance PASS (max 0,000062).
PSS baseline 346,2 MiB → po load 2121,0 → max workload 2132,1 → po close 1336,8.
Główny obserwowany przyrost występuje podczas load; szczegółowa przyczyna nieustalona.
Krótkie konteksty total p50/p95 66,5/69,4 i 82,4/84,2 ms, długi limit 32
228,0/229,7 ms, limit 64 zachowujący 48 słów 327,5/329,2 ms.
Następny zaplanowany eksperyment porówna ładowanie z mapped buffer i z prywatnej
ścieżki pliku w osobnych procesach, bez zmian modelu/bramek. Jeszcze nie wdrożony;
nie twierdzimy, że zużycie pamięci spadnie. Nowy raport nie wymaga ponowienia v2 teraz.

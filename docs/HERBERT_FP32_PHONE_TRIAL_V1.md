# Test HerBERT FP32 na telefonie — v1

**Aktualizacja 05.10.2026:** naprawa odczytu ZIP-a GitHuba i raport błędów przeszły CI.
Nowy APK jest gotowy do ponownej próby telefonu. Zachowaj pobrany ZIP modelu.
Na czas importu pozostaw co najmniej **1,4 GB wolnego miejsca** na tymczasową kopię
ZIP i rozpakowany model; kopia będzie usunięta po imporcie.

[Run Androida 37280833641](https://github.com/jakamilek/CleverKeysPL/actions/runs/37280833641)
zakończył się SUCCESS dla code commit 884a29b72e8673ad57e506c3d3d2d174e5cd329f.
Compile, JUnitCore OK(2842), mandatory original tokenizer/feed parity, 83 focused
editor regressions, debug/vital lint, assembleDebug i APK ZIP audit/upload przeszły.
To ekran pomiarowy. SI nie jest jeszcze podłączona do codziennych podpowiedzi.

## Pobieranie

1. Pobierz [paczkę APK ARM64](https://github.com/jakamilek/CleverKeysPL/actions/runs/37280833641/artifacts/11331993610).
   Rozpakuj ZIP i zainstaluj CleverKeys-v2.0.0-arm64-v8a.apk. Numer wersji pozostał
   dotychczasowy; to trial diagnostic, nie release. ZIP około 36 MB.
2. Pobierz [ZIP z modelem FP32](https://github.com/jakamilek/CleverKeys-langpack-pl/actions/runs/37230171787/artifacts/11312693984),
   artifact herbert-fp32-benchmark-v1-model (657295441 B, około 657 MB /627 MiB).
   Tego ZIP nie rozpakowuj: zaimportujesz go bezpośrednio w ekranie testowym.
   Wybierz model artifact, nie mały reports artifact. Link modelu ważny do 18.10.2026.

## Wykonanie pomiaru

1. Otwórz plac testowy gestów / Swipe Playground i wybierz Test polskiej SI.
2. Wybierz Importuj ZIP z modelem i wskaż pobrany ZIP z kroku 2.
3. Po statusie gotowości wybierz Uruchom test. Nie trzeba wpisywać przykładów ręcznie.
   Ekran sprawdzi tokenizację, wszystkie feeds, rzeczywiste wyniki ONNX i rankingi,
   a następnie szybkość dwóch form przy limitach 32/64 słów.
4. Po zakończeniu wybierz Kopiuj raport i przekaż jego treść. Możesz powtórzyć test
   2–3 razy bez opuszczania tego ekranu i przekazać także raport z ostatniego pomiaru.
   Jeśli import się nie powiedzie, wybierz Kopiuj raport i przekaż kod oraz przyczynę.
   Jeśli test modelu nie przejdzie, przekaż dokładny komunikat ekranu.

Test używa gotowych przykładów, nie odczytuje tekstu innych aplikacji. Raport zawiera
device/Android/ABI, zgodność, czas load z checksum, tokenizację/wejście, inference i total
p50/p95, osobne okna 32/64 (90 próbek każde), maximum sampled whole-process PSS.
Próbkowane PSS nie jest dokładnym szczytem ani pamięcią samego modelu. Benchmark czasu
nie stanowi niezależnego porównania trafności okien. Zamknięcie ekranu usuwa imported
model po zamknięciu native session; przy ponownym otwarciu trzeba importować ZIP ponownie.

## Tożsamość i zakres weryfikacji

APK 35709678 B, SHA256
ce65d548cd8b82fa5dd8d71f8b68cbf764ec0a48223a1e880b76ee913223e80b.
APK ZIP artifact 11331993610, 35710698 B, GitHub digest
c4c528139523c0e27b326184829cf3a299990b87e80360c5da093da0f357befc.
W paczce są APK, herbert-benchmark-apk.json i herbert-benchmark-apk.sha256.
Reports artifact 11331924004, 22313 B, GitHub digest
ab9b662d1e685a5b66eb779099c718ffe235812f1121ba02fd97f9aac5b563e6.

CI obliczyło raw APK hash i sprawdziło jego ZIP: bez FP32 weights i JVM test fixtures.
Raw APK nie był pobrany/weryfikowany lokalnie w tym workspace. Powyższe SHA pochodzą
ze zweryfikowanych logs/metadata CI. Nie przypisywać tej kontroli lokalnemu skanowaniu.
Model ZIP także nie był pobrany lokalnie; małe metadata były w pełni hash verified,
a importer telefonu zweryfikuje wszystkie siedem plików z compiled trust.

Real Kotlin parity: 2471 original token vectors i wszystkie pięć inputs na
232 batches/532 candidates PASS. Android JNI scores/phone timings/accuracy dopiero
do sprawdzenia na urządzeniu. Żadnego live SI, merge/tag/release/version bump.

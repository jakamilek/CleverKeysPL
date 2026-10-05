# Test HerBERT FP32 na telefonie — v1

**Aktualizacja 05.10.2026:** zgłoszony import nie powiódł się w APK z poniższego
starego runu. Przygotowano naprawę odczytu ZIP-a GitHuba i raport błędów; nowy APK
czeka na CI i ponowny test telefonu. Zachowaj pobrany ZIP modelu.
Na czas importu pozostaw co najmniej **1,4 GB wolnego miejsca** na tymczasową kopię
ZIP i rozpakowany model; kopia będzie usunięta po imporcie.

[Run Androida 37232458354](https://github.com/jakamilek/CleverKeysPL/actions/runs/37232458354)
zakończył się SUCCESS dla code commit 73627fae758bcdf845ed8696be821b7052e326ff.
Compile, JUnitCore OK(2835), mandatory original tokenizer/feed parity, 83 focused
editor regressions, debug/vital lint, assembleDebug i APK ZIP audit/upload przeszły.
To ekran pomiarowy. SI nie jest jeszcze podłączona do codziennych podpowiedzi.

## Pobieranie

1. Pobierz [paczkę APK ARM64](https://github.com/jakamilek/CleverKeysPL/actions/runs/37232458354/artifacts/11314463703).
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
   Jeśli import/test się nie powiedzie, przekaż dokładny komunikat ekranu.

Test używa gotowych przykładów, nie odczytuje tekstu innych aplikacji. Raport zawiera
device/Android/ABI, zgodność, czas load z checksum, tokenizację/wejście, inference i total
p50/p95, osobne okna 32/64 (90 próbek każde), maximum sampled whole-process PSS.
Próbkowane PSS nie jest dokładnym szczytem ani pamięcią samego modelu. Benchmark czasu
nie stanowi niezależnego porównania trafności okien. Zamknięcie ekranu usuwa imported
model po zamknięciu native session; przy ponownym otwarciu trzeba importować ZIP ponownie.

## Tożsamość i zakres weryfikacji

APK 35696490 B, SHA256
70c3e0e94f10e0a6e7ec4f9b6e043767cd9e41e7e21f14f21586958b07e07958.
APK ZIP artifact 11314463703, 35697510 B, GitHub digest
a5fda8a6163de65bdd531cd48a88aa3ce14b79cff04dbb630d80a59f1c3109d5.
W paczce są APK, herbert-benchmark-apk.json i herbert-benchmark-apk.sha256.
Reports artifact 11313914852, 21636 B, GitHub digest
bc3283e33e416130ab41ccb0d3b6287c6dbc5ae2b2908b754891ced34770e9b5.

CI obliczyło raw APK hash i sprawdziło jego ZIP: bez FP32 weights i JVM test fixtures.
Raw APK nie był pobrany/weryfikowany lokalnie w tym workspace; próba dostępu do
download URL zwróciła HTTP403, a download_file ma limit 32 MiB. Powyższe SHA pochodzą
ze zweryfikowanych logs/metadata CI. Nie przypisywać tej kontroli lokalnemu skanowaniu.
Model ZIP także nie był pobrany lokalnie; małe metadata były w pełni hash verified,
a importer telefonu zweryfikuje wszystkie siedem plików z compiled trust.

Real Kotlin parity: 2471 original token vectors i wszystkie pięć inputs na
232 batches/532 candidates PASS. Android JNI scores/phone timings/accuracy dopiero
do sprawdzenia na urządzeniu. Żadnego live SI, merge/tag/release/version bump.

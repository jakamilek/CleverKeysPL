# HerBERT live — wyniki CI i poprawa bramek, 2026-10-06

Kod oceniony: 7c651e753ff72b54b8c3ff05bbcc664a0ae7aaf6.
Run live 37510369362 i standardowy CI 37510378362 zakończone FAILURE.
Nie udostępniać APK z tych runów: artefakt live zawiera tylko raporty.

## Odczytane wyniki

- Android compileDebugKotlin PASS: poprzednia kolizja JVM setter/metoda usunięta.
- Standardowy assembleDebug PASS; upload APK pominięty po błędzie testów.
- Pure: 2857 testów, 2 FAIL. Oba to źródłowe szukanie starej bezparametrowej
  sygnatury handleBackspaceUndoAutocorrect. Odcinek swipe i metoda istnieją;
  parser testu ma szukać początku deklaracji niezależnie od parametrów.
- Oryginalny native conformance marker 2471 token vectors / 232 batches /
  532 candidates / five inputs PASS. To nie oznacza PASS całej próby pure.
- Live etap pure błędnie zgłosił success: pipeline Gradle | tee nie propagował
  exit code producenta. W kolejnym kodzie jawne set -euo pipefail przed pipeline.
- Mock PASS: LanguagePackImportTest45, SwipeAutocapCommitTest20,
  EditorPredictionRegressionTest8, SuggestionTapPartialReplaceTest11,
  SuggestionStripScrollTest3, BackspaceHoldTest45, PointersBackspaceHoldTest26,
  BackspaceHapticRoutingTest1. Razem159. W BS45 także nowe restore/next-BS/
  stale eligibility/refused-commit przypadki. To rzeczywiste wyniki testów.
- LearningFunnelBookkeepingTest: 29 prób, 19 FAIL. 18 miało null zamiast
  języka w mock Config; ścieżka realnego refreshCurrentWordFromEditor wymaga
  prawidłowego języka. Ustawiono config.primary_language="en" w fixture.
  Pozostały test oczekiwał automatycznej spacji po swipe w haśle, niezgodnie
  z przyjętą regułą. Teraz oczekuje samego słowa i nadal sprawdza brak uczenia.
  Dwa nowe przypadki restored ExactAdd/storage-only i cut/paste nie były w liście
  failures, ale nie deklarujemy całej klasy PASS.
- Debug/release vital lint, live assemble/audyt APK pominięte, phone pending.
- Standardowy security gate FAIL: source-map-js1.2.1 w site/bun.lock,
  CVE-2026-93749 HIGH, w logu naprawiona wersja1.2.2. To zależność strony,
  nie nowego modelu ani kodu BS. Bramka pozostaje blokująca.

## Przygotowana poprawka

BackspaceUndoTest akceptuje aktualną sygnaturę z parametrem. Fixture learning
ma prawidłowy język i aktualny kontrakt bez spacji w hasłach. Kod produkcyjny
BS/HerBERT nie został osłabiony ani zmieniony, aby dopasować go do starego testu.
Live pure pipeline teraz przerywa workflow także wtedy, gdy sam marker zgodności
jest obecny, a inny test pure nie przechodzi. Następny run musi wykonać pełne bramki.

site/bun.lock zmienia wyłącznie rozwiązaną wersję source-map-js1.2.1→1.2.2
oraz jej integrity; istniejące semver ranges obejmują1.2.2, package.json bez zmian.
Dane wersji pobrano z [oficjalnego rejestru npm](https://registry.npmjs.org/source-map-js/1.2.2).
Pobrany archive36349B ma zgodne registry SHA1 i SHA512:
sha512-KGj/8Y43x35aZVDtt+J4mK1hoLGHULMYfSkODJNQjNDC3oW1PqPoxMwo0pLUsWM/UEGzON/NxeHywEfNXNP3Vw==.
Lokalny Node SourceMapGenerator/Consumer smoke: round-trip pozycji i source content
PASS. Nie jest to pełny build strony ani wynik kolejnego Trivy PASS.

Lokalnie PASS także YAML/shell parsing, demonstracja nonzero producer przez tee
bez wykonania dalszego polecenia, aktualne source-anchor assertions i JSON Bun lock.
Brak lokalnego Android toolchain; poprawione JVM/MockK próby dopiero w następnym CI.

Monitoring nowego CI najwyżej60s/run, potem użytkownik zgłasza zakończenie.
Po udanym APK i sprawdzeniu SHA: telefon HerBERT/BS, następnie globalny słownik.

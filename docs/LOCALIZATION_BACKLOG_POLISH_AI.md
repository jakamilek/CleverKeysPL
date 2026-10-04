# Tłumaczenia ekranu testowego SI — do uzupełnienia

2026-10-04. Polski i bazowy angielski są kompletne. Użytkownik wskazał polskie
tłumaczenia jako priorytet; pozostałe języki mają być zapisane do późniejszej poprawy.

Run 37230173951, commit 63524ecda63cbbfebc39392a8826f28b0690a0d1: compile/tests PASS,
lintDebug zgłosił 19 MissingTranslation errors wyłącznie dla nowego
res/values/herbert_benchmark.xml. Nie wykryto innego błędu blokującego lint w tym runie.
Dodano lokalny tools:ignore=MissingTranslation tylko dla tego nowego pliku trial,
z odsyłaczem do tego backlogu. Globalne reguły lint ani istniejące tłumaczenia niezmienione.

Braki: de, ru, ko, pt, in/id, lv, it, fr, hu, es, cs, vi, uk, ja, fa, ro, nl, tr.
Efekt: ten eksperymentalny ekran korzysta z bazowego angielskiego w tych językach.
Uzupełnić i poddać weryfikacji językowej przed szerszą publikacją funkcji.

Klucze do tłumaczenia (20; ostatni dodany wraz z porównaniem limitów 32/64):

- herbert_benchmark_title
- herbert_benchmark_explanation
- herbert_benchmark_pending
- herbert_benchmark_no_model
- herbert_benchmark_import
- herbert_benchmark_run
- herbert_benchmark_cancel
- herbert_benchmark_remove
- herbert_benchmark_notice
- herbert_benchmark_copy
- herbert_benchmark_back
- herbert_benchmark_importing
- herbert_benchmark_ready
- herbert_benchmark_import_failed
- herbert_benchmark_running
- herbert_benchmark_passed
- herbert_benchmark_cancelled
- herbert_benchmark_test_failed
- herbert_benchmark_report
- herbert_benchmark_window_report

Zachować numerowane formaty i jednostki raportu. Po dodaniu pełnych tłumaczeń usunąć
lokalny ignore. Brak tłumaczenia nie oznacza nieprawidłowych wyników modelu.

# Tłumaczenia ekranu testowego SI — do uzupełnienia

## Podpowiedź schowka — 2026-10-10

Klucze `clipboard_suggestion_paste` i `clipboard_suggestion_actions` mają pełne wersje
bazową i polską: „Clipboard” / „Schowek” oraz opis dostępności kliknięcia i przytrzymania.
Etykieta nie pokazuje treści schowka i nie zawiera argumentu formatowania. Pozostałe locale
wymagają późniejszego tłumaczenia obu kluczy; obecnie użyją wersji bazowej angielskiej.
Lokalny MissingTranslation ignore obejmuje tylko plik clipboard_suggestion.xml;
globalny lint bez zmian. Polski pozostaje priorytetem. Stary opis „Wklej: %1$s” jest zastąpiony.

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

## Import diagnostics follow-up — 2026-10-05

Those same 18 locales also lack seven new base/Polish keys:
herbert_benchmark_import_storage, herbert_benchmark_import_read,
herbert_benchmark_import_zip, herbert_benchmark_import_contents,
herbert_benchmark_import_identity, herbert_benchmark_import_metadata,
herbert_benchmark_import_error_report. The changed explanation and import_failed
wording also need translation review. Evidence: resources added for the phone import
repair. Other locales fall back to base English; preserve the three numbered string
arguments in the error report. No global lint exception or other-locale edits.

## Phase measurement v2 — 2026-10-05

Those same 18 locales also need 20 new base/Polish keys: herbert_benchmark_identity_report,
case_report, first_report, memory_explanation, memory_row, memory_unavailable,
partial_report, progress_load, progress_first, progress_warmup, progress_workload,
progress_conformance, phase_baseline, phase_loaded, phase_first, phase_conformance,
phase_closed, phase_warmup, phase_workload, phase_case (all with herbert_benchmark_ prefix).
Also review changed report-v2 heading and running message. Evidence: phase diagnostic
resources, base/Polish complete; fallback English until later translation. Preserve
indexed argument types and phase sample units. Global lint remains unchanged.

## Usuwanie podpowiedzi ze słownika — 2026-10-10

Nowy klucz suggestion_remove_from_dictionary ma wersję bazową i polską, z argumentem
%1$s. Pozostałe 21 locale wymagają tłumaczenia później; lokalny MissingTranslation ignore
obejmuje tylko nowy suggestion_removal.xml. advanced_provenance_markers_desc w pozostałych
locale nadal mówi o przytrzymaniu w celu otwarcia statystyk; ten opis jest już nieaktualny
po zmianie listenera. W bazowym i polskim usunięto tę instrukcję. Pozostałe locale
zapisać do korekty; nie zmieniać globalnego lint. Dowód: obsługa offerSuggestionRemoval.

# Migracja CleverKeys PL do wspólnego repozytorium — 2026-10-01

## Wynik

Aplikacja/runtime i pakiet PL zostały połączone na osobnej gałęzi eksperymentalnej w jednym repozytorium.

Aktywne repozytorium integracyjne:
jakamilek/CleverKeysPL

Branch:
exp/unified-pl-project-2026-10-01

Finalny HEAD:
d4876a406a7d3b37075d7295e0ec443d49711287

## Źródła

Runtime przed migracją:
jakamilek/CleverKeysPL @ 6d101df98e28dbcb762cff39f193ebd3e1d644dc

Pakiet PL:
jakamilek/CleverKeys-langpack-pl @ 8862b31044695f0f28a8667ec851d1ba5b278dd3

Snapshot pakietu znajduje się pod:
langpack-pl/

## Walidacja

Porównanie z runtime przed migracją:
2 commity ahead, 0 behind

Brak zmodyfikowanych plików runtime w wyniku migracji.

Dodano 125 plików: 123 pliki bieżącego snapshotu pakietu PL + 2 pliki stanu migracji.

Tryby wykonywalne build-on-termux.sh i gradlew zostały zachowane jako 100755.

## Historia

Nie utworzono sztucznej historii Git przez wpisywanie commitów z niezależnego repozytorium jako parentów. Provenance snapshotu jest zapisany przez dokładny source commit SHA oraz zachowaną zawartość.

## Ochrona produkcji

main nie został zmieniony.

Stare repozytorium pakietu nie zostało usunięte.

Nie zmieniono immutable 100k ani treści pakietu podczas migracji.

## Następne zadanie

Przeanalizować i zintegrować workflowy z langpack-pl/.github/workflows/ do głównego .github/workflows/, uwzględniając nowe ścieżki w monorepo. Nie zakładać, że stare workflowy działają bez zmian.

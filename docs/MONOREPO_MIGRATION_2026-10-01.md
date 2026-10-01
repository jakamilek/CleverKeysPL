# Migracja CleverKeys PL do wspólnego repozytorium — 2026-10-01

## Decyzja

Aplikacja/runtime oraz pakiet PL są od tej pory traktowane jako dwa zależne tory jednego projektu eksperymentalnego.

Aktywnym repozytorium integracyjnym jest:
jakamilek/CleverKeysPL

## Punkt wyjścia

Runtime:
- repo: jakamilek/CleverKeysPL
- branch: exp/context-reranking-runtime-2026-10-01
- HEAD przed migracją: 6d101df98e28dbcb762cff39f193ebd3e1d644dc

Pakiet:
- repo: jakamilek/CleverKeys-langpack-pl
- branch: ops/baseline-sync-2026-09-20
- HEAD źródłowego snapshotu: 8862b31044695f0f28a8667ec851d1ba5b278dd3

## Migracja

Utworzono branch:
exp/unified-pl-project-2026-10-01

z runtime HEAD:
6d101df98e28dbcb762cff39f193ebd3e1d644dc

Bieżący stan pakietu PL został skopiowany bez zmian pod:
langpack-pl/

Źródło snapshotu pozostaje jednoznacznie określone przez:
jakamilek/CleverKeys-langpack-pl@8862b31044695f0f28a8667ec851d1ba5b278dd3

## Dlaczego snapshot, a nie sztuczny merge historii

GitHub API nie pozwala poprawnie podpiąć drzewa/commita z niezależnego repozytorium jako zwykłego poddrzewa drugiego repozytorium. Próba takiego rozwiązania została odrzucona przez API.

Nie fałszujemy więc historii przez wpisywanie nieistniejącego parenta ani przez zakładanie, że obiekty Git z drugiego repozytorium są już dostępne w pierwszym.

Zamiast tego:
- aktywny runtime zachowuje swoją historię
- snapshot pakietu zachowuje dokładny source SHA
- zawartość pakietu jest obecna w jednym aktywnym repozytorium
- oryginalne repozytorium pozostaje zachowane jako źródło historyczne

## Kolejny krok

Należy osobno zintegrować workflowy pakietu do głównego .github/workflows/, po uprzednim sprawdzeniu ich ścieżek i założeń. Nie należy zakładać, że stare workflowy działają po samym przeniesieniu do langpack/.

## Zasada bezpieczeństwa

Nie zmieniono main.
Nie usunięto starego repozytorium.
Nie usunięto starej gałęzi pakietu.
Nie zmieniono immutable 100k.
Nie zmieniono danych pakietu podczas migracji.

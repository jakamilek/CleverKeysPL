# HerBERT w klawiaturze — próba v1

Decyzja użytkownika z 2026-10-06: przygotować testową klawiaturę z oryginalnym
HerBERT-em FP32 oraz haptyką BS, a po przygotowaniu aplikacji wrócić do słownika.
To odrębna próba włączana w ustawieniach. Wysoki koszt pamięci oraz wcześniejsze
nieudane bramki INT8 / distilHerBERT pozostają zapisane; nie stają się wynikami PASS.

## Obsługa

1. Zaktualizuj APK z gałęzi `trial/herbert-live-v1`, zachowując dane aplikacji.
   Budowa i kontrole CI muszą zakończyć się powodzeniem przed instalacją.
2. Ustawienia → menedżery funkcji → **SI HerBERT — test w klawiaturze**.
   Ekran można też znaleźć, wyszukując `HerBERT` lub `SI`.
3. Zaimportuj ten sam zweryfikowany ZIP FP32 co do wcześniejszego pomiaru:
   [model z uruchomienia producenta 37230171787](https://github.com/jakamilek/CleverKeys-langpack-pl/actions/runs/37230171787/artifacts/11312693984).
   Import z poprzedniego ekranu pomiarowego był tymczasowy; nowy ekran zachowuje
   własny zweryfikowany plik między uruchomieniami. Nie usuwa słownika ani ustawień.
4. Włącz **SI w podpowiedziach (test)**. Zaczekaj na status „SI gotowa”. Przed
   aktywacją klawiatura sprawdza sumy siedmiu plików, 2471 przykładów tokenizera
   oraz 232 paczki / 532 kandydatów natywnego modelu, w tym kolejność i wyniki.
   Podczas wczytywania nadal działają zwykłe podpowiedzi geometryczne.
5. Domyślny kontekst to 32 słowa i maksymalnie 4096 jednostek UTF-16. Można
   porównać 16 / 32 / 64 słowa. Limit oczekiwania ma domyślnie 350 ms (regulacja
   100–1000 ms). Wyniki pomiaru Nubii: około 66–83 ms dla krótkich par,
   około 228 ms dla 32 słów i 328 ms dla 48 słów; nowa obsługa wymaga próby telefonu.

SI porządkuje tylko dwie formy pierwszego słowa potwierdzone w metadanych
słownika, np. `łódź` / `Łódź`. Nie dodaje wpisów do słownika, nie opisuje ręcznie
znaczeń i nie zmienia wag geometrycznych ani innych słów. Obie formy pozostają
na pierwszych dwóch miejscach. Jawny Shift, Caps Lock, kapitalizacja początku
zdania i istniejąca preferencja pisowni mają pierwszeństwo. Interpunkcja SI
i porządkowanie różnych słów są poza zakresem tej próby.

Słowo jest wstawiane przez dotychczasową ścieżkę podpowiedzi po analizie lub po
limicie czasu. Następne dotknięcie klawiatury kończy oczekiwanie i wstawia słowo
według dotychczasowej kolejności przed obsłużeniem nowego gestu. Późny wynik nie
przepisuje tekstu. Zmienione pole, kursor, zaznaczenie, tekst, język, pakiet lub
ustawienia uniemożliwiają zastosowanie starego wyniku. Brak modelu, błąd lub
zajęty wykonawca zachowuje działanie geometryczne. Liczniki pokazują liczbę
analiz, przyjętych odpowiedzi, powrotów do dotychczasowej kolejności i ostatni
czas — bez zapisu wpisywanego tekstu.

Kontekst jest odczytywany z bieżącego pola przed wstawieniem maźniętego słowa,
z wielkością liter i interpunkcją. Nie korzysta z dwuwyrazowej historii małymi
literami. Hasła, pola prywatne, wyszukiwanie, WWW/e-mail, terminale i zaznaczenie
tekstu są pomijane. Wszystko działa lokalnie. Model jest poza APK i kopiami
zapasowymi; kopia ustawień zawiera wyłącznie trzy opcje SI. Domyślnie SI jest
wyłączona. Import nie zmienia oryginalnego manifestu `benchmarkOnly=true`,
`phoneReady=false`: ponowne wykorzystanie zweryfikowanych wag do testu live
jest nową decyzją użytkownika, a nie promocją manifestu do wydania produkcyjnego.

## Haptyka BS

Krótki impuls potwierdza udane zaznaczenie poprzedniego słowa. Drugi impuls
potwierdza udane usunięcie — zarówno podczas przytrzymania, jak i przy puszczeniu.
Każde kolejne słowo w cyklu otrzymuje te same potwierdzenia. Sam ruch po znakach
nie wibruje za każdą literą. Anulowanie, puste zaznaczenie i odmowa edytora nie
potwierdzają usunięcia. Odmowa zaznaczenia nie potwierdza wyboru słowa.

Oba zdarzenia korzystają z istniejącego głównego przełącznika wibracji,
przełącznika haptyki długiego przytrzymania oraz własnego czasu wibracji, jeżeli
użytkownik go ustawił. Błąd wibracji nie zatrzymuje edycji. Ruch BS nadal używa
zaakceptowanego mechanizmu przesuwania ze spacji; czasy powtarzania słów pozostają
dotychczasowe. Oddzielne ustawienia tych czasów pozostają na liście dalszych zmian.

## Próba telefonu po udanym CI

- W środku zdania: `Na jeziorze płynie łódź`, `Naszym celem podróży jest Łódź`;
  sprawdź parę na pasku i zastąpienie pierwszej formy drugą bez utraty tekstu.
- Analogicznie `malina / Malina`, `warszawska / Warszawska`, `łodzi / Łodzi`,
  jeżeli bieżący pakiet zawiera potwierdzone pary. Brak pary oznacza brak analizy SI.
- Wprowadź kolejny gest szybko, użyj BS, Shift, kropek, przenieś kursor,
  zaznacz, wytnij/wklej i zmień aplikację podczas oczekiwania. Spóźniony wynik
  nie może zmienić starszego tekstu ani podmienić nowej listy.
- Sprawdź pola prywatne, hasło i wyszukiwanie. W tych polach SI nie powinna
  analizować kontekstu; licznik analiz nie powinien rosnąć.
- Przytrzymaj BS: impuls przy zaznaczeniu i po usunięciu kolejnych słów.
  Przeciągnij BS jak spację, odwróć kierunek, anuluj. Wyłącz haptykę długiego
  przytrzymania oraz wszystkie wibracje i sprawdź brak nowych impulsów.
- Zgłoś płynność, zużycie pamięci/baterii i liczniki dla 16/32 słów. Wcześniej
  PSS całego procesu osiągnął 2132,1 MiB; skrócenie kontekstu nie usuwa stałego
  kosztu ładowania modelu. Ten nowy wariant nie ma jeszcze wyniku telefonu.

Po przygotowaniu APK wracamy do globalnego problemu pokrycia słownika swipe:
`dodam`, `grzeje`, `kasami`, `nawilżane`, `odpowiadam`, `patrzysz`, `poczekaj`,
`podpowie`, `pozdrawiam`. Przyczyna odrzucenia każdego słowa wymaga śledzenia
etapów producenta; nie naprawiamy tego ręcznym dodaniem dziewięciu wyjątków.

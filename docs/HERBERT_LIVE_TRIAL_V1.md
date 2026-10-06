# HerBERT w klawiaturze — próba v1

Decyzja użytkownika z 2026-10-06: przygotować testową klawiaturę z oryginalnym
HerBERT-em FP32 oraz haptyką BS, a po przygotowaniu aplikacji wrócić do słownika.
To odrębna próba włączana w ustawieniach. Wysoki koszt pamięci oraz wcześniejsze
nieudane bramki INT8 / distilHerBERT pozostają zapisane; nie stają się wynikami PASS.

## Obsługa

### Cofanie autokorekty słowa wpisanego literami

Bezpośrednio po autokorekcie pierwszy krótki BS przywraca wpisany oryginał
ze spacją, która już była w polu. Ten wyjątek działa też przy domyślnej opcji
usuwania pojedynczego znaku. Następny BS usuwa znak/spację zgodnie z wybraną
opcją. Przytrzymanie i przeciąganie BS pozostają osobnymi gestami.

Jeśli oryginału nie ma w słowniku, przy włączonym „Pokazuj dokładnie wpisane
słowo” pierwsza podpowiedź pozwala go dodać. Dodawanie nie wpisuje słowa
ponownie, nie usuwa go i nie rusza kursora. Normalne potwierdzenie pozycji
kursora po zamianie zachowuje podpowiedź. Zmiana pola, tekstu lub pozycji
kursora unieważnia cofnięcie; nie jest to reguła dla konkretnego słowa „grzeje”.

Próba po udanym buildzie: wpisz literami słowo spoza słownika, zakończ spacją
i sprawdź, że autokorekta je zmieniła. Pierwszy BS: oryginał + istniejąca
spacja, podpowiedź dodania; dodanie: tekst identyczny. Powtórz i zamiast
dodawać naciśnij drugi BS: zwykłe usunięcie spacji. Sprawdź też, że po
przesunięciu kursora albo wklejeniu innego tekstu stara podpowiedź nie edytuje
tekstu ani nie dodaje nieaktualnego słowa. Ta poprawka wymaga CI i próby telefonu.

### Włączenie SI

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

## Rozszerzenie case-family-v2 — 2026-10-06

SI porównuje najwyżej cztery potwierdzone formy z dwóch rozpoznanych kluczy,
np. `malina / Malina / maliną / Maliną`. Do pierwszego klucza można dołączyć
jeden z pierwszych pięciu kandydatów dekodera, jeżeli różni się wyłącznie
polskimi znakami diakrytycznymi i należy do tego samego polskiego języka.
Wszystkie pisownie muszą już istnieć w metadanych zaimportowanego słownika.
Nie tworzymy odmian ani dodatkowych wpisów i nie przeszukujemy całego słownika.

To zgrupowanie bliskich pisowni, a nie dowód wspólnego lematu: np. `laska`
i `łaska` mają różne znaczenia, choć ich ścieżka na klawiaturze jest taka sama.
SI może uporządkować tylko tę małą grupę. Każda forma zachowuje wagę swojego
klucza dekodera, język i znacznik pisowni; pozostałe kandydatury zachowują
kolejność. Wynik SI nie jest dodawany do wagi geometrycznej. Model nadal zwraca
średni logarytm prawdopodobieństwa całego zamaskowanego słowa, bez zmiany
normalizacji dla odmian o różnej liczbie tokenów. Jakość tej metody w grupach
między różnymi kluczami wymaga osobnej oceny; testy algorytmu jej nie dowodzą.

Również przy wyłączonej SI formy grupy pozostają obok siebie. Cztery formy nie
zmieszczą się wszystkie w pierwszej trójce: celem testu jest poprawna forma w
Top3. Brak `maliną` w pierwszych pięciu zdekodowanych kandydatach lub brak jej
metadanych oznacza brak tej alternatywy w grupie. `łódź → łodzi` wymaga innych
liter, więc nie jest objęte tym rozszerzeniem. Jawny Shift, Caps Lock,
kapitalizacja początku zdania i zastosowana preferencja pisowni nadal wyłączają
ranking SI. Interpunkcja SI pozostaje poza zakresem.

Nie zmieniamy modelu, 32 słów kontekstu ani 350 ms oczekiwania. Poprzednie
pomiary dotyczyły dwóch form; czas i PSS dla czterech nie zostały zmierzone na
telefonie. Dłuższy kontekst może częściej kończyć się powrotem do kolejności
bazowej. Ten powrót także zachowuje wszystkie formy grupy.

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
- `Jutro będę widział się z Maliną`, `Ciasto smakuje maliną`,
  `Rozmawiałem z Maliną o pracy`, `To jest świeża malina`: maznij ostatnie słowo
  i oceń poprawną formę w pierwszej trójce. Zapisz osobno słowo wstawione i
  kolejność na pasku. Porównaj z wyłączoną SI; alternatywy mają pozostać.
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

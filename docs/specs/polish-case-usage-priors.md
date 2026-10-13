# Priorytet użycia pisowni i rzadkie nazwy geograficzne

Data: 2026-10-10. Status: zaakceptowany kierunek, etap danych i kalibracji **niewdrożony**.
Pierwszy etap runtime: compact-case-v4 w [polish-context-ai.md](polish-context-ai.md).
Wdrożenie danych wymaga osobnego audytu producenta; ten dokument nie zmienia langpacka.

## Problem i cel

Samo źródłowe potwierdzenie pisowni nie jest dowodem jej częstego użycia. Jeden klucz
praca może prawidłowo deklarować rzeczownik praca oraz geograficzne Praca, ale oba
odczytania nie powinny automatycznie otrzymywać równorzędnej ekspozycji w podpowiedziach.
Nie usuwać prawdziwych nazw i nie tworzyć zdublowanych kluczy; priorytet dotyczy
odczytania/lematu i konkretnej pisowni, nie poprawności słowa ani całego klucza.

Etap v4 zachowuje pełną grupę dla SI, oddzielając ją od prezentacji: dodatkowe pisownie
kapitalizacji nie rezerwują pierwszej trójki. Nie jest to jeszcze kara za rzadkość.
Model może nadal preferować wielką literę. Brak gwarantowanego Top3 dla pisowni,
która przegrała z inną pisownią tego samego klucza, jest świadomym kompromisem v4.

## Automatyczne źródła i identyfikacja

1. Morfeusz/SGJP: zachować odczytanie, case-sensitive lemat, POS, kategorie i powiązania
   odmian. Ten istniejący dowód mówi, co jest dozwolone; nie dostarcza popularności.
2. Korpus z zachowanym rozróżnieniem lematów/odczytań: osobne dowody dla praca/Praca.
   Kontrolować wielką literę wynikającą z początku zdania, tytułów, skrótów i niejednoznacznych
   analiz. Łączna case-folded częstotliwość słowa nie jest częstotliwością nazwy własnej.
   Sprawdzić bieżący zbiór NKJP używany w producentach przed wyborem metody. Wyszukiwarka
   korpusowa sama w sobie nie jest gotową redystrybuowalną tabelą priorytetów.
3. SIMC/PRNG: potwierdzone identyfikatory obiektów i typ miejscowości. Nie utożsamiać
   każdego odczytania nazwa_geograficzna z polską wsią; mogą to być inne obiekty/kraje.
4. GUS BDL NSP2021 P4182: ludność miejscowości statystycznych jako pomocniczy dowód.
   Miejscowość statystyczna może obejmować kilka miejscowości; wymaga jawnego, audytowanego
   powiązania SIMC/SIMC-STAT. Nie kopiować liczby mieszkańców gminy na każdą wieś.
5. Jedna pisownia może oznaczać wiele miejsc. Zachować wiele identyfikatorów, zakres
   i niepewność agregacji; nie brać pierwszego dopasowania po samej nazwie.

Źródła informacyjne sprawdzone 2026-10-10 (nie oznacza pobrania całych danych):
- [SIMC — podstawowe informacje GUS](https://eteryt.stat.gov.pl/eTeryt/rejestr_teryt/informacje_podstawowe/informacje_podstawowe.aspx)
- [SIMC-STAT — definicja jednostki](https://eteryt.stat.gov.pl/eTeryt/rejestr_teryt/udostepnianie_danych/formy_i_zasady_udostepniania/simc_stat.aspx)
- [BDL P4182 — Ludność w miejscowościach statystycznych](https://bdl.stat.gov.pl/bdl/dane/podgrup/wymiary/31/640/4182)
- [NKJP — dokumentacja wyszukiwania według lematu](https://nkjp.pl/poliqarp/help/ense3.html)

## Kontrakt przyszłych metadanych

Przy odczytaniu przechowywać opcjonalnie dowody użycia: lemat/POS, rodzaj odczytania,
liczniki korpusowe z mianownikiem i metodą dezambiguacji, identyfikatory geograficzne,
populację z rokiem i poziomem agregacji, zakres danych oraz stan kompletności.
Każdy dowód ma źródło, rewizję/datę i SHA/proweniencję; bez ręcznych opisów znaczeń.
Konkretny JSON/schema i współczynniki pozostają do ustalenia po audycie danych.

Nieznane pole/licznik = brak wiedzy, nigdy zero mieszkańców lub dowód rzadkości.
Nieobecność w małej próbce korpusu nie dowodzi nieistnienia ani małej popularności.
Same ludność/typ miejscowości nie mogą blokować pisowni: mała miejscowość może być
znana turystycznie lub często wspominana lokalnie. Bez GPS i pobierania kontekstu z sieci.
Przy lemacie zachować źródłowe odmiany i ich własne dowody; nie zgadywać rdzeni.

## Ranking i zachowanie aplikacji

Po zbadaniu danych priorytet stanowiłby oddzielny, kalibrowany sygnał dla wariantu.
Kontekst SI może awansować nazwę własną, ale nie dodajemy surowego logp do geometrii
ani nie interpretujemy różnicy logp jako skalibrowanego prawdopodobieństwa nazwy.
Obecny model porównuje tylko grupę, więc pozycjonowanie względem pozostałych słów
wymaga osobnej reguły i niezależnej oceny. Nie ustalać współczynników na zdaniu praca.
Jawny Shift, Caps Lock, kapitalizacja zdaniowa i ręczny wybór pozostają nadrzędne.
Każda dozwolona pisownia ma pozostać dostępna; brak nowych danych nie zmienia zera
w dowód ani nie usuwa dotychczasowych możliwości. Domyślna pisownia zwykłego słowa
pozostaje małą literą, gdy istnieją odpowiednie dowody źródłowe.

## Kolejność wdrożenia i bramki

1. Audytować źródła, zakres połączeń i brakujące dowody w CleverKeys-langpack-pl.
2. Zamrozić proponowany kontrakt, bazę kontrolną, proweniencję i protokół oceny.
3. Zbudować raport całego słownika przed zmianą pakietu; osobno common/geo/person,
   znane/nieznane dane, odmiany, homonimy wielomiejscowe i nazwy zagraniczne.
4. Kalibrować na osobnym dev; ocenić na niewidzianych zdaniach i szerszych listach
   dekodera: Top1/Top3, regresje nazw, niepożądane duplikaty, dostępność alternatyw.
5. Dopiero potem zbudować nowy testowy langpack/runtime, wykonać pełne gates i telefon.

Regresje obejmą praca/Praca jako przykład, ale nie będą ograniczone do niego. Łódź,
malina/Malina, małe miejscowości bez homonimu, znane małe miejscowości, niejednoznaczne
identyfikatory, brak danych oraz Shift/początek zdania/prywatne pola pozostają wymagane.
Ogólne pokrycie słownika, haptyka/czasy BS i interpunkcja SI to oddzielne zadania.

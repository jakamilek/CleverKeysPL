# HerBERT FP32 — pomiar pamięci etapami v2

Nowy APK oczekuje CI; wcześniejszy APK z runu 37280833641 zawiera pomiar v1.
Zachowaj ten sam ZIP modelu. Po otrzymaniu nowego zweryfikowanego APK zainstaluj
aktualizację. Najbardziej przydatna będzie pierwsza próba po aktualizacji aplikacji.

1. Otwórz Plac testowy gestów → Test polskiej SI.
2. Importuj dotychczasowy ZIP modelu (co najmniej 1,4 GB wolnego miejsca na czas importu).
3. Wybierz Uruchom test i pozostań na ekranie. Widzisz aktualny etap oraz postęp pomiarów
   lub kontroli zgodności. Test zaczyna od dwóch form, następnie sprawdza pełną zgodność.
4. Po zakończeniu Kopiuj raport i przekaż całość, w szczególności PSS etapami i sześć
   zestawów czasów. Raport ma nagłówek „pomiar etapów v2” i identyfikatory APK/modelu.

Import modelu i sam model pozostają bez zmian. SI w podpowiedziach pozostaje wyłączona.
Kopiowanie jest dostępne po powstaniu raportu; anulowanie/błąd również daje raport
częściowy z pamięcią, jednoznacznie bez potwierdzonej zgodności tej próby.

Raport pokaże punkt bazowy po imporcie, wczytanie, pierwszą krótką analizę dwóch form,
rozgrzewkę/pomiary dla trzech kontekstów i dwóch limitów, pełną kontrolę zgodności oraz
pamięć zaraz po zamknięciu sesji. Konteksty źródłowe mają 7, 8, 48 słów — dla długiego
przy limicie 32 pozostaje 32, przy 64 pozostaje 48. Każdy zestaw ma 30 próbek.

PSS dotyczy całego procesu. Różnica względem punktu bazowego nie jest pamięcią samych
wag. Jedna sesja zachowuje wcześniejsze alokacje między etapami; po zamknięciu mapowanie
pliku może czekać na GC, a alokator procesu może zachowywać pamięć. Nie wymuszamy GC
ani zmiany ustawień alokacji. Próbki między wywołaniami nie wskazują dokładnego szczytu.
Sesja jest nowa przy każdym uruchomieniu testu, ale proces może pozostawać rozgrzany.

Poprzedni raport telefonu jest zachowany w
[analizie v1](eval/2026-10-05-herbert-fp32-nubia-phone-v1.md).
Ten etap ma wyjaśnić koszt pamięci, nie ocenia niezależnej trafności nowych zdań.

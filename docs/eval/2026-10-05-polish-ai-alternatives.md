# Alternatywy SI po pomiarze RAM HerBERTa — przegląd 2026-10-05

Status: przegląd źródeł i shortlista do nowej próby, bez wdrożenia/uruchomienia modeli.
Użytkownik pyta, czy szukać innego silnika ze względu na koszt RAM. HerBERT nie jest
zatwierdzonym silnikiem produkcyjnym. Zachowujemy działający scorer i wyniki jako referencję.

## Co wiemy z własnych pomiarów

Nubia phase-v2: 346,2 MiB PSS baseline, 2121,0 po load, 2132,1 maksimum workload,
1336,8 zaraz po close. Główny przyrost istnieje przed pierwszą inferencją.
Krótsze konteksty 7/8 słów mają total p95 około 69/84 ms, 32 słowa 230 ms,
48 słów 329 ms. Nie zmierzono jeszcze 16 słów ani ich jakości.
PSS całego procesu nie jest izolowaną pamięcią modelu i nie dowodzi leak.
Źródło: eval/2026-10-05-herbert-fp32-nubia-phase-v2.md.

W zamrożonym ai_compare_v5 HerBERT plain trafił 50/64 form top1, MiniLM NLI 28/64,
Qwen3-0.6B 36/64. Przecinek/brak znaku:15/20, 12/20, 8/20. Top 3 par jest nasycone
przez konstrukcję i nie dowodzi przewagi SI. Wyniki mają znane klucze/autorskie
konteksty i konkretne adaptery, nie niezależną jakość ani ocenę całej rodziny.
Źródło producenta:
https://github.com/jakamilek/CleverKeys-langpack-pl/blob/ebbe16b2cea2a924a7fef4feb635b2cf44ebbed3/experiments/ai_compare_v5_results/RESULTS.md

## Pierwszy nowy kandydat: sdadas/polish-distilroberta

Karta autora podaje polski Fill-Mask, około 82M parametrów oraz Apache-2.0:
https://huggingface.co/sdadas/polish-distilroberta
Repozytorium autorów opisuje sześć bloków 768/12, destylację polskiej RoBERTa-v2,
około 20 GB polskiego korpusu i przeznaczenie na urządzenia z ograniczonymi zasobami:
https://github.com/sdadas/polish-roberta

To uzasadnia pierwszeństwo w próbie, nie gwarancję RAM, szybkości, poprawnej
kapitalizacji czy interpunkcji. Parametry około 82M wobec124494416 faktycznie
załadowanego HerBERTa z v5: około jedna trzecia mniej, nie automatycznie połowa RAM.
Sześć warstw nie oznacza dwukrotnie mniejszego całego modelu. Head, tokenizer,
duplicated graph constants i runtime są istotne; nie prognozujemy PSS z samych wag.
Karta README jest pusta, konfiguracja/tokenizer nie dały się odczytać narzędziem
WWW. Nie przypięto jeszcze rewizji ani nie potwierdzono pełnego pretrained MLM head,
case preservation i zgodności tokenizerów w rzeczywistym ładowaniu.

Ten sam typ zadania MLM umożliwia zaplanowanie oceny dopuszczalnych form słownika.
Porównanie przecinek/brak znaku jako osobna próba jest możliwe w adapterze; jakość
wymaga własnych danych. Nie obiecujemy gotowej głowicy interpunkcji ani interpretacji
metadanych bez treningu. Nie tworzymy nowego modelu od zera.

## Inne sprawdzone kierunki

- distilbert/distilbert-base-multilingual-cased: polski w 104 językach, MLM/case,
  sześć warstw, 134M parametrów według karty. Nie jest oczywistą oszczędnościąRAM
  względem naszego 124M HerBERTa; rezerwa, nie pierwszy wybór.
  https://huggingface.co/distilbert/distilbert-base-multilingual-cased
- Qwen3-0.6B już badano i jego dotychczasowy adapter był słabszy i droższy na CPU
  hosta. Nie ma pomiaru telefonu ani kwantyzacji; nie powtarzać tej samej próby jako
  nowego dowodu. Qwen3.5-0.8B pozostaje kandydatem z wcześniejszego protokołu,
  w tym przeglądzie nie zweryfikowano go do nowej shortlisty.
- alphaedge-ai/Qwen3-0.6B-pol-16384 usuwa tokeny dla polskiego. Autor deklaruje
  mniejszy model, lecz tabela bazowych 751632384 parametrów różni się od 596049920
  w faktycznie testowanym Qwen. Bez własnego odczytu wag/tokenizera i jakości nie
  przyjmujemy rozmiaru ani równoważności. To mniejszy słownik, nie dowód lepszego
  rozumieniaPL; pomijać jako natychmiastowy zamiennik.
  https://huggingface.co/alphaedge-ai/Qwen3-0.6B-pol-16384
- Małe modele embeddingowe nie mają automatycznie wytrenowanej głowicy wyboru
  poprawnej pisowni. Obecny NLI MiniLM miał 28/64; inny uczony ranker jest osobnym
  projektem wymagającym danych, nie gotową podmianą.
  https://huggingface.co/sdadas/mmlw-e5-small

## Kolejność dalszej pracy

1. Potwierdzić tożsamość/revizję i pełny MLM polskiego DistilRoBERTa, bez brakujących/
   losowych wag i trust_remote_code. Własny tokenizer, diakrytyka/case/mask alignment.
2. Nowy zamrożony protokół: HerBERT referencyjny vs DistilRoBERTa, okna 16/32,
   identyczne przypadki i źródłowe formy/atrybuty. Znane testy jako regresje;
   osobny zbiór wcześniej niewidzianych kontekstów, etykiety przed inferencją.
   Bez zmiany promptów/metody per word po wyniku. Obie formy stale dostępne.
3. Osobne wyniki kapitalizacji, regresji zwykłych małych liter, nazw i interpunkcji.
   Nie dodawać opisów miasta/owocu ręcznie ani nie mieszać surowych scores z geometric.
4. Dopiero obiecujący model: eksport FP32 i zgodność, osobna kwantyzacja z pełnymi
   bramkami. PorażkaINT8 HerBERTa nie rozstrzyga kwantyzacji innego modelu.
5. Telefon: osobne procesy, load+hash, PSS baseline/loaded/workload/close,
   p50/p95, powtórzenia i energia. Ustalić budżety przed uruchomieniem, nie wymyślać
   wyniku RAM z nazwy „distil” lub rozmiaruZIP.

Przegląd zmienia kolejność: najpierw ocena mniejszego gotowego polskiego MLM,
optymalizacja ładowania HerBERTa pozostaje rezerwowym eksperymentem/reference.
Nie usuwamy sprawdzonego kodu/tokenizera, nie włączamy live SI i nie zmieniamy
domyślnego 32 na niezweryfikowane 16. INT8 HerBERT pozostaje FAIL.
Bez nowego CI/APK, merge/release/versionbump. Ustawienia czasów BS nadal backlog.

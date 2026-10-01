# POLISH CONTEXT CAPTURE — DEVICE PROCEDURE

## Cel

Ten eksperyment potrzebuje rzeczywistych wyników dekodera swipe. Nie generujemy candidate setu („zbioru kandydatów”) z modelu ani z odległości edycyjnej.

Ta procedura zbiera dla każdego swipe:

- surowy ślad;
- aktywny layout („układ klawiatury”);
- użyty decoder („dekoder”);
- dokładny slate dekodera przed casingiem, context rerankingiem i augmentacją;
- ranking prezentowany użytkownikowi;
- opóźnienie;
- słowo faktycznie zapisane.

## Wersje

Runtime:
\`jakamilek/CleverKeysPL\`

Gałąź:
\`exp/context-reranking-runtime-2026-10-01\`

Bazowy runtime:
\`263bd0abc03dec420f60fa073a9d2c5e25a176b5\`

Pakiet PL:
\`CleverKeys-PL-size-100k-final.zip\`

Zweryfikowany SHA-256 pakietu:
\`b7e7b948937aba177c9903149ad78f0cd956d9bf4473847df0a2152b00a1e734\`

Manifest pakietu:

\`\`\`json
{
  "code": "pl",
  "name": "Polish 100000",
  "version": 2,
  "author": "jakamilek/CleverKeys-langpack-pl",
  "wordCount": 100000,
  "hasPrefixBoost": false
}
\`\`\`

## Instalacja

W GitHubie otwórz:
Actions → Build CleverKeys APK → Run workflow.

Jako branch wybierz:
\`exp/context-reranking-runtime-2026-10-01\`

Nie uruchamiaj żadnej ścieżki release i nie zmieniaj \`main\`.

Zainstaluj wynik na urządzeniu testowym lub emulatorze.

Następnie zaimportuj pakiet PL wskazany powyżej.

## Ustawienia capture

W czasie zbierania danych:

- język słownika: Polski;
- layout: QWERTY (Polski), zgodnie z testem projektu;
- engine: \`geometric\`;
- \`swipe_context_rescoring\`: OFF;
- \`next_word_prediction_enabled\`: OFF;
- inne słowniki: OFF.

Celem jest rejestracja surowego wyniku geometrycznego dekodera bez wpływu eksperymentalnego rerankingu.

## Swipe Debug / Playground

Otwórz Swipe Playground („panel testowy swipe”).

Przed serią testową wybierz wyłącznie:
**Clear → Playground only**

Nie używaj:
**Clear → All swipe data**

Wpisuj wyłącznie przygotowane przypadki testowe. Nie wykonuj w tej sesji przypadkowych prywatnych swipe'ów, ponieważ eksport jest przeznaczony do analizy eksperymentalnej.

## Ważne: target jest literalnym słowem z pakietu

Podczas capture nie używamy lematu jako automatycznie zakładanego celu.

Każdy target powinien być dokładnie taką powierzchnią („formą słowa”), jaką rzeczywiście chcesz sprawdzić i która istnieje w aktualnym pakiecie.

Zweryfikowane przykłady obecne w pakiecie:

- \`Warszawa\`
- \`Warszawie\`
- \`Warszawy\`
- \`Warszawą\`
- \`Warszawę\`
- \`Łódź\`
- \`łodzi\`
- \`toruń\`
- \`Wrocław\`
- \`wrocławia\`
- \`wrocławiem\`
- \`brzeg\`
- \`brzegu\`
- \`Jakub\`
- \`jakubowi\`
- \`malina\`

Nieobecne w tym konkretnym 100k:

- \`Łodzi\`
- \`Torunia\`
- \`toruniu\`
- \`Wrocławiu\`
- \`malinę\`
- \`Malina\`

Te nieobecności są wynikiem leksykonu i muszą być raportowane jako coverage/OOV („pokrycie / brak kandydata”), a nie jako porażka modelu kontekstowego.

## Zalecana pierwsza seria

Najpierw zbierz małą, czystą serię kontrolną:

### Warszawa

1. \`Warszawie\` — „Mieszkam w”
2. \`Warszawy\` — „Jadę do”
3. \`Warszawą\` — „Zachwycam się”
4. \`Warszawę\` — „Widzę”
5. \`Warszawa\` — „To jest”

### Łódź / łódź

6. \`Łódź\` — „Zwiedzam”
7. \`łodzi\` — „Siedzę w”

Uwaga: pakiet zawiera \`Łódź\` i \`łodzi\`, ale nie zawiera \`Łodzi\`. Ten zestaw nie rozstrzyga jeszcze dual-casing („podwójnej kapitalizacji”) w pełni; służy do sprawdzenia coverage i wpływu kontekstu na dostępne powierzchnie.

### Wrocław

8. \`Wrocław\` — „Zwiedzam”
9. \`wrocławia\` — „Jadę do”
10. \`wrocławiem\` — „Jadę przez”

### Toruń

11. \`toruń\` — „Zwiedzam”

Uwaga: brak \`Torunia\` i \`toruniu\` w tym 100k.

### Zwykłe słowo / homonimia

12. \`malina\` — „To jest”
13. \`brzegu\` — „Idę wzdłuż”
14. \`jakubowi\` — „Przyglądam się”

Te trzy przypadki są przede wszystkim kontrolą wspólnego dekodera i morfologii; nie traktujemy ich jeszcze jako dowodu jakości modelu kontekstowego.

## Ile przykładów

Na początek wystarczy 14 czystych przypadków, ale każdy przypadek można wykonać kilka razy z naturalną, lekko różną trajektorią swipe.

Nie próbuj ręcznie normalizować wyników ani przepisywać kandydatów z paska.

## Eksport

Po zakończeniu serii:

Swipe Playground → Export.

Eksport zawiera całe zapisane dane w formacie JSON. Dla eksperymentu potrzebujemy zwłaszcza pól:

- \`metadata.layout_name\`
- \`metadata.engine\`
- \`target_word\`
- \`trace_points\`
- \`registered_keys\`
- \`decoder_candidates\`
- \`candidates\`
- \`decode_latency_ms\`

Pole \`decoder_candidates\` jest najważniejsze: to dokładny slate otrzymany przed casingiem, kontekstowym rerankingiem i późniejszą augmentacją.

Nie usuwaj eksportu ani nie edytuj go przed przekazaniem do analizy.

## Co zrobimy po imporcie eksportu

Z pliku wyliczymy osobno:

- coverage surowego dekodera;
- top-1 / top-3 / top-5 przed rerankingiem;
- MRR przed rerankingiem;
- top-1 / top-3 / top-5 po rerankingiem;
- MRR po rerankingiem;
- liczbę przypadków bez prawidłowego kandydata;
- opóźnienie.

Dopiero wtedy będzie można ocenić, czy kontekst rzeczywiście poprawia kolejność kandydatów.

## Prywatność

Playground przeznaczony jest do jawnej sesji testowej. Eksport może zawierać wpisy słów użytych podczas tej sesji.

Dlatego do tej serii używaj wyłącznie przygotowanych, nieprywatnych przykładów testowych.

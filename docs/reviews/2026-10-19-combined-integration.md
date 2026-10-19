# Közös áttekintés és integráció – 2026-10-19

## Felhasználói döntés

A felhasználó kifejezetten kérte az alábbi három ág és az elkezdett #193/#202
közös áttekintését, majd mainre emelését, **a készülékes teszt előtt**.
Ez erre a csomagra felülírja a korábbi „előbb fizikai elfogadás” munkarendet.
Kódellenőrzés és GitHub-kapuk szükségesek; az egyetlen main telefon/Wear párt
utána próbálja a felhasználó. Nem általános engedély jövőbeli funkciók merge-ére.

## Átvett források

- #193/#202 ág, `0a0467d`: kanonikus átvétel/offline GPS, a `01e0fa1` utáni
  lezárási regresszió javítása. A közös finish-lánc natív fence-t, stoppert,
  naptárat, Health/feedback munkát is megtart; az órás mentés nem nyit külön
  telefonos lezárási/értékelő UI-t. Csak tartós mentés után küld saved nyugtát.
- `test/ui-automation`, `aa09e87`: új böngészős alapfolyamat-teszt, mobil/desktop,
  edzés, sorozat, undo, mentés, napló, nyelvek; a suite-manifestbe regisztrálva.
- `feat/212-language-audit`, `25265ff`: hiányzó telefonos HU/EN/DE/RO szövegek,
  néhány SK/PL üresállapot, Health Connect/napló feliratok és sötét natív
  select/adatvédelmi oldal. A kísérő Windows-tesztrunner fallback Linuxon nem
  változtatja a kaput; nem helyettesít natív készülékes tesztet.
- `feat/197-wear-sensor-stability`, `abe985f`: pulzus-frissesség/availability,
  callback-regisztráció újrapróbálása, aktív szenzorfigyelő és Wear regressziók.

Az utóbbi két ág neve/commitja nem jelenti #212/#190 vagy #197 teljesítését.
A kompakt Intervallumok/korona-lünetta és a progressziós edzésmódok külön nyitott
követelmények; nem zárhatók le a nyelvi vagy szenzor-kiegészítéssel.
A korábban feltöltött öt új funkcióötlet továbbra is csak backlog.

## Áttekintés során javított pontok

1. A közvetlen naplóírás megkerülte a natív eseményfence-t, stopperleállítást és
   ütemezést. A közös lánc visszaállt. Az első kész sorozat is érkezhet csak a
   lezárási fence alatt: a készrejelölés ellenőrzése ezután történik.
2. Korábbi sorozat/start esemény nem sorolható a lezárás mögé. A telefonos
   feldolgozás és az órás outbox megőrzi a korábbi sorrendet; a hiteles végső
   snapshot továbbra is menthető a start előtt beérkezve is.
3. A szenzorág automatikusan minden error állapotot újrapróbált volna. Most
   csak ismert átmeneti hiba, megfelelő cooldown és helyes helyállapot mellett
   teheti; engedélyhiány, másik app és ismeretlen hiba nem automatikus retry.
4. Pause/resume hiba utáni próbálkozás nem lehet végtelen: legfeljebb három
   késleltetett próbálkozás, majd látható Health-hiba; az edzés időzítője külön él.
5. A háttérfigyelő késői válasza nem írhatja át a már lezárt/cserélt sessiont.
   Meglévő owned mérés átvételekor a watchdog friss időalapról indul.
   Ismeretlen HR-availability nem jelenhet meg friss „élő” mérésként.
6. Az új UI-teszt sorozatgomb/input szelektorait konkrét sorozatsorra szűkítettük,
   ne egy fejléc vagy más `.row` beviteli mezőjét kezeljék.

7. Az új `reconnecting` állapothoz a telefon is kapott HU/EN/DE/RO feliratot és
   protokollellenőrzést: az új órás állapot nem eshet ki a telefonos validátorból.
   Callback-regisztráció sikere önmagában még nem aktív mérés: a szolgáltató
   tulajdonosi visszaigazolásáig `starting` marad.

8. A nyelvi ág a védett CSS-baseline elejét módosította. A natív sötét
   formvezérlő szabályok a kiegészítő blokk végére kerültek, az elfogadott
   alapstílus bájtpontosan megmarad; a paritástesztet nem gyengítettük.

## Megőrzendő és ellenőrizendő

- A main `9188932` már tartalmazza #208-at; nem régi mainből kezdődött újra a
  fejlesztés. Az új mentési regresszió a későbbi `01e0fa1` változásból ered.
- Telefonos GPS kifejezett „Mérés befejezése” után a rögzített sorozat kész,
  külön sorozat-OK nélkül menthető. Térképbezárás/hibás/implicit stop nem készít el.
- Új böngészős regresszió védi a lezárás közben érkező natív sorozatot,
  nyugtasorrendet, naptárteljesítést, stopperleállítást és háttéroldal megőrzését.
- A `0a0467d` telefonos teljes gate (39180046566) és 6/6 UI már sikeres;
  ez az összevont új forrás további nyelvi/szenzorváltozásait még nem igazolja.
- A közös forrás végső kapui és main APK-bizonylata az aktuális handoffba kerülnek.
  Verzió jelenleg változatlan 1.3.51 /2732. Fizikai elfogadást ne állíts.

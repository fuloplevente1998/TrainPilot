# P1 #191 — órás mentés és visszaigazolt szinkron

## Kiindulás

Friss main: `20fe5ceaf992bc2658be7422d5c3f45b9c060ace`, a készülékesen elfogadott #158 / #196 után. A #158 és #159 lezárva; a Mini Napló meglévő funkció, nem készül újra. Fejlesztési ág: `feat/191-wear-save-sync-status`. Verzió **1.3.4 /2730**.

Állapot: **implementáció és automatizált ellenőrzés kész; aláírt tesztpár átadva, készülékes elfogadásra vár**. A meglévő outbox és telefonos parancsbizonylat bővítve. [#199 draft PR](https://github.com/fuloplevente1998/TrainPilot/pull/199); #191/#200 nyitva, main változatlan.

## Megvalósított adatút

- A helyi draft rögzítési határán ellenőrzött tartós írás, majd az eredeti outbox ellenőrzött `commit()`. Sikertelen írás nem jelenik meg „Órán mentve” állapotként; az aktív draft nem törlődik. A gyors időzítőfrissítések továbbra is aszinkronok.
- A Data Layer-átadás csak várakozást jelent. A rögzítőparancs az outboxban marad, amíg a telefon tartós parancsbizonylata vissza nem érkezik. Újrapróbálás az eredeti commandId-val, automatikus átadási kísérletek legfeljebb percenként; újracsatlakozáskor és appnyitáskor ismételt átadás.
- A telefon a meglévő `seen` bizonylattárba írja a parancs eredményét; ebből minimális `/trainpilot/command-receipt/<commandId>` DataItem készül. Egészségérték, GPS és edzéssorozat nem kerül a nyugtába. Sikeres nyugtapublikáció után törölhető az eredeti DataItem. Ismételt parancs ugyanazt a megőrzött nyugtát küldi vissza, nem hajtja újra végre az edzést.
- Az óra schema, commandId, workoutId, action és sequence alapján illeszti az eredményt. Az outboxból kivétel és a nyugta megőrzése egy tartós írás. A duplikált, késői és idegen eredmény nem készít naplómásolatot. Korábbi elutasított művelet látható marad; egy későbbi visszaigazolt végső mentés/törlés feloldhatja.
- Kis státuszsor a főmenüben és edzésmenüben, külön görgethető „Mentés és szinkron” oldal, biztonságos újrapróbálás. A főoldal meglévő alcímhelye lezárt edzésnél mutatja a státuszt. Az edzésvezérlés és alsó időzítő mérete/helye megmarad. Magyar, angol, német és román státuszszövegek a rendszer nyelvét követik.
- A régi, csak azonosítót tartalmazó telefonos bizonylatból nem gyártunk kitalált sikeres nyugtát; ezért a #191 próbához mindkét APK frissítendő. A telefon korábbi natív/JS műveleti protokollja és a Mini Napló kanonikus története megmarad.

## Teszt közben jelzett automata-regresszió — #200

A felhasználó telefonon indított edzésben órán rögzített, majd telefonon kihagyta a pihenőt; a következő gyakorlatnál az óra Folytatásban maradt. A #191 előtti mainben is létező eltérést hibázó Wear modell- és natív telefonos teszttel reprodukáltuk: a befejezett clock nem fut, ezért paused jelzést kapott; a natív pillanatkép emellett az előző, már kész gyakorlatot mutathatta.

Javítás: a befejezett, nem megszakított clock nem paused; a lezárult ismétléses pihenő pillanatképe a terv tényleges következő gyakorlatát mutatja. A Wear a korábbi telefonos complete/paused jelzést is normalizálja. A tényleges szünet/megszakítás, időalapú munkafázis, mért idő és az ismétlés kézi rögzítése megmarad. Telefonos OK nélkül kipróbálandó a következő sorozat és következő gyakorlat rögzítése, telefonon és órán kihagyott pihenővel is.

A [#200](https://github.com/fuloplevente1998/TrainPilot/issues/200) a #199 draft PR-ban készül a #191 mellett. A korábbi `0d4e9d7`/`9f7ff75` APK-kat ehhez az új körhöz nem adjuk át; új telefonos és órás build készül a javítással. Készülékes elfogadás nincs.

## Ellenőrzött tesztpár — `c503887`

Mindkét APK forrása: `c503887b5977cd2c3f1591d8b2ac02ee1d9becfd`. Verzió 1.3.4 /2730; a verziószám egyedül nem azonosítja a tesztbuildet. A fájlnevet és hash-t kell követni. Mindkét APK a korábbi aláírással frissíthető; aláírás, forrásazonosító, SHA-256, becsomagolt telefonos webes és natív forrás, Wear forrásarchívum és feature-jelenlét külön ellenőrizve.

| APK | SHA-256 | Sikeres build |
| --- | --- | --- |
| `TrainPilot-1.3.4-P1-191-200-c503887-Telefon.apk` | `3bb02f683ef2e1683c9fd6da2fac79369bb86013b407299f2dfdf34ceb13bd8a` | [38575222927](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38575222927) |
| `TrainPilot-1.3.4-P1-191-200-c503887-Wear.apk` | `2ccc65c1f9417fd8c2ad1c8af757891667438e209ddd2893428f8399d5de51ac` | [38575222264](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38575222264) |

Sikeres: **140 Node regresszió és 93 Chromium-csomag**, **156 telefonos + 108 Wear natív teszt** ([264 natív próba](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38575455390)), **86 valódi Wear-emulátoros teszt** ([38575222210](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38575222210)), [Quick validation](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38575222378), [Phase 7 performance](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38575460586). A 86 közül öt körkijelző/betűméret-kombináció ellenőrzi a #200 rögzítőgombot, és ugyanezeken futnak az outbox-írási hiba, duplikált nyugta és újrapróbálás próbák. A korábbi hibás queue-szimuláció versenyhelyzetét javítottuk; a teljes emulátoros kör újra sikeres.

Gépi bizonylat: [2026-10-14-p1-191-200.json](../builds/2026-10-14-p1-191-200.json). Az átadás utáni dokumentációs commit nem változtat az APK-k kódján. Nincs új release vagy main-egyesítés.

### Rövid készülékes ellenőrzés

1. **Mindkét új APK frissítendő.** Telefonon indított automata edzésben órán töltsd ki és rögzítsd az első sorozatot; telefonon hagyd ki a pihenőt. A következő sorozat és következő gyakorlat órás ismétlésmezője és Rögzítés gombja legyen használható külön telefonos OK nélkül. Órás pihenőkihagyással is próbálható.
2. Egy pihenő járjon le természetesen. Valódi szünetnél maradjon Folytatás, befejezett pihenőnél rögzítés. Időalapú körnél a kihagyás a következő munkafázist indítja, nem talál ki ismétlést.
3. #191: órás indítás és rögzítés telefon nélkül → helyi/várakozó állapot; kapcsolat visszatér → valódi nyugta után Szinkronizálva. Menü → Mentés és szinkron alatt újrapróbálás, appújranyitás, majd befejezés után egyetlen telefonos naplóbejegyzés. A meglévő mérési források külön maradjanak.

Nincs általános 45 perces minimum. A fizikai óra/telefon, lezárt kijelző és offline/online működés elfogadását a felhasználó visszajelzése igazolja; az emulátor és natív teszt nem helyettesíti ezt. A #191/#200 és a #199 draft PR addig nyitva marad.

## Kiinduláskori adatút és hiány (a módosítás előtt)

- `WearCommandOutbox.enqueue()` a sorozatparancsot SharedPreferencesbe írja és Data Layerre küldi. A lokális `commit()` eredménye jelenleg nincs ellenőrizve. A `putDataItem()` sikeres visszatérése után a lokális küldési sorból kiveszi a parancsot: ez a Data Layer-átadás sikere, nem annak bizonyítéka, hogy a telefon edzésadataiba már bekerült.
- `WearDataListenerService` jelenleg a `TYPE_CHANGED` eseményeket dolgozza fel; az általános parancsokra nincs az órás felületen közös, tartós telefonos visszaigazolási státusz. A Data Item törlés vagy a beérkező pillanatkép megfelelő bizonylatát csak az azonosítók és a telefonos tartós mentés feltételeinek ellenőrzése után szabad nyugtának tekinteni.
- `MainActivity.sendCommand()` az outbox sorszámát átírja a helyi edzéspillanatképbe. Az új UI-jelzésnek a valóban megőrzött helyi adatot, a kézbesítést és a telefonos elfogadást külön kell követnie.
- `WearClosureStore` már külön `pending`/`saved`/`discarded`/`error` lezárási eredményt használ, a beérkező eredményt `commandId` alapján illeszti. Ez újrafelhasználandó működés; a #158/#162 által elfogadott befejezés, értékelés, törlési védelem és automata idővonal megmarad.
- A Mini Napló a telefon kanonikus történetének offline nézete. Egy órán helyben megőrzött, még vissza nem igazolt parancs nem kerülhet bele második naplóbejegyzésként.

## Eredeti munkaterv és ellenőrzési feltételek

1. **Állapottábla és bizonylat:** stabil edzés-/sorozat-/parancsazonosítóval külön helyi mentés, szállításra várás, átadás és valódi telefonos nyugta. Térképezzük fel a meglévő natív és JS parancsbizonylatokat; ne épüljön párhuzamos mentési protokoll.
2. **Tartós átmenetek:** helyi írási hiba, sikertelen küldés, időtúllépés, kapcsolat-visszatérés és újraindítás. A sikeres Data Layer-küldés önmagában nem lehet „Szinkronizálva”. A sikertelen tartós írást nem jelezhetjük „Órán mentve” állapotként.
3. **Kompakt Wear UI:** „Mentés folyamatban”, „Órán mentve”, „Szinkronra vár”, „Szinkronizálva”, „Hiba / újrapróbálás”. A jelzés kis körkijelzőn és nagy betűvel is olvasható, a sorozatgombok és alsó számláló megmaradnak. Az Újrapróbálás a meglévő, azonosított műveletre vonatkozik.
4. **Regresszió:** valódi visszaigazolás vs. puszta átadás; párosított és önálló óra; offline → online; újraindítás; duplikált, késői és idegen eredmény; törlés után nincs feltámadás. Az automata, mért adatok, nyitott szerkesztő és Mini Napló megmarad.
5. **Átadás:** érintett telefonos/Wear tesztek és natív gate-ek, aláírt tesztbuild csak a működés bekötése után. Fizikai próba rövid működésellenőrzésből és szükség szerint normál edzés alatti megfigyelésből; nincs általános 45 perces minimum. Main-egyesítés a konkrét tesztbuild elfogadása után.

Kapcsolódó [#191](https://github.com/fuloplevente1998/TrainPilot/issues/191). Utána a következő P1 a [#163 telefonos térkép](https://github.com/fuloplevente1998/TrainPilot/issues/163). P2/P3 funkciók nem kerülnek ebbe a körbe.

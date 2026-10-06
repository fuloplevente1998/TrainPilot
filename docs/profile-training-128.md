# Profilvezérelt A/B program és főmenüikonok (#128)

Kiindulás: a felhasználó által elfogadott `b11a112c62e4520398cc8b2190a2c9f92837c840` (Actions 37362457898). A #127 adaptív navigációja visszavonva; a felső főmenü és az eredeti kompakt oldalak maradnak. A nyolc kis grafika Lucide SVG-re cserélve; az indítóikon változatlan.

## Programok és profil

- Az új telepítés üdvözlésből a meglévő kompakt profilpanelre lép, majd előnézet után aktivál. A félbehagyott folyamat újraindításkor folytatható, a kihagyás biztonságos alapot hagy.
- A `generatePersonalProgram` közös belépési pont. Ajánlott Alap/2. szint, explicit sablon vagy külön személyes felosztás választható.
- Az A/B sablonok a nagy gyakorlatokkal kezdődnek. A B napon a fekvőtámasz a 3. helyre kerül: bolgár guggolás → kétkezes rúddal evezés → fekvőtámasz → pullover → oldalemelés → tricepsz → hasprés. A 2. szinten a 3. gyakorlat a lábemelt fekvőtámasz. A sablonverzió 2; a már mentett programok csak külön elfogadott előnézet után frissülnek. Alapesetben két munkasorozat, nagy gyakorlatoknál 120–150, izolációknál 75 mp pihenő és 1–2 RIR szerepel. Három sorozatot megfelelő tapasztalat, izomépítési cél, legalább 60 perc és nem fizikai munkavégzés indokol.
- A 2. szint nehezebb gyakorlatokat, szűkebb céltartományt és terhelhető törzsvariációt használ. A profil felszerelése, kizárásai és kerülendő területei helyettesítést vagy elhagyást okozhatnak. Húzóeszköz nélküli alapozó tervnél a felület jelzi, hogy a törzsgyakorlat nem helyettesít húzógyakorlatot.
- A kezdősúly próbasorozattal állítandó. A beállítatlan súlyzós recept „Súly: beállítandó” jelzést és üres mezőt kap; nem jelenik meg előírt 0 kg-ként. A numerikus recept és a `trial` jelző megőrzi a régi mentések kompatibilitását. A kézzel mentett súly megszünteti a próbaállapotot, és előzmény nélküli edzésindításkor is kitölti a sorozatokat. A saját testsúly és az explicit megadott nulla külön eset. Magasság/testsúly alapján nem számolunk emelhető súlyt.
- Az előnézet egy rövid, tagolt összegzésben mutatja az időbecslést és ritmust. Az indokok és helyettesítések lenyithatók; a gyakorlatok a meglévő kompakt sorokat, kis videógombot és lenyitható receptadatokat használják. Nincsenek ismétlődő nagy videókártyák. Az előnézet nem szerkeszti az aktív programot; elfogadáskor új példány készül, a régi program és napló megmarad. Tárolási hiba esetén minden érintett kulcs visszaáll.
- Régi Alap/2. szint használónál opcionális frissítési előnézet van a Programokban. Nincs automatikus migráció.
- Beépített Alap/2. szint kézi aktiválása is a profilhoz igazított előnézetet nyitja, és csak annak elfogadása után vált programot.

## Coach és progresszió

- A generátor a legutóbbi 90 nap befejezett naplóját is használja. Egyező gyakorlat, súlyegység és variáció esetén átveszi a teljesített munkasúlyt, sorozatszámot és megtartja az ismétlési felső határt. Nem alakítja át a kg/kar értéket összsúllyá. Fájdalom, hiányos vagy érvénytelen adatok esetén nem állít be automatikus kezdősúlyt.
- A már teljesített teljes A/B nap megmarad, ha a tényleges és becsült idő is belefér a profil időkeretébe. Időkeret, felszerelés vagy kizárás miatti csökkentés külön megjelenik az előnézetben. A naplóból ismert gyakorlat kezdő profil mellett is megtartható, ha a felszerelés és kizárások engedik.
- Legfeljebb négy kompatibilis alkalom számít. Az elfogadott előnézet legfeljebb négy konkrét előzményhivatkozást tárol gyakorlatonként (`history128`); ezek új programazonosító után is folytatják a progressziót. Nem kapcsolódó programok nem indítanak automatikus emelést. Variációváltás törli az adott gyakorlat örökölt hivatkozásait.
- Egy gyenge nap célmegtartást okoz. Három egymást követő romló, teljesített alkalom kisebb terhelést javasolhat.
- Súlyemelés automatikusan csak három egymást követő, azonos terhelésű stabil felső-határ teljesítés, megfelelő erőfeszítés és friss kedvezőtlen jelzés nélkül lehetséges. Friss fájdalom vagy alacsony, adatból számolt readiness blokkolja.
- Testsúlyos gyakorlatnál stabil felső határ után nehezebb variáció következik; nincs automatikus végtelen ismétlés/tartás. A variáció előnézet és külön elfogadás után új programpéldányban jelenik meg.
- Több stabilan teljesített Alap-gyakorlat után a Coach felajánlja a 2. szint előnézetét.
- A programforrás, sablonverzió, létrehozáskori profil és variációs recept a meglévő program/adatmentési modellen belül marad; a backup és Drive ugyanazokat a mezőket viszi. A régi mentések továbbra is elfogadhatók.

## Szakmai háttér és implementációs döntés

Referencia: [ACSM 2026 Position Stand](https://pmc.ncbi.nlm.nih.gov/articles/PMC12965823/), DOI [10.1249/MSS.0000000000003897](https://doi.org/10.1249/MSS.0000000000003897). A forrás támogatja a több munkasorozatot, a nagy gyakorlatok előre helyezését és azt, hogy a bukás nem kötelező. A konkrét A/B gyakorlatlista, a 120/75 mp, a 1–2 RIR, a háromalkalmas küszöb és a readiness határ az alkalmazás konzervatív, tesztelt döntési szabályai; nem személyenként klinikailag validált előírások. Nem másoljuk a Blood & Guts splitet vagy annak kényszerített ismétléseit.

## Ellenőrzés és telefonos elfogadás

Új regresszió: `tests/browser/profile-training-128.cjs`. Új telepítés/újraindítás/kihagyás, közös generátor, 20–120 perc, felszerelés/kizárások, programhoz kötött trend, fájdalom/readiness, előnézet/rollback és backup/sync. A kompakt, csak olvasható előnézet, a videó megnyitása/visszatérés, a beállítatlan súly mentése/visszaállítása és a megadott kezdősúly tényleges edzésindítása is ellenőrzött. 320/360/393/412 px, négy nyelv, bezárógomb melletti cím és az eredeti felső, nyolc ikonos elrendezés ellenőrzése.

A `tests/browser/profile-history-128.cjs` elkülönített adatokkal ellenőrzi a 7 gyakorlat/17 sorozat, 5 kg és 12,5 kg átvételét, a rövidebb időkeret jelzését, az egység/variáció/fájdalom/hiányos/régi/jövőbeli adat védelmét, a változó napló miatti előnézetérvénytelenítést és a backup/Drive kört. Tényleges `startWorkout` → `finishWorkout` hívásokkal három teljesítés után 5 → 5,5 kg, majd három további után 6 kg lesz a következő edzés terhelése; a felhasználó naplójához nem nyúl. A közvetlen sablonelőnézet is ugyanazt a kompakt panelt használja, háttéroldali kártyák nélkül.

A meglévő UI-regressziók visszatérő felhasználói állapotot kapnak. A korábbi kétalkalmas automatikus progresszió-elvárás három alkalomra változik. A kezdő hosszú edzés továbbra is két sorozat; a több sorozat megfelelő profilhoz kötött.

Telefonon ellenőrizendő: meglévő napló/program változatlan, eredeti gyorsedzés és felső menü, új SVG-k minden témában, profil/előnézet bezárás, új terv elfogadása és aktív edzés terhelési mezői. A PR draft marad a felhasználó készülékes elfogadásáig.

# Profilvezérelt A/B program és főmenüikonok (#128)

Kiindulás: a felhasználó által elfogadott `b11a112c62e4520398cc8b2190a2c9f92837c840` (Actions 37362457898). A #127 adaptív navigációja visszavonva; a felső főmenü és az eredeti kompakt oldalak maradnak. A nyolc kis grafika Lucide SVG-re cserélve; az indítóikon változatlan.

## Programok és profil

- Az új telepítés üdvözlésből a meglévő kompakt profilpanelre lép, majd előnézet után aktivál. A félbehagyott folyamat újraindításkor folytatható, a kihagyás biztonságos alapot hagy.
- A `generatePersonalProgram` közös belépési pont. Ajánlott Alap/2. szint, explicit sablon vagy külön személyes felosztás választható.
- Az A/B sablonok a nagy gyakorlatokkal kezdődnek. A B napon a fekvőtámasz az izolációk elé kerül. Alapesetben két munkasorozat, nagy gyakorlatoknál 120–150, izolációknál 75 mp pihenő és 1–2 RIR szerepel. Három sorozatot megfelelő tapasztalat, izomépítési cél, legalább 60 perc és nem fizikai munkavégzés indokol.
- A 2. szint nehezebb gyakorlatokat, szűkebb céltartományt és terhelhető törzsvariációt használ. A profil felszerelése, kizárásai és kerülendő területei helyettesítést vagy elhagyást okozhatnak. Húzóeszköz nélküli alapozó tervnél a felület jelzi, hogy a törzsgyakorlat nem helyettesít húzógyakorlatot.
- A kezdősúly próbasorozattal állítandó. Magasság/testsúly alapján nem számolunk emelhető súlyt.
- Az előnézet az időbecslést, ritmust és helyettesítéseket mutatja. Aktív program soha nem írható át profilváltozáskor automatikusan. Elfogadáskor új példány készül; a régi program és napló megmarad. Tárolási hiba esetén minden érintett kulcs visszaáll.
- Régi Alap/2. szint használónál opcionális frissítési előnézet van a Programokban. Nincs automatikus migráció.
- Beépített Alap/2. szint kézi aktiválása is a profilhoz igazított előnézetet nyitja, és csak annak elfogadása után vált programot.

## Coach és progresszió

- Legfeljebb négy, ugyanazon programhoz és variációhoz tartozó alkalom számít; más program előzménye nem indít automatikus súlyemelést.
- Egy gyenge nap célmegtartást okoz. Három egymást követő romló, teljesített alkalom kisebb terhelést javasolhat.
- Súlyemelés automatikusan csak legalább három stabil felső-határ teljesítés, megfelelő erőfeszítés és friss kedvezőtlen jelzés nélkül lehetséges. Friss fájdalom vagy alacsony, adatból számolt readiness blokkolja.
- Testsúlyos gyakorlatnál stabil felső határ után nehezebb variáció következik; nincs automatikus végtelen ismétlés/tartás. A variáció előnézet és külön elfogadás után új programpéldányban jelenik meg.
- Több stabilan teljesített Alap-gyakorlat után a Coach felajánlja a 2. szint előnézetét.
- A programforrás, sablonverzió, létrehozáskori profil és variációs recept a meglévő program/adatmentési modellen belül marad; a backup és Drive ugyanazokat a mezőket viszi. A régi mentések továbbra is elfogadhatók.

## Szakmai háttér és implementációs döntés

Referencia: [ACSM 2026 Position Stand](https://pmc.ncbi.nlm.nih.gov/articles/PMC12965823/), DOI [10.1249/MSS.0000000000003897](https://doi.org/10.1249/MSS.0000000000003897). A forrás támogatja a több munkasorozatot, a nagy gyakorlatok előre helyezését és azt, hogy a bukás nem kötelező. A konkrét A/B gyakorlatlista, a 120/75 mp, a 1–2 RIR, a háromalkalmas küszöb és a readiness határ az alkalmazás konzervatív, tesztelt döntési szabályai; nem személyenként klinikailag validált előírások. Nem másoljuk a Blood & Guts splitet vagy annak kényszerített ismétléseit.

## Ellenőrzés és telefonos elfogadás

Új regresszió: `tests/browser/profile-training-128.cjs`. Új telepítés/újraindítás/kihagyás, közös generátor, 20–120 perc, felszerelés/kizárások, programhoz kötött trend, fájdalom/readiness, előnézet/rollback és backup/sync. 320/360/393/412 px, négy nyelv és az eredeti felső, nyolc ikonos elrendezés ellenőrzése.

A meglévő UI-regressziók visszatérő felhasználói állapotot kapnak. A korábbi kétalkalmas automatikus progresszió-elvárás három alkalomra változik. A kezdő hosszú edzés továbbra is két sorozat; a több sorozat megfelelő profilhoz kötött.

Telefonon ellenőrizendő: meglévő napló/program változatlan, eredeti gyorsedzés és felső menü, új SVG-k minden témában, profil/előnézet bezárás, új terv elfogadása és aktív edzés terhelési mezői. A PR draft marad a felhasználó készülékes elfogadásáig.

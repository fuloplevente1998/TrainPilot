# #171 — telefonos lenyíló nézetek gyorsítása

Az 1.3.3 /2729 felületén a lenyíló részek kezelői a `render()` műveleti hatókörén kívül futottak. Emiatt a napló többször újraolvasta és normalizálta az összes edzést; a programkártya több ezer alkalommal olvasta a nyelvi beállítást. A testsúlyrész nyitása a teljes Egészség-kártyát újraépítette. Emellett a dokumentum minden kattintására és `toggle` eseményére külön elrendezésmérés futott.

## Javítás

- A napló- és programrészletek ugyanazt a műveleten belüli pillanatképet használják, mint a korábban optimalizált oldalváltások. A pillanatkép a szinkron művelet végén megszűnik; a tároló írása azonnal érvényteleníti. Az aszinkron Health-beolvasás után végzett műveletek élő adatot kapnak.
- A már betöltött napló egészségablakának ismételt megnyitása a meglévő mérési gyorsítótár kulcsát használja. Az explicit Frissítés továbbra is újraolvas.
- A már betöltött programszerkesztő nem készíti újra a kontextusát; az új választómező helyben kapja meg a vezérlőjét.
- A testsúly nyitása/csukása helyben változtatja az állapotot, és megtartja a beírt értéket, a belső lenyílót és a pulzus részfáját. Mentés és időszakváltás továbbra is frissít.
- Az Edzés, Programok és Napló lenyílói nem mérik újra a változatlan navigációt. Az Egészség eseményei képkockánként összevonódnak. A navigáció méretváltozását `ResizeObserver`, a kijelzőváltozást `resize` követi, megfigyelő–újramérés hurok nélkül.

## Mérési módszer

Kiindulás: `5b9195eb8a90348fd1944067210f96bf8d548e42`, a #169 legújabb javítása. Ugyanaz a tényleges `www` felület és Playwright Chromium, 393×873 mobil nézet, hu-HU / Europe/Budapest, négyszeres CPU-lassítás. Egy naplózott edzésben nyolc gyakorlat és gyakorlatonként négy sorozat szerepel. Mind a nyolc vizsgált lenyíló négyszer nyílik és négyszer csukódik; az első nyitás még nem betöltött tartalomra történik. A kezelő idejét és a két animációs képkockáig eltelt időt, a tárolóolvasásokat, a Layout/UpdateLayoutTree eseményeket és a CPU-profilt rögzítjük.

Ez azonos körülmények között végzett böngészős összehasonlítás, nem készülékes WebView-időmérés. A képkockaidő érzékeny a futtató terhelésére, ezért az automatikus regressziós kapu a felesleges munkát tiltja, és nem egy bizonytalan millisekundumos küszöböt.

240 edzésnél, nyolc váltás összes munkája:

| Nézet | Teljes naplóolvasás előtte → utána | Elrendezésillesztés előtte → utána |
| --- | ---: | ---: |
| Naplózott edzés | 10 → 2 | 16 → 0 |
| Napló gyakorlatának szerkesztője | 8 → 4 | 16 → 0 |
| Testsúlynapló | 8 → 0 | 16 → 8 |
| Programkártya | 0 → 0 | 16 → 0 |

A programkártya nyelvbeállítás-olvasásai 3755-ről 1-re csökkentek. A naplózott edzés ismételt megnyitása már nem olvassa újra a naplót; egy új gyakorlat szerkesztőjének megnyitása egy friss olvasást végez.

240 edzésnél az első kattintástól a két képkockáig mért idő:

| Nézet | Előtte | Utána |
| --- | ---: | ---: |
| Programkártya | 107,7 ms | 44,6 ms |
| Naplózott edzés | 698,4 ms | 219,0 ms |
| Napló gyakorlatának szerkesztője | 154,2 ms | 111,4 ms |
| Testsúlynapló | 310,6 ms | 36,7 ms |

20 és 800 edzéssel is lefutott ugyanaz a profilozás. 800 edzésnél az első programkártya 254,0 → 58,2 ms, a naplózott edzés 3119,3 → 1099,1 ms, a testsúlynapló 504,5 → 27,9 ms volt. A napló gyakorlatának első megnyitása ezen a futáson 300,3 → 653,7 ms volt, miközben a négy megnyitás szerkesztőjének összes CPU-ideje 1033,5 → 892,4 ms-ra és a naplóolvasások száma 8 → 4-re csökkent. Kis adathalmaznál is ingadozott a képkockaidő. Ezért nem állítjuk, hogy minden egyedi kattintás gyorsabb: a nagy napló feldolgozásának és kirajzolásának maradó költségét a végső készülékes próbában is figyelni kell.

## Reprodukció és ellenőrzés

```bash
node tests/browser/disclosure-performance-171.cjs
TP171_ROWS=240 TP171_OUTPUT=ui-evidence/disclosure-profile-171 node tests/browser/disclosure-profile-171.cjs
```

Másik forrás összehasonlításához a `TP171_WEB_ROOT` környezeti változó adja meg annak `www` könyvtárát. A `TP171_ROWS` 20, 240 és 800 edzéssel használható; a `TP171_CPU` alapértéke 4. A profilozó teljes CPU-profilokat, Chrome trace-eket és `metrics.json` fájlt ír. A teljesítményteszt a normál Chromium-kiadási kapu része.

Ellenőrzés: hideg/ismételt nyitás, olvasási korlátok, helyi választómezők, megmaradó súly- és testsúlypiszkozatok, új naplóadat láthatósága, testsúly mentése/időszakváltása, navigáció átméretezése és a megfigyelőhurok kizárása. Az új teszt a kiinduló forráson elbukik a felesleges elrendezésillesztés miatt. A meglévő háttérfrissítési audit továbbra is ellenőrzi a szinkronizálás közben megmaradó nyitott tartalmat.

## Végső telefonos próba

Az új telefonos APK-val az Edzés, Programok, Napló és Egészség lenyílóit többször megnyitni/csukni, közben egy súly- és egy testsúlyértéket beírni. Ellenőrizni a gördülékeny reakciót, a nyitott állapot és piszkozat megmaradását háttérszinkron közben, majd a mentést és az új adatok megjelenését. A #171 készülékes elfogadása és a nyilvános 1.3.3 kiadás frissítése ennek visszajelzésére vár.

A telefonos tesztbuild örökli a #169 javításait; a Wear alkalmazás forrása ebben a változtatásban nem módosul. A hozzá tartozó legutóbbi órás tesztbuild a `5b9195e` forrásból készült `TrainPilot-1.3.3-Wear-kepernyok.apk`.

## Elfogadás és kiadás — 2026. október 12.

A felhasználó a legutóbbi telefonos (`083078d`) és órás (`5b9195e`) tesztbuildet mainre és 1.3.4 kiadásra elfogadta. A korábbi függő elfogadási státuszt ez felváltja. A kiadási verzió 1.3.4 /2730; [kiadási feljegyzés](releases/v1.3.4.md).

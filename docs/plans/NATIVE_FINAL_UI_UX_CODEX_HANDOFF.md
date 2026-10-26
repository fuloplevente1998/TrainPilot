# Végleges natív TrainPilot UI/UX – külön Codex átadó

Felhasználói irány, 2026-10-26. Repo fuloplevente1998/TrainPilot.
Olvasd AGENTS.md-t, docs/NATIVE_RESUME.md-t és az egész
**docs/design/NATIVE_DESIGN_HANDOFF.md** dokumentumot. Utóbbi a teljes
funkcióleltárt és a 24 mintanézethez tartozó részletes működési szerződést tartalmazza.

## Minőségi követelmény

A régi WebView-os app a funkcionális és használhatósági minimum. A grafit/arany
jóváhagyott vizuális irány az új felület alapja. A natív változat legyen áttekinthetőbb,
olvashatóbb és következetesebb, teljes funkciómegőrzéssel. Kevesebb külön oldal
lehetséges, kevesebb funkció vagy elvesző szerkesztési állapot nem.

Öt alsó fül: Kezdőlap, Edzés, Napló, Fejlődés, Egészség. Globális Settings fogaskerék
főoldalról és aktív edzésből, azonos sessionhöz visszatérve. Naptár/Coach/Programok
meglévő belépési útjai megmaradnak. Mind az öt főoldal ÉS az aktív súlyzós,
időalapú, kétoldali és GPS-edzés teljes vizuális kidolgozást kapjon.

Nem csak arany színű Material alapkomponensek a cél: egységes tipográfia,
ritmus, adat-hierarchia, kártyák, ikonok, állapotszínek, beviteli és hibajelzések,
üres/betöltő/offline/engedélyhiányos állapotok, kompakt időzítők. A sűrű adatok
és a főműveletek kis kijelzőn is áttekinthetők és elérhetők legyenek.
A valódi funkciót ne cseréld díszítő demo-kártyára vagy kitalált mérési adatra.

## Kötelező vizuális források

- docs/design/mockups/01-main-pages-and-planning.png
- docs/design/mockups/02-workouts-and-measurement.png
- docs/design/mockups/03-journal-maps-and-health.png
- docs/design/mockups/04-editing-and-settings.png
- docs/design/mockups/MANIFEST.json

Nézd meg a négy tényleges képet. A dokumentum és a mostani userutasítás az irányadó,
ha a képről funkció/Settings-gomb/állapot hiányzik. Nem kell újabb generált mockup.
A 24 kép nem a funkciók vagy a navigációs útvonalak felső korlátja.

## Most végrehajtható első szakasz – ütközésmentes előkészítés

A fix/native-phone-ui-final-gate chat MOST a teljes ui/**, phoneUItesztek és host
screenshot/evidence út tulajdonosa. A külön dizájn-Codex nem módosíthatja ezeket,
és nem húzhatja át automatikusan a másik saját workflow-ját.

Induló olvasási referencia: migration c116a042229f61b86babbbeb23c5d2c360486e20.
Új saját ág: feat/native-final-ui-ux, draft cél feat/native-android-migration.
Main és migrációs főág csak olvasható; nincs oda push/merge/force/version change.
Első szakaszban CSAK docs/design/final-ui-ux/** és saját átadó módosítható.

Készíts konkrét implementálható csomagot:
1. A tényleges main és a négy kép összevetésével nézetenként oldalfelépítés,
   elsődleges/másodlagos művelet, információs hierarchia és scroll/keyboard/back szerződés.
2. Meglévő Compose-fájl/route → tervezett vizuális komponens → meglévő state/callback
   megfeleltetés. Ne találj ki új üzleti logikát. Ha valódi funkció nincs bekötve,
   jelöld a pontos hiányt, ne fedd el vizuális pótlékkal.
3. Meglévő tématokenekből kialakított specifikáció: szövegszintek, méretek,
   térközök, sarkok, vonalak, kijelölések és állapotszínek. A main teljes32palettája
   és a HU/EN/DE/RO megmarad, nincs fix arany/fehér szín minden témán.
4. Elfogadási lista minden nézethez: kis kijelző, nagy font, hosszú német/román
   címke, TalkBack/sorrend/kontraszt/min48dp, üres/töltő/hiba és aktív session.
5. Kivitelezési sorrend és pontos fájlhatárok úgy, hogy a működő adatút és teszt
   assertionök megmaradjanak. Ellenőrzés saját meglévő repo-forrásból, nem csak vélemény.

Ez előkészítési szakasz, nem a kész app vagy a 24 képernyő megvalósításának állítása.

## Második szakasz – csak az UI-QA átadása és Root területfeloldása után

Akkor olvasd újra a migration aktuális integrált HEAD-jét; Root adja meg az exact
implementációs baseline-t és az ági frissítés módját. Ezt a feltételt ne tekintsd
automatikusan teljesültnek attól, hogy a másik chat egy köztes runja zöld.

Kizárólag a meglévő Compose megjelenítés és vizuális resource-ok alakíthatók át.
ViewModel publikus üzleti szerződés, edzésmotor, Room/DAO, migráció, backup,
Health/Wear/GPS writer, ownership, időzítő és finalizer nem változhat dizájnindokból.
Új komponensek meglévő state-et jelenítsenek meg, meglévő callbacket hívjanak.
Szemantikus testTag és accessibility jelentés marad; selektorváltozás csak indokolt
vizuális átalakítás következménye lehet, üzleti teszt nem gyengíthető.

Kezdőlap Folytatás, rest Skip/±15s, gyakorlatváltás, draft=pause, discard, undo,
pozitív mérés auto-rögzítés, két pozitív oldal, valódi Coach/Samsung prioritás,
nincs pulzusforrás → —, teljes egészség-/testsúlynapló mind megőrzendő.
A térkép piros X-e csak a térképet zárja; Settings nem állíthatja meg az edzést.

GitHubon valódi futó app/Compose UI ellenőrzés és változatlan
adb exec-out screencap -p PNG-k: előtte/utána azonos fixture/route/méret/téma/nyelv.
Minden főoldal és mérési mód, kritikus üres/hibaállapotok, több téma és mind4nyelv.
PNG/checksum/commit/artifact és tényleges tesztfutás kell; generált vagy módosított
látványkép nem elfogadás. E szakasz saját CI-szerződését az aktuális baseline-nál
Root egyezteti; most ne írj workflow-t vagy production UI-t.

## Modell és gondolkodási keret – javaslat, nem automatikus modellváltás

6.1 Sol megfelelő alap a repo-szintű Compose megvalósításhoz. Astra opcionális
független UX-kritikához/nehéz tradeoffhoz; nem előfeltétel. Vizuális specifikációhoz
Medium, összetett bekötéshez High, konkrét nehéz CI-debughoz Extra High javasolt.
Nagyobb thinking önmagában nem vizuális minőségbiztosítás: a döntő bizonyíték a
megépített képernyő és a működő folyamat. A felhasználónak jelezd a fázisváltást.

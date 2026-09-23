# TrainPilot 1.7.1 – Phase 4 Coach teljes gyakorlatlefedettség (teszt)

- A Coach / Statisztikák minden érvényes Napló-előzménnyel rendelkező gyakorlatot megjelenít, nem csak a súlyos/ismétléses típusokat.
- Külön kezeli a súlyos, testsúlyos ismétléses, időalapú és kétoldalas `mp/oldal` gyakorlatok statisztikáit.
- A következő edzés célprogramjának minden gyakorlatához készül Coach-javaslat; kevés előzménynél is látható induló/előzmény alapú állapot.
- A Coach „Következő edzés” főértéke programnév + edzésnap + tervezett dátum/idő összefoglalót mutat, a külön kontextus nélküli A/B érték megszűnik.
- 320–412 px szélességen a következő edzés összefoglaló tördelhető, vízszintes túlcsordulás nélkül.
- Android `versionCode 2660`, `versionName 1.7.1`.

# TrainPilot 1.7.0 – Phase 3 Napló véglegesítés

- A Napló összecsukott edzései kompaktak maradnak: név, dátum/idő, időtartam, gyakorlat- és sorozatszám.
- Lenyitva az Edzés összegzés egyetlen 1×4 stat sort használ ikonokkal: gyakorlatok, sorozatok, összes ismétlés, össz volumen.
- A „Mit edzettél?” gyakorlatlistában minden gyakorlat külön, alapból csukott; koppintásra helyben nyílik a kompakt szerkesztő.
- A gyakorlat szerkesztése támogatja a súly/ismétlés módosítását, sorozat hozzáadását és törlését, Mentés / Mégse / Gyakorlat törlése műveleteket.
- A naplózott edzéshez utólag új gyakorlat is hozzáadható; meglévő gyakorlat nem duplikálódik, ahhoz új sorozat adható.
- A gyakorlat-szerkesztő megnyitása gyorsult: már nem rendereli újra az egész Napló kártyát.
- A Health adatok a lenyitott edzésen belül maradnak; nyiluk jobb oldalra került és az Edzésfotók lenyitójával egységes.
- A külön felső Szerkesztés gomb és a duplikált Gyakorlatok szekció megszűnt.
- A Statisztikák részletei helyben nyílnak, a Journal / Statistics / Home chevronok egységesek.
- A #37 személyes edzéstervező kompakt overlay panelként működik, az alatta lévő útvonal megtartásával.
- A naplótörlés tombstone-védelemmel megakadályozza, hogy régi Drive snapshot visszahozza a törölt edzést.
- Release Gate #122: teljes Node regresszió, teljes Chromium/UI regresszió, signed APK build és APK/package/source ellenőrzés sikeres.
- Android `versionCode 2659`, `versionName 1.7.0`.

## #15 – Egységes videós bemutató (1.6.9, következő minor)

- Az aktív edzés „Bemutató / leírás” gombja egy koppintással nyitja a közös videó + leírás panelt. A köztes lenyíló rész megszűnt.
- Kompakt, témaszínt követő, mobilra méretezett panel; jobb felső, rögzített piros X és Android Vissza.
- Egyetlen külső videómegnyitó; a forrásinformáció külön szövegként olvasható.
- Megmaradnak az eddigi videóazonosítók, URL-ek és natív külső megnyitási útvonalak.
- Külön statikus és böngészős regressziós teszt 320 px szélességig.
- Verziójelzés változatlan a tesztkör alatt: 1.6.9 / 2658.

# TrainPilot 1.6.9 – Egészség felület finomhangolás

- Minor 2: a Testsúlynapló duplikált testsúly-kártyái és belső naplógombja kikerült; a Test és fittség rész csak Testzsír / SpO₂ / VO₂max adatokat mutat.
- Minor 2: az új testsúlymérés külön, alapból összecsukott „+ Új mérés” blokkba került; Változás és Minimum/Maximum marad mindig látható.
- Minor 2: a Pulzustrend „Regenerációs előzmények” része közvetlenül a mentett napi Alvás + HRV előzményekből épül, a Health Connect jogosultságok pedig kizárólag a Health Connect panelben maradnak.
- Minor 2: a Mai állapot dátuma közvetlenül a „Mai állapot” cím mellé került nagyobb, olvashatóbb méretben.

- Minor frissítés: a Pulzustrend helyben lenyílik és átveszi a korábbi külön Pulzus panel adatait/regenerációs előzményeit.
- A Test és fittség adatok a lenyíló Testsúlynaplóba költöztek; a külön Test és fittség kártya megszűnt.
- A pulzus- és testsúlygrafikonok szögletes töréspontok helyett simított, íves görbét használnak.

- Az Egészség oldal minden kiemelése az aktuális témaszínt követi; az alap témák mattak, glow/neon csak az élénk témáknál jelenik meg.
- A külön Egészség fejléc és alcím kikerült; a Mai állapot kártya közvetlenül tartalmazza az aktuális dátumot.
- A 2×4 fő navigáció minden aktív eleme ugyanazt a témaszínes kijelölési nyelvet használja.
- A Mai állapot jobb oldali nyílstílusa egységesen terjed a fő lenyitható/navigációs nyilakra.
- A testsúlynapló már nem nyit külön régi Health oldalt: a Pulzustrend alatt kompakt, lenyitható súlynapló jelenik meg, a meglévő mentési, szerkesztési és törlési adatok megtartásával.
- A 0 bpm nyugalmi pulzus hiányzó adatként jelenik meg (—), nem valós mérésként.
- Android `versionCode 2658`, `versionName 1.6.9`.

# TrainPilot 1.6.8 – Egészség dashboard vizuális frissítés

- Az Egészség oldal új, kék kiemelésű sötét dashboardot kapott a jóváhagyott referencia alapján.
- A Mai állapot blokk négy fő metrikát, regenerációs és nyugalmi pulzus státuszt, valamint valós 7 napos pulzustrendet jelenít meg.
- A Test és fittség, Pulzus, További Health-adatok és Health Connect területek egységes, kompakt lenyitható felületet kaptak.
- A Health Connect szinkron és engedélykezelés változatlanul elérhető; a #22–24 javítások megmaradnak.
- Android `versionCode 2657`, `versionName 1.6.8`.

# TrainPilot 1.6.2

- Kezdőlap: kompaktabb Személyes edzéstervező kártya; a 2×4 fő navigáció a felső, egységes alkalmazáschrome.
- Naptár: kisebb függőleges térközök és alacsonyabb napcellák, hogy a tervezési CTA több telefonon görgetés nélkül elérhető legyen.
- Coach / Naptár / Beállítások / Gyakorlatok panel: a piros bezáró X beljebb került, így nem takarja a panel keretét.
- Programok → Gyakorlatok / Izomcsoportok: kompaktabb fejléc és gyakorlatkártyák; a Programok navigáció aktív marad az alnézetben.
- Kezdőlap Health Connect összesítő: a törölt helyi edzéshez tartozó, TrainPilot által korábban Health Connectbe írt elavult session nem növeli tovább a mai edzésszámot.
- Android `versionCode 2651`, `versionName 1.6.2`.

# TrainPilot 1.6.1

- Android keyboard/IME: Quick Workout panel stays below the fixed 2×4 navigation and follows the visual viewport.
- Calendar, Coach and Settings panels sit closer to the navigation and no longer reserve a large blank row below the close button.
- Bilateral `mp/oldal` stopwatch now presents left/right as two distinct small cards.
- Left/right stopwatch values write back visibly to the active set row, persist in the workout draft, survive rerenders, and resetting one side leaves the other intact.
- Development toolchain: Capacitor CLI updated to 7.6.9 to remove the vulnerable legacy `tar` dependency chain.
- Android `versionCode 2650`, `versionName 1.6.1`.

# TrainPilot 1.6.0

- Quick Workout: compact context/search header beside the shared close control.
- Quick Workout: muscle and equipment menus stay anchored to their fields inside the panel.
- Journal: date filter now uses the shared CSS disclosure arrow.
- Navigation: removed the forced layout read, animated clip-path and full-screen backdrop blur; page/panel reveal is now lightweight.
- Theme picker behavior is unchanged from the phone-tested 1.5.5 build.
- Workout and Journal data formats remain compatible.

# TrainPilot 1.5.5 – Egységes navigáció és lenyitó vezérlők

- A felső 2×4 navigáció görgetéskor is ugyanazt a geometriát tartja; a Coach és a Beállítások nem zsugorodik össze és nem mozdul másik pozícióba.
- A fő navigáció feliratai nagyobb, reszponzív betűméretet kaptak, hosszabb fordításoknál legfeljebb két sorban törhetnek.
- A Programok, Edzésnapok, gyakorlatok, Beállítások, Napló és könyvtári kártyák lenyitói egységes, egyszerű fehér háromszöget használnak: zárt állapotban jobbra, nyitott állapotban lefelé mutat; a korábbi dupla jelölés megszűnt.
- Az Egészség és Naptár oldal felső címsora vékony, lekerekített kártyakeretet kapott, a többi fő nézet vizuális rendszeréhez igazítva.
- Android `versionCode 2648`, `versionName 1.5.5`.

# TrainPilot 1.5.4 – Mobil navigáció és edzésfelület finomítás

- A felső 8 fő művelet egységes 2×4-es mobilrácsot kapott: Kezdőlap, Edzés, Naptár, Programok / Napló, Egészség, Coach, Beállítások.
- A TrainPilot név kisebb lett, az intelligens edzéstervező felirat ugyanabba a fejlécsorba került.
- A Beállítások továbbra is ikon-only elem; a többi fő navigáció ikon + felirat formátumú.
- A lenyitó nyilak az alkalmazás felületein egységes, keret nélküli, kártyába beleolvadó átmenetes hátteret kaptak.
- A gyakorlat-videók play jelzése most biztosan a külső gombkereten belül marad.
- Aktív edzésnél a rövid gyakorlatleírás és a „Bemutató megnyitása” visszakerült a felső kártyába egy kompakt, lenyitható Bemutató blokkban.
- A korábbi alsó külön bemutató-kártya megszűnt, így a sorozatok és navigáció kompaktabb marad.
- Javítva a pihenőidőzítő érintési rétege: a fix időzítő háttere nem nyeli el az alatta lévő edzésgombok koppintását, a „Kihagyás” gomb továbbra is használható.
- Android `versionCode 2647`, `versionName 1.5.4`.

# TrainPilot 1.5.3 – UI/funkcionális finomítások és regressziós javítások

- A Coach Kezdőlapról és Egészségből azonos felületet használ, megjegyzi a nyitási helyet, és a kapcsolódó edzés/Health célokra közvetlenül navigál.
- A testsúly, alvás és HRV statisztikák kattintható részleteket kaptak.
- Az aktív edzés kompaktabb fejlécet, sűrűbb sorozatnézetet és laposabb videó/demó vezérlést kapott.
- A Gyors edzés kompakt felületén helyreállt a gyakorlat hozzáadása és a helyes „Gyors edzés” fejléc.
- A videómodal Android Vissza gombbal / gesztussal zárható, mielőtt az alkalmazás navigálna.
- Az egyszerű sikeres műveletek több helyen nem blokkoló toast visszajelzést használnak.
- A sticky navigáció kompakt módban egy sorban tartja a fő füleket és a Coach/Beállítások műveleteket.
- Az alap témaszínek visszafogottabbak, az élénk színpaletta külön opcióként megmarad.
- Javítva a kompakt saját időválasztó fókuszváltási hibája, amely begépelt órát vagy percet mentés előtt visszaállíthatott.
- A teljes JavaScript- és Chromium UI regressziós csomag sikeresen lefutott a kiadási verzió előtt.
- Android `versionCode 2646`, `versionName 1.5.3`.

# TrainPilot 1.5.2 – hierarchikus UX és Android visszalépés

- Az Edzés, Programok, Gyors edzés, személyes terv, Napló és Beállítások természetesebb, egymásba ágyazott lenyitható hierarchiát kaptak.
- A lenyitható kártyák teljes fejlécfelülete használható; a külön művelet- és videógombok nem nyitják vagy zárják a szülőkártyát.
- Az aktív program felépítése Program → edzésnap → gyakorlat, a gyakorlat szerkesztése helyben történik.
- A Gyors edzés beállításai és a Napló edzésszerkesztése helyben nyílnak meg; a Programok napjai és a saját program napjai szintén hierarchikusak.
- A személyes terv öt fő csoportra tagolódik, a kerülendő területek és az egyedi gyakorlatkizárások külön alcsoportok maradnak.
- A kompakt saját dátum/idő választó fekvő, kis magasságú nézetben is megőrzi a beírt órát és percet.
- Az Android rendszer Vissza gombja és visszahúzó gesztusa először a legbelső nyitott elemet zárja: időválasztó/dialógus → dropdown → inline szerkesztő → lenyitott panel → előző nézet → Kezdőlap.
- A Kezdőlapon és a többi fő nézetben a Coach, Beállítások és mind a hat fő navigációs fül sticky marad görgetés közben; a TrainPilot márkanév/szlogen külön, görgethető fejlécben marad.
- Az 1.5.2 teszt-APK csak a teljes JavaScript- és Chromium UI regressziós csomag sikeres lefutása után épül; a forráscsomag buildkor megkapja a pontos Git commit azonosítót.
- Android `versionCode 2645`, `versionName 1.5.2`.

# TrainPilot 1.5.1 – UNIFIED UI finomhangolás

- A hat fő navigációs pont nagyobb feliratot és ikont kapott; az aktív elem szövegkontrasztja javult.
- Az Egészség napi kártyáján az aktív energia megnevezése „Napi aktív kalória” lett.
- Az időválasztó a nagy órarács helyett kompakt óra/perc választót használ.
- A Beállítások oldal egységesebb, kompaktabb kártyadizájnt kapott; a nyelvválasztó zászlós jelölést használ.
- A témaszíneknél külön visszafogott alapszín-paletta és élénk színpaletta választható.
- Az Edzésnaptár gyorshivatkozása, a pihenőidő és a progresszív súlylépcsők kikerültek az általános Beállításokból.
- A pihenőidő és progresszív súlylépcsők az Edzés oldalon, az aktív programhoz tartozó „Edzés beállításai” panelen kezelhetők.
- A Gyakorlatok / Izomcsoportok kártyáin a videóindító és lenyitó nyíl az Edzés oldal programgyakorlatainak vezérlőstílusát követi.
- Android `versionCode 2644`, `versionName 1.5.1`.

# TrainPilot 1.4.6 – kompakt, egységesített felület

- Az Edzés fül gyakorlatrészletei helyben lenyílnak; az A/B edzésnap címe teljes programnévvel jelenik meg.
- A Napló összecsukott edzései és a Naptár tervezett edzései kompaktabb kártyákat kaptak.
- A Naptárban a Törlés a Kihagyás fölé került, az eredeti veszély/szekunder színekkel.
- A Kezdőlapon a következő edzés teljes programnap-neve nagyobb hangsúlyt kap.
- Az Egészség / Mai állapot és Test és fittség kártyák kompaktabbak.
- A naplófotó törlése már TrainPilot-saját megerősítő ablakot használ; a globális aktív confirm/prompt útvonalakat Chromium teszt ellenőrzi, hogy ne nyissanak natív WebView dialógust.
- Android `versionCode 2630`, `versionName 1.4.6`.

# TrainPilot 1.4.5 – UI / Health / WebView dialógus audit

- A korábban natív, szürke WebView `confirm()` / `prompt()` ablakot használó aktív útvonalak TrainPilot-stílusú saját dialógust kaptak.
- Lefedve többek között: testsúly szerkesztés/törlés, félbehagyott vagy részleges edzés, programműveletek, naptári áthelyezés, biztonsági mentés visszatöltés, Google-fiók és szinkronütközés.
- Az Egészség / Test és fittség kártya Health Connect testsúly hiányában a kézzel rögzített testsúlyt mutatja.
- Javítva a Kezdőlap → Segíts elkezdeni, valamint a Coach és Statisztikák görgetési állapota.
- A Napló Health blokkja és az edzés időablakhoz kötött Health-adatok megjelenítése egységesebb.
- Kamera, Xiaomi/helyi galéria, MediaStore, Drive fotószinkron és Progresszív edzés logika regressziós tesztekkel védett.
- A `main` kiadás előtt a teljes regressziós és Chromium UI tesztcsomag, valamint a 2629 WebView dialógus audit zölden lefutott.
- Android `versionCode 2629`, `versionName 1.4.5`.

# TrainPilot 1.4.4 – Hivatalos Capacitor kamera/galéria

- A Kamera és Kiválasztás gomb többé nem a saját `WorkoutPhotosPlugin.startActivityForResult()` útvonalat használja.
- A natív felületet a hivatalos `@capacitor/camera` 7.0.5 plugin nyitja meg.
- A kapott kép ezután visszakerül a saját `WorkoutPhotos.storeDataUrl()` folyamatba, ahol továbbra is privát app-tárhelyre kerül és JPEG-re normalizálódik.
- `saveToGallery:false`: a fotózás nem ment külön példányt a telefon galériájába.
- Drive-szinkron, meglévő naplófotók, stopper és Progresszív edzés logika változatlan.
- Android `versionCode 2617`, `versionName 1.4.4`.

# TrainPilot 1.4.3 – Képválasztó valódi készülékes javítása

- A galéria `Kiválasztás` útvonala nem használja többé a problémás rendszer Photo Pickert.
- Közvetlen `ACTION_OPEN_DOCUMENT` / Storage Access Framework indul `image/*` szűréssel; továbbra sincs teljes galéria- vagy tárhelyengedély.
- A kamera, privát fotótárolás, JPEG-normalizálás, Drive-szinkron, stopper és Progresszív edzés logika változatlan.
- Android `versionCode 2616`, `versionName 1.4.3`.

# TrainPilot 1.4.2 – Fotóindítás + stopper GUI

- Kamera és rendszer képválasztó indítása explicit Android főszálon; Photo Picker hiányakor megmarad az `ACTION_OPEN_DOCUMENT` fallback.
- A stopper az aktív edzés tetejére került, nagyobb kijelzéssel és egyértelmű `Stop és rögzítés` / rögzítési visszajelzéssel.
- A felhasználói `Progresszív terhelés 2.0` megnevezés `Progresszív edzés` lett; a progressziós döntési logika nem változott.
- Android `versionCode 2615`, `versionName 1.4.2`.

# TrainPilot 1.4.1 – Coach belépési hotfix

- A Kezdőlap „Részletek” gombja ugyanazt a Coach nézetet nyitja meg, mint az Egészség fül.
- A Coach megnyitásakor Health kontextus aktív, ezért a Kezdőlap-specifikus kompakt dekorátor és `overflow:hidden` nem kerül a Coach képernyőre.
- Javítva a Kezdőlapról nyitott Coach eltérő kinézete és befagyó/nem görgethető állapota.
- A Progresszív terhelés 2.0 logikája változatlan.
- Android `versionCode 2614`, `versionName 1.4.1`.

# TrainPilot 1.4.0 – Progresszív terhelés 2.0

- Az utolsó 2–3 azonos gyakorlatot is figyelembe vevő, konzervatív progresszió.
- Aktív program prescription elsőbbséget élvez a gyakorlatkönyvtár alapértékével szemben.
- RIR célok külön parserágon futnak; a `2–3 RIR` nem értelmezhető 2–3 ismétlésnek.
- `Túl könnyű` önmagában nem emel súlyt; automatikus emeléshez stabil felső teljesítés kell.
- Kicsit nehéz / túl nehéz / fájdalom blokkolja az emelést.
- Gyakorlat- és terheléstípus-specifikus súlylépcsők, megfigyelt korábbi lépcsők figyelembevételével.
- Testsúlyos gyakorlatok ismétlés → stabil felső cél → variáció/tempó/ROM irányban haladnak.

# TrainPilot 1.3.1 – fotógomb hotfix

- Javítva a Napló „Fotó hozzáadása” gomb Android/WebView eseménykezelése.
- A fotómodal gombjai explicit click-listenereket használnak az inline események helyett.
- Kamera/képválasztó indításakor azonnali státusz-visszajelzés jelenik meg.
- Az 1.3.0 összes fotó-, Drive- és Play-előkészítési funkciója változatlanul megmarad.
- Android `versionCode 2612`, `versionName 1.3.1`.

# TrainPilot 1.3.0 – Workout Photos + Play readiness

- Edzésenként opcionális naplófotó: Edzés előtt / Edzés után / Egyéb.
- Kamera vagy rendszer képválasztó, széles médiatár-jogosultság nélkül.
- Privát, optimalizált JPEG tárolás legfeljebb 1600 px hosszú oldallal; az újramentés nem viszi tovább az eredeti EXIF/GPS metaadatot.
- Naplóban lazy fotóelőnézet, teljes képes megnyitás és törlés.
- Google Drive `appDataFolder` bináris fotófeltöltés, ellenőrzés, igény szerinti letöltés és törlés.
- Fotómetaadat bekerül a meglévő edzésnapló/Drive szinkronba; a fotófájl nem kerül base64-ként a localStorage JSON-ba.
- Fotóeltérés esetén a csak-fotó jellegű naplóváltozások összeolvaszthatók kézi konfliktus nélkül.
- Android 16 / API 36 target előkészítés a 2026-os Google Play beküldéshez.
- Play Store / OAuth / privacy dokumentációs checklist hozzáadva.
- Android `versionCode 2611`, `versionName 1.3.0`.

Fizikai telefonos kamera, Photo Picker, Google OAuth és Drive fotószinkron teszt szükséges a funkció végleges igazolásához.

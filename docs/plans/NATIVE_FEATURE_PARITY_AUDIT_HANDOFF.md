# Külön chatnek átadható feladat: main → natív funkcióleltár

2026-10-25. **A felhasználó átadta külön chatnek.** Ág/PR/riport még nem
érkezett vissza, az audit eredménye nem igazolt. A fő munkás közben a Kezdőlap napi
számláló/célprogramnap/start/invalidation bekötését folytatja a migrációs ágon.

## Másolható feladat

A TrainPilot meglévő és natív alkalmazásának funkcióegyezését auditáld.
Repo: https://github.com/fuloplevente1998/TrainPilot

- Main rögzített referencia: **7473b30f387eda54186c1e31830304ca12bd104f**.
- Natív vizsgálati bázis: **a5f562d526a69e6584d33d0ef3238cb49e5769d4** a
  feat/native-android-migration ágon; alkalmazáskódja7d62ae3.
- Saját új ág: **test/native-feature-parity-audit**, a fenti natív bázisból,
  külön clone/worktree. Más agent munkafáját/ágát ne váltsd át.
- A feladat **elemzés és egy dokumentum**. Production kódot, tesztet, workflow-t,
  Gradle-t, verziót, AGENTS.md-t, main/migrációs/külső lokalizációs ágakat ne módosíts.
- Csak ezt az új fájlt írd:
  **docs/reviews/NATIVE_FEATURE_PARITY_AUDIT.md**.

Olvasd el: AGENTS.md, docs/NATIVE_RESUME.md, a docs/WORK_HANDOFF.md teteje,
docs/NATIVE_PARITY_AUDIT.md, docs/plans/NATIVE_FINAL_DELIVERY.md és
docs/design/NATIVE_DESIGN_HANDOFF.md. Ez az explicit külön docs-audit feladat
ágkivétel a fő migrációs munkás általános „csak migrációs ág” szabálya alól;
számodra kizárólag a saját auditág írható.

### Az audit öt területe

1. **Egészség/egészségnapló/testsúly:** összes main-metrika, mai és korábbi napok,
   részletek/forrás/idő, alvásfázisok, kézi mérésműveletek, Health Connect/
   Samsung/BLE/Wear állapot és engedélyek; hiányzó adat ne legyen hamis siker.
2. **Napló és fejlődés:** keresés/szűrés/részletek/szerkesztés/törlés/PR,
   fotók/útvonal elérése, grafikonok, lapozás/korlátozott adatlekérés tényleges útja.
3. **Programok és gyakorlatok:** könyvtár/kedvencek/videó/saját gyakorlat,
   program- és napszerkesztés/klónozás, profil/előnézet/aktiválás/terv-változatok.
4. **Beállítások és elérhetőség:** teljes téma- és nyelvválaszték, edzésidőzítő,
   kapcsolt eszközök, adatvédelmi/mentési/engedély-beállítások; minden meglévő
   user action megtalálható-e valamelyik új natív útvonalról.
5. **Edzésfelület:** súly/ism/RIR, alsó stopper/kétoldali/GPS/kézi mérés,
   intervallumpreset/pihenő/gyakorlatváltás, undo, draft/resume, save/discard.
   Itt UI→callback→motor/forrás/nyugta láncot vizsgálj, ne új motort tervezz.

A fő munkásnál marad a Home/Coach/Naptár bekötés, cloud/backup/szinkron és
konkurencia javítása. Ezeket ne vedd át implementálni. Kapcsolódó fontos
hiányt feljegyezhetsz, de ismert folyamatban lévő tételt ne adj el új felfedezésnek.

### Módszer és bizonyíték

- A main valódi felhasználói műveleteiből indulj: www/index.html, www/app.js és
  ténylegesen bekötött feature CSS/JS/natív bridge. A végső rf*/tp* felülírást
  kövesd, ne az első vagy már felülírt függvénydefiníciót. Halott/teszt/demókód
  puszta létezése nem termékfunkció-követelmény.
- Natív oldalon nav route → látható vezérlő → callback → VM/repository → valós
  motor/Room/provider → mentés/hibajelzés útját kövesd. Kódblokk vagy adatmodell
  megléte önmagában nem bekötött funkció. Valós provider helyett prototípus/fake
  használatát nevezd meg, de szenzor-hozzáférést forrás alapján ne állíts igazoltnak.
- Az új dizájn/eltérő elhelyezés elfogadott: a mockup24 képe nem funkciós plafon,
  és nem kell a main régi layoutját lemásolni. Elérhetőség és viselkedés a mérce.
- Minden sorhoz main exact-ref fájl/sor/handler + native exact-ref fájl/sor/
  route/callsite + kapcsolódó tesztazonosító vagy „nincs” kell.
- Állapotok: **bekötve, csak kódblokk/modell, részleges, hiányzik, futtatással
  nem igazolt**. Az utolsó külön dimenzió: a nem futtatott teszt nem funkcióhiány,
  de nem is zöld eredmény. A bázison519 JVM+48 Phone UI-kód futtatásra vár;
  utolsó teljes migrációs zöld dd4cc27/40072751592, nem a mostani kód bizonyítéka.
- P1/P2 csak konkrét trigger/hibás adatút alapján, path:line és következmény
  megadásával. Különítsd el a mainből hiányzó funkciót, a már mainen is meglévő
  hibát és a puszta ellenőrzési hiányt; ne legyen feltételezett hosszú hibajegyzék.
- Ismert saját munkánk: Home mai edzésszám még teljes-napló count; cél nap még
  első nap és csak dayId indul. Ezek javítása folyamatban a fő agentnél.
  #240/#241 kódja már squash integrálva, valódi nyelvválasztóval; a régi jelentés
  „nincs nyelvgomb/nem integrált” állapotát ne ismételd aktuális hibaként.
- Az auditot a rögzített bázison végezd. Átadáskor olvasd ki a migráció frissebb
  HEAD-jét, és csak a megállapításokat érintő deltakódot nézd: az időközben javított
  tétel kapjon SHA-s pontosítást, ne mozgasd észrevétlenül a vizsgálati bázist.

### Átadás

Egy áttekinthető megfeleltetési táblázat és a tetején rövid, prioritásos
hiánylista + legfeljebb néhány összetartozó javítási csomag. Minden kijelölt
terület legyen lefedve; bizonyítékhiánynál „nem eldönthető”, ne kitalált lezárás.

Kis **[skip ci]** dokumentációs commit, push csak a saját auditágra, külön draft
PR célja **feat/native-android-migration**. Ne merge-elj, ne zárj más PR-t/issue-t,
ne küldj GitHub-kommentet. Teljes Gradle/emulátor/APK és új CI-futtatás/workflow
ne induljon. Tesztkódot se írj; a fő agent végzi az összevont kaput.

GitHub-írási lehetőség nélkül a dokumentum/patch és exact-ref riport is elég.
Ha a pinned forrást nem tudod elérni, ezt jelöld pontosan; ne állíts teljes auditot
vagy GitHub-/tesztműveletet az anyag tényleges elérése nélkül.

Magyarul, tömören add át: vizsgált refek, lefedettség, bizonyított hiányok,
megírt dokumentum és ténylegesen végzett ellenőrzés. Implementációt ne kezdj.


Utólagos migrációs delta: **e4920f4** javítja a Home mai számlálóját. A rögzített
a5f562d bázist ne mozgasd; a saját jelentés végső deltaellenőrzésekor ezt a tételt
már javítottnak jelöld. Célprogramnap/start/invalidation továbbra a fő munkásé.

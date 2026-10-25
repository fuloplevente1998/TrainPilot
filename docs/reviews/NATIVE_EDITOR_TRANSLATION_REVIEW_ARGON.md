# Natív szerkesztők fordítási és szövegkövetkezetességi auditja

- **Vizsgált referencia (commit):** `18038924674c7fcfc7f648054d5830ccbf011018` (`feat/native-android-migration` ág)
- **Vizsgált erőforrásfájlok:**
  - `android/app/src/main/res/values/native_journal_editor.xml` (HU – 20 string)
  - `android/app/src/main/res/values-en/native_journal_editor.xml` (EN – 20 string)
  - `android/app/src/main/res/values-de/native_journal_editor.xml` (DE – 20 string)
  - `android/app/src/main/res/values-ro/native_journal_editor.xml` (RO – 20 string)
  - `android/app/src/main/res/values/native_program_prescription.xml` (HU – 17 string)
  - `android/app/src/main/res/values-en/native_program_prescription.xml` (EN – 17 string)
  - `android/app/src/main/res/values-de/native_program_prescription.xml` (DE – 17 string)
  - `android/app/src/main/res/values-ro/native_program_prescription.xml` (RO – 17 string)
- **Összes vizsgált string:** 148 string (8 fájl)

## Rövid összesítés

1. **Kulcskészlet és formázási paraméterek:** Mind a 4 nyelvi változatban hiánytalanul és azonos sorrendben szerepel a `native_journal_editor.xml` 20 kulcsa és a `native_program_prescription.xml` 17 kulcsa. Az egyetlen paraméterezett string (`program_rx_title`: `%1$s`) minden nyelven pontosan egyezik.
2. **XML és Android string-escape:** Mind a 8 XML-fájl szintaktikailag szabályos; nincs escape-eletlen aposztróf (`'`), idézőjel (`"`), `&`, `<`, `>` vagy hibás `%` karakter. A hármaspont (`…`) és a nagykötőjel (`–`) használata egységes.
3. **Tartalmi és működési megfelelés:** Az edzésnapló-szerkesztő (mért értékek utólagos módosítása, egészségmérések eredeti időablakhoz kötése) és a programcélérték-szerkesztő (előírt célértékek, próbasúly üresen hagyása) funkcionális jelentése egyik nyelven sem mosódik össze.
4. **Főbb nyelvi és szakmai észrevételek:**
   - **2 biztos hiba (félreérthető szerkezet / pontatlan szakterminológia):**
     - Magyar `journal_edit_failed`: *„Ha az edzés közben változott…”* – a névutós szerkezet miatt ez magyarul azt jelenti, hogy *„az edzés ideje alatt változott”*, nem pedig azt, hogy *„az edzés időközben (a háttérben) módosult”*.
     - Német `journal_edit_rir`: *„Verbleibende Wiederholungen”* – jelentése *„hátralévő (még elvégzendő) ismétlések”*, miközben a RIR szakmai jelentése *„tartalékban maradó ismétlések”* (*Wiederholungen in Reserve*).
   - **Stiláris és terminológiai következetességi javaslatok:** magyar helyesírási pontosítás (`időzóna-eltolás`), súly/terhelés/tömeg megnevezések egységesítése (`Egy kézisúlyzó súlya (kg)`), mértékegység-rövidítések összehangolása a két szerkesztő között (`másodperc` vs. `mp`), valamint kisebb angol és román megfogalmazási finomítások.
   - **Felületi ellenőrzést igénylő elemek:** a `journal_edit_failed` többmondatos hibaüzenet (213–246 karakter), a másodperc-alapú oldalcímkék (`journal_edit_left`/`right`), a német `journal_edit_rir` címke (43 karakter), valamint a `program_rx_apply` gombfelirat (20–22 karakter HU/DE/RO nyelven).

---

## Érdemi találatok részletezése

### 1. `journal_edit_failed` (HU) – Kétértelmű névutós szerkezet és szöveghossz

- **Nyelv:** HU
- **Fájl és stringkulcs:** `android/app/src/main/res/values/native_journal_editor.xml` – `journal_edit_failed`
- **Jelenlegi szöveg:** `Nem sikerült menteni. Ellenőrizd az értékeket: a befejezés a kezdés után legyen, legfeljebb 24 órán belül és ne a jövőben; a kész sorozatokhoz pozitív mért érték kell. Ha az edzés közben változott, zárd be és nyisd újra a szerkesztőt.`
- **Javasolt teljes szöveg:** `Nem sikerült menteni. Ellenőrizd az értékeket: a befejezés a kezdés után legyen, legfeljebb 24 órán belül és ne a jövőben; a kész sorozatokhoz pozitív mért érték kell. Ha az edzés időközben módosult, zárd be és nyisd újra a szerkesztőt.`
- **Rövid indok:** A *„Ha az edzés közben változott”* mondatban a *közben* szót az olvasó az *edzés* főnév névutójaként értelmezi (*„az edzés folyamatában / ideje alatt”*), holott a kívánt jelentés határozói: az edzés adata *időközben* (a szerkesztő megnyitása óta) változott meg. Emellett a 236 karakteres szöveg megjelenítését érdemes keskeny kijelzőn ellenőrizni.
- **Besorolás:** biztos hiba (a *„közben”* → *„időközben”* jelentéstorzulás miatt) / felületi ellenőrzést igényel (a szöveghossz miatt)

---

### 2. `journal_edit_rir` (DE) – Pontatlan edzéselméleti szakkifejezés és címkehossz

- **Nyelv:** DE
- **Fájl és stringkulcs:** `android/app/src/main/res/values-de/native_journal_editor.xml` – `journal_edit_rir`
- **Jelenlegi szöveg:** `Verbleibende Wiederholungen (RIR, optional)`
- **Javasolt teljes szöveg:** `Wiederholungen in Reserve (RIR, optional)`
- **Rövid indok:** A *„Verbleibende Wiederholungen”* kifejezés a sorozatból még hátralévő (elvégzendő) ismétlésekre utal. A RIR (*Reps in Reserve*) német szaknyelvi megfelelője a *„Wiederholungen in Reserve”* (tartalékban maradó ismétlések). A 41–43 karakteres címke beviteli mező feletti/melletti elhelyezése UI-ellenőrzést is igényel.
- **Besorolás:** biztos hiba (szakmai félrefordítás) / felületi ellenőrzést igényel (a címke hossza miatt)

---

### 3. `journal_edit_time_hint` és `journal_edit_invalid` (HU) – „időzóna-eltolás” helyesírása

- **Nyelv:** HU
- **Fájl és stringkulcs:** `android/app/src/main/res/values/native_journal_editor.xml` – `journal_edit_time_hint`, `journal_edit_invalid`
- **Jelenlegi szöveg:**
  - `journal_edit_time_hint`: `Dátum és idő: 2026-10-25 18:30:00 +02:00. Az utolsó érték az időzóna eltolása.`
  - `journal_edit_invalid`: `Ellenőrizd a dátumokat és az időzóna eltolását.`
- **Javasolt teljes szöveg:**
  - `journal_edit_time_hint`: `Dátum és idő: 2026-10-25 18:30:00 +02:00. Az utolsó érték az időzóna-eltolás.`
  - `journal_edit_invalid`: `Ellenőrizd a dátumokat és az időzóna-eltolást.`
- **Rövid indok:** A magyar szaknyelvben a *time zone offset* megfelelője a német *Zeitzonenversatz* mintájára az összetett **időzóna-eltolás** (háromtagú, hét szótagos összetételként az AkH. 138. alapján kötőjeles), ami természetesebb és tömörebb a birtokos szerkezetnél.
- **Besorolás:** stiláris javaslat

---

### 4. `program_rx_weight_single` (HU) – Terminológiai következetesség (súly vs. tömeg)

- **Nyelv:** HU
- **Fájl és stringkulcs:** `android/app/src/main/res/values/native_program_prescription.xml` – `program_rx_weight_single`
- **Jelenlegi szöveg:** `Egy kézisúlyzó tömege (kg)`
- **Javasolt teljes szöveg:** `Egy kézisúlyzó súlya (kg)`
- **Rövid indok:** A két szerkesztőben a súlyra a `Súly (kg)` (`journal_edit_weight`), a `próbasúllyal` (`program_rx_trial`) és a `Terhelés (kg)` (`program_rx_weight*`) kifejezések szerepelnek; az EN/DE/RO változatok is egységesen a `weight` / `Gewicht` / `Greutatea` szót használják. A fizikai *„tömege”* helyett az edzőtermi nyelvhasználatban a *„súlya”* természetesebb és egységesebb.
- **Besorolás:** stiláris javaslat

---

### 5. `journal_edit_seconds`, `journal_edit_left`, `journal_edit_right` (HU, EN, DE, RO) – Mértékegység-rövidítések egységesítése és mezőcímkék hossza

- **Nyelv:** HU, EN, DE, RO
- **Fájl és stringkulcs:** `android/app/src/main/res/values*/native_journal_editor.xml` – `journal_edit_seconds`, `journal_edit_left`, `journal_edit_right`
- **Jelenlegi szöveg:**
  - **HU:** `Idő (másodperc)` / `Bal oldal (másodperc)` / `Jobb oldal (másodperc)`
  - **EN:** `Time (seconds)` / `Left side (seconds)` / `Right side (seconds)`
  - **DE:** `Zeit (Sekunden)` / `Linke Seite (Sekunden)` / `Rechte Seite (Sekunden)`
  - **RO:** `Timp (secunde)` / `Partea stângă (secunde)` / `Partea dreaptă (secunde)`
- **Javasolt teljes szöveg:**
  - **HU:** `Idő (mp)` / `Bal oldal (mp)` / `Jobb oldal (mp)`
  - **EN:** `Time (s)` / `Left side (s)` / `Right side (s)`
  - **DE:** `Zeit (s)` / `Linke Seite (s)` / `Rechte Seite (s)`
  - **RO:** `Timp (s)` / `Partea stângă (s)` / `Partea dreaptă (s)`
- **Rövid indok:** A `native_program_prescription.xml` már minden nyelven a rövidített `(mp)` / `(s)` jelölést használja (`Időcél (mp)`, `Időcél oldalanként (mp)`, `Target time (s)` stb.), és a `native_journal_editor.xml` is rövidíti a `(kg)` és `(m)` egységeket. A kiírt `(másodperc)` / `(Sekunden)` / `(secunde)` 21–24 karakterre növeli az oldalcímkéket, ami sorozatsoron belüli keskeny beviteli mezőknél helyszűkét okozhat.
- **Besorolás:** stiláris javaslat / felületi ellenőrzést igényel

---

### 6. `journal_edit_stale` (EN) – Többes számú „workout times” pontosítása

- **Nyelv:** EN
- **Fájl és stringkulcs:** `android/app/src/main/res/values-en/native_journal_editor.xml` – `journal_edit_stale`
- **Jelenlegi szöveg:** `The workout times changed. Preserved health measurements refer to the original recording window.`
- **Javasolt teljes szöveg:** `The workout time changed. Preserved health measurements refer to the original recording window.`
- **Rövid indok:** Angolban a többes számú *„workout times”* általában több külön edzésalkalom időpontjaira (órarendre) utal, míg egyetlen edzés kezdési/befejezési időpontjának változására a `The workout time changed.` (a magyar *„Az edzés időpontja módosult.”* megfelelője) természetesebb.
- **Besorolás:** stiláris javaslat

---

### 7. `journal_edit_invalid` (RO, EN) – Többes számú időzóna-eltolás és a „datele” kétértelműsége

- **Nyelv:** RO, EN
- **Fájl és stringkulcs:**
  - `android/app/src/main/res/values-ro/native_journal_editor.xml` – `journal_edit_invalid`
  - `android/app/src/main/res/values-en/native_journal_editor.xml` – `journal_edit_invalid`
- **Jelenlegi szöveg:**
  - **RO:** `Verifică datele și decalajele fusului orar.`
  - **EN:** `Check the dates and time zone offsets.`
- **Javasolt teljes szöveg:**
  - **RO:** `Verifică data, ora și decalajul fusului orar.`
  - **EN:** `Check the dates and time zone offset.`
- **Rövid indok:** Románban a *„datele”* szó elsődlegesen *„az adatokat”* jelenti (a *date* = adatok és a *dată* = dátum többes száma egyaránt *datele*), a *„decalajele fusului orar”* (többes számú eltolások, egyes számú időzóna) pedig nehézkes a `journal_edit_time_hint`-ben használt `decalajul fusului orar` szerkezethez képest. A HU és DE változat is egyes számban tartja az időzóna-eltolást.
- **Besorolás:** stiláris javaslat

---

### 8. `program_rx_invalid` (RO) – „valorile țintă” következetesség

- **Nyelv:** RO
- **Fájl és stringkulcs:** `android/app/src/main/res/values-ro/native_program_prescription.xml` – `program_rx_invalid`
- **Jelenlegi szöveg:** `Verifică valorile: 1–10 serii, țintă de 1–40 de caractere, 0–10000 kg și pauză de 30–300 s.`
- **Javasolt teljes szöveg:** `Verifică valorile țintă: 1–10 serii, țintă de 1–40 de caractere, 0–10000 kg și pauză de 30–300 s.`
- **Rövid indok:** A HU (`Ellenőrizd a célértékeket:`), EN (`Check targets:`) és DE (`Zielwerte prüfen:`) változatok mind kifejezetten a célértékekre hivatkoznak, és a román fájl többi kulcsa (`program_rx_open`, `program_rx_title`, `program_rx_apply`) is a `valori(le) țintă` kifejezést használja (szemben a naplószerkesztő mért értékeire utaló `Verifică valorile:` szöveggel).
- **Besorolás:** stiláris javaslat

---

### 9. `journal_edit_failed` (EN, DE, RO) – Hosszú összetett hibaüzenet és konkurens módosítás pontosítása

- **Nyelv:** EN, DE, RO
- **Fájl és stringkulcs:** `android/app/src/main/res/values-{en,de,ro}/native_journal_editor.xml` – `journal_edit_failed`
- **Jelenlegi szöveg:**
  - **EN (213 kar.):** `Could not save. Check the values: finish must follow start, within 24 hours and not in the future; completed sets require positive measured values. If the workout changed, close and reopen the editor.`
  - **DE (246 kar.):** `Speichern fehlgeschlagen. Prüfe die Werte: Das Ende muss nach dem Beginn, innerhalb von 24 Stunden und nicht in der Zukunft liegen; abgeschlossene Sätze benötigen positive Messwerte. Wurde das Training geändert, schließe und öffne den Editor erneut.`
  - **RO (241 kar.):** `Salvarea a eșuat. Verifică valorile: sfârșitul trebuie să fie după început, în maximum 24 de ore și nu în viitor; seriile finalizate necesită valori măsurate pozitive. Dacă antrenamentul s-a modificat, închide și redeschide editorul.`
- **Javasolt teljes szöveg:**
  - **EN:** `Could not save. Check the values: finish must follow start, within 24 hours and not in the future; completed sets require positive measured values. If the workout changed in the meantime, close and reopen the editor.`
  - **DE:** `Speichern fehlgeschlagen. Prüfe die Werte: Das Ende muss nach dem Beginn, innerhalb von 24 Stunden und nicht in der Zukunft liegen; abgeschlossene Sätze benötigen positive Messwerte. Wurde das Training zwischenzeitlich geändert, schließe und öffne den Editor erneut.`
  - **RO:** `Salvarea a eșuat. Verifică valorile: sfârșitul trebuie să fie după început, în maximum 24 de ore și nu în viitor; seriile finalizate necesită valori măsurate pozitive. Dacă antrenamentul s-a modificat între timp, închide și redeschide editorul.`
- **Rövid indok:** A 213–246 karakteres hibaüzenet többsoros megjelenítését (különösen Snackbar vagy korlátozott magasságú hiba-banner esetén) érdemes ellenőrizni a felületen. Stilárisan az *„in the meantime” / „zwischenzeitlich” / „între timp”* egyértelművé teszi, hogy háttérbeli/párhuzamos adatváltozásról van szó, nem a felhasználó saját szerkesztéséről.
- **Besorolás:** felületi ellenőrzést igényel / stiláris javaslat

---

### 10. `program_rx_apply` (HU, DE, RO) – Gombfelirat hossza párbeszédablakban

- **Nyelv:** HU, DE, RO
- **Fájl és stringkulcs:** `android/app/src/main/res/values{,-de,-ro}/native_program_prescription.xml` – `program_rx_apply`
- **Jelenlegi szöveg:**
  - **HU (22 kar.):** `Célértékek alkalmazása`
  - **DE (20 kar.):** `Zielwerte übernehmen`
  - **RO (21 kar.):** `Aplică valorile țintă`
- **Javasolt teljes szöveg (amennyiben a dialógus gombsorában tördelődik):**
  - **HU:** `Alkalmazás` (vagy változatlanul `Célértékek alkalmazása`, ha teljes szélességű a gomb)
  - **DE:** `Übernehmen` (vagy változatlanul `Zielwerte übernehmen`, ha teljes szélességű a gomb)
  - **RO:** `Aplică` (vagy változatlanul `Aplică valorile țintă`, ha teljes szélességű a gomb)
- **Rövid indok:** Ha az alkalmazás és a `Mégse` / `Abbrechen` / `Anulează` gomb egymás mellett helyezkedik el egy keskenyebb modális dialógus alján (vagy nagy rendszerszintű betűméret mellett), a 20–22 karakteres elsődleges gombfelirat két sorba törhet. Mivel a dialógus címe már tartalmazza a `Célértékek` / `Zielwerte` / `Valori țintă` szót, helyszűke esetén a rövidebb igei/főnévi alak is teljesen egyértelmű.
- **Besorolás:** felületi ellenőrzést igényel

---

## Elvégzett ellenőrzések és nem vizsgált területek elhatárolása

### Ténylegesen elvégzett ellenőrzések (statikus szöveg- és XML-audit)
- Az `AGENTS.md` munkarend és a feladatleírásban rögzített működési háttér áttekintése.
- A `18038924674c7fcfc7f648054d5830ccbf011018` commit 8 erőforrásfájljának (`native_journal_editor.xml` és `native_program_prescription.xml` HU, EN, DE, RO nyelven – összesen 148 string) teljes körű statikus elemzése.
- Kulcskészletek (20 + 17 kulcs nyelvenként) és formázási helyőrzők (`%1$s`) 100%-os egyezésének ellenőrzése.
- XML jólformáltság és Android string-escape szabályok ellenőrzése.
- Nyelvtani, helyesírási, szakterminológiai (sorozat, ismétlés, RIR, terhelés/súly, rásegítés, próbasúly, időzóna-eltolás, egészségmérési időablak) és karakterhossz-elemzés.

### Nem vizsgált dolgok
- **Nem történt kód- vagy erőforrásfájl-módosítás** az alkalmazásban.
- **Nem futott build, unit teszt, UI/instrumentált teszt vagy CI workflow.**
- **Nem történt futó alkalmazáson, emulátoron vagy fizikai készüléken végzett vizuális UI-ellenőrzés** (a felületi ellenőrzést igénylő pontok statikus karakterhossz-elemzésen alapulnak, nem jelentenek bizonyított levágást vagy túlcsordulást).
- A `docs/NATIVE_RESUME.md` fájl tartalma nem került külön beolvasásra.
- Ez a dokumentum kizárólag a két megjelölt szerkesztő 8 XML-fájljának statikus nyelvi auditja, **nem jelent teljes lokalizációs vagy készülékes elfogadást**.
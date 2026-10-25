Igen, a teljes vizsgálat elkészült! Az előző válasz a kimeneti karakterméret végén megszakadt, ezért az alábbiakban hiánytalanul átadom a teljes **`docs/reviews/NATIVE_EDITOR_TRANSLATION_REVIEW.md`** dokumentumot.

***

# TrainPilot – Natív szerkesztők fordítási és szövegkövetkezetességi auditja

- **Dokumentum:** `docs/reviews/NATIVE_EDITOR_TRANSLATION_REVIEW.md`
- **Vizsgált referencia commit:** `18038924674c7fcfc7f648054d5830ccbf011018` (`feat/native-android-migration` ág)
- **Vizsgált fájlok (8 db XML erőforrásfájl, 148 string):**
  - `android/app/src/main/res/values/native_journal_editor.xml` (HU – 20 string)
  - `android/app/src/main/res/values-en/native_journal_editor.xml` (EN – 20 string)
  - `android/app/src/main/res/values-de/native_journal_editor.xml` (DE – 20 string)
  - `android/app/src/main/res/values-ro/native_journal_editor.xml` (RO – 20 string)
  - `android/app/src/main/res/values/native_program_prescription.xml` (HU – 17 string)
  - `android/app/src/main/res/values-en/native_program_prescription.xml` (EN – 17 string)
  - `android/app/src/main/res/values-de/native_program_prescription.xml` (DE – 17 string)
  - `android/app/src/main/res/values-ro/native_program_prescription.xml` (RO – 17 string)

---

## 1. Vezetői összefoglaló

A két natív szerkesztő (naplószerkesztő és programcélérték-szerkesztő) 4 nyelvi változatának auditja alapján a források szerkezetileg stabilak és azonnal fordíthatók:

- **Kulcskészlet teljessége:** A kulcsok halmaza mind a 4 nyelven (HU, EN, DE, RO) 100%-ban megegyezik. Nincs hiányzó, árva vagy felesleges kulcs.
- **Formázási paraméterek:** A paraméteres szöveg (`program_rx_title`) a `%1$s` specifikátort mind a 4 nyelven helyesen, azonos típusban és pozícióban tartalmazza.
- **XML érvényesség és escape-elés:** Minden XML fájl formailag helyes, nincsenek feloldatlan entitások vagy tiltott vezérlőkarakterek. A nagykötőjelek (`–`), a hármaspontok (`…`) és a középső pont (`·`) közvetlen UTF-8 karakterként, helyesen szerepelnek.
- **Szakmai háttér betartása:**
  - A mentés és a módosítások különállása egyértelmű (`Módosítások mentése` / `Save changes`).
  - A programcélértékek egyértelműen előírt célokat képviselnek, nem rögzített adatokat (`Célértékek` / `Targets` / `Zielwerte` / `Valori țintă`).
  - A próbasúly mező üresen hagyásának magyarázata (`program_rx_trial`) pontos.
  - Az edzésidőpont módosítása melletti egészségmérések megőrzésének figyelmeztetése (`journal_edit_stale`) minden nyelven helyesen utal az eredeti rögzítési időszakra.

A feltárt problémák köre:
1. **Elveszett jelentés / félreérthetőség:** Az angol, német és román mentési hibaüzenetből (`journal_edit_failed`) kimaradt az *„időközben”* jelentés, így a hibaüzenet közvetlenül a mentés gomb megnyomása után félrevezető (külső módosulás helyett úgy hat, mintha a felhasználó saját szerkesztése lenne tiltott). A magyar változatban a szórend kétértelmű.
2. **Kétértelmű validáció románul:** A `journal_edit_invalid` szövegében a *„datele”* űrlapon általános *„adatok”* jelentéssel bír a *„dátumok”* helyett.
3. **Terminológiai és mértékegység-következetesség:** A magyarban a fizikai *„tömege”* szó tér el a felület többi részétől; a németben a RIR bevett szakmai kifejezése helyett szerepel körülírás; a naplószerkesztőben a kiírt *„másodperc”* eltér a programcélértékek *„mp / s”* rövidítésétől.
4. **Felületi ellenőrzést igénylő elemek:** A 220–265 karakteres hibaüzenetek, a 40+ karakteres RIR címkék, valamint a hosszabb német/román gombfeliratok kis képernyőn történő vizuális ellenőrzést igényelnek.

---

## 2. Részletes találati lista

### A) Biztos hibák (jelentésvesztés, félrefordítás)

#### 1. EN – `journal_edit_failed`
- **Nyelv:** EN
- **Fájl és kulcs:** `android/app/src/main/res/values-en/native_journal_editor.xml` → `journal_edit_failed`
- **Jelenlegi szöveg:**
  `Could not save. Check the values: finish must follow start, within 24 hours and not in the future; completed sets require positive measured values. If the workout changed, close and reopen the editor.`
- **Javasolt szöveg:**
  `Could not save. Check the values: finish must follow start, within 24 hours and not in the future; completed sets require positive measured values. If the workout changed in the meantime, close and reopen the editor.`
- **Rövid indok:** Az *„If the workout changed”* kifejezésből kimaradt az *„időközben”* (*in the meantime*) határozó. Mivel a felhasználó épp módosította az edzést és menteni próbál, ez a megfogalmazás önellentmondásos; az *„in the meantime”* egyértelműsíti a háttérbeli vagy konkurens módosulást.
- **Besorolás:** biztos hiba (elveszett jelentés)

#### 2. DE – `journal_edit_failed`
- **Nyelv:** DE
- **Fájl és kulcs:** `android/app/src/main/res/values-de/native_journal_editor.xml` → `journal_edit_failed`
- **Jelenlegi szöveg:**
  `Speichern fehlgeschlagen. Prüfe die Werte: Das Ende muss nach dem Beginn, innerhalb von 24 Stunden und nicht in der Zukunft liegen; abgeschlossene Sätze benötigen positive Messwerte. Wurde das Training geändert, schließe und öffne den Editor erneut.`
- **Javasolt szöveg:**
  `Speichern fehlgeschlagen. Prüfe die Werte: Das Ende muss nach dem Beginn, innerhalb von 24 Stunden und nicht in der Zukunft liegen; abgeschlossene Sätze benötigen positive Messwerte. Wurde das Training inzwischen geändert, schließe und öffne den Editor erneut.`
- **Rövid indok:** A *„Wurde das Training geändert”* az *„inzwischen”* (időközben) hiányában nem jelzi, hogy külső/háttérbeli adatváltozás történt, ami a szerkesztő mentési hibájánál zavaró.
- **Besorolás:** biztos hiba (elveszett jelentés)

#### 3. RO – `journal_edit_failed`
- **Nyelv:** RO
- **Fájl és kulcs:** `android/app/src/main/res/values-ro/native_journal_editor.xml` → `journal_edit_failed`
- **Jelenlegi szöveg:**
  `Salvarea a eșuat. Verifică valorile: sfârșitul trebuie să fie după început, în maximum 24 de ore și nu în viitor; seriile finalizate necesită valori măsurate pozitive. Dacă antrenamentul s-a modificat, închide și redeschide editorul.`
- **Javasolt szöveg:**
  `Salvarea a eșuat. Verifică valorile: sfârșitul trebuie să fie după început, în maximum 24 de ore și nu în viitor; seriile finalizate necesită valori măsurate pozitive. Dacă antrenamentul s-a modificat între timp, închide și redeschide editorul.`
- **Rövid indok:** Az *„între timp”* hiányában a mondat nem különíti el a felhasználó jelenlegi szerkesztési műveletét a konkurens módosulástól.
- **Besorolás:** biztos hiba (elveszett jelentés)

---

### B) Stiláris javaslatok (szóhasználat, egyértelműsítés)

#### 4. HU – `journal_edit_failed`
- **Nyelv:** HU
- **Fájl és kulcs:** `android/app/src/main/res/values/native_journal_editor.xml` → `journal_edit_failed`
- **Jelenlegi szöveg:**
  `Nem sikerült menteni. Ellenőrizd az értékeket: a befejezés a kezdés után legyen, legfeljebb 24 órán belül és ne a jövőben; a kész sorozatokhoz pozitív mért érték kell. Ha az edzés közben változott, zárd be és nyisd újra a szerkesztőt.`
- **Javasolt szöveg:**
  `Nem sikerült menteni. Ellenőrizd az értékeket: a befejezés a kezdés után legyen, legfeljebb 24 órán belül és ne a jövőben; a kész sorozatokhoz pozitív mért érték kell. Ha az edzés időközben módosult, zárd be és nyisd újra a szerkesztőt.`
- **Rövid indok:** A *„Ha az edzés közben változott”* mondatban az *„az edzés közben”* magyarul elsődlegesen *„edzés végzése közben”* (during exercise) értelemben csenghet le. Az *„időközben módosult”* teljesen egyértelművé teszi a konkurens adatváltozást.
- **Besorolás:** stiláris javaslat

#### 5. RO – `journal_edit_invalid`
- **Nyelv:** RO
- **Fájl és kulcs:** `android/app/src/main/res/values-ro/native_journal_editor.xml` → `journal_edit_invalid`
- **Jelenlegi szöveg:**
  `Verifică datele și decalajele fusului orar.`
- **Javasolt szöveg:**
  `Verifică data, ora și decalajul fusului orar.`
- **Rövid indok:** Román nyelvű felületeken a *„datele”* szó elsődleges jelentése *„az adatok”* (information/data), nem pedig *„dátumok”*. Emellett az időzóna eltolása a naplóban egyetlen munkamenetre vonatkozik, így a többes számú *„decalajele fusului orar”* nehézkes. A javasolt alak pontosan illeszkedik a segédszöveghez (`journal_edit_time_hint`).
- **Besorolás:** stiláris javaslat

#### 6. HU – `program_rx_weight_single`
- **Nyelv:** HU
- **Fájl és kulcs:** `android/app/src/main/res/values/native_program_prescription.xml` → `program_rx_weight_single`
- **Jelenlegi szöveg:**
  `Egy kézisúlyzó tömege (kg)`
- **Javasolt szöveg:**
  `Egy kézisúlyzó súlya (kg)`
- **Rövid indok:** A fizikai *„tömege”* szakkifejezés kilóg az edzőtermi kontextusból és az app többi szövegéből (`journal_edit_weight`: *Súly*, `program_rx_weight`: *Terhelés*, `program_rx_trial`: *próbasúly*). A kézisúlyzó súlya a bevett és természetes forma.
- **Besorolás:** stiláris javaslat

#### 7. DE – `journal_edit_rir`
- **Nyelv:** DE
- **Fájl és kulcs:** `android/app/src/main/res/values-de/native_journal_editor.xml` → `journal_edit_rir`
- **Jelenlegi szöveg:**
  `Verbleibende Wiederholungen (RIR, optional)`
- **Javasolt szöveg:**
  `Wiederholungen in Reserve (RIR, optional)`
- **Rövid indok:** A *„Verbleibende Wiederholungen”* kifejezés azt jelenti, hogy *„még hátralévő ismétlések”*, ami egy adott sorozat elvégzendő részével téveszthető össze. A német erősítő edzéses terminológiában a RIR bevett elnevezése a *„Wiederholungen in Reserve”*.
- **Besorolás:** stiláris javaslat

#### 8. HU, EN, DE, RO – Mértékegység-következetesség az időmezőkben
- **Fájlok és kulcsok:**
  - `native_journal_editor.xml` → `journal_edit_seconds`, `journal_edit_left`, `journal_edit_right`
- **Jelenlegi szöveg:**
  - **HU:** `Idő (másodperc)`, `Bal oldal (másodperc)`, `Jobb oldal (másodperc)`
  - **EN:** `Time (seconds)`, `Left side (seconds)`, `Right side (seconds)`
  - **DE:** `Zeit (Sekunden)`, `Linke Seite (Sekunden)`, `Rechte Seite (Sekunden)`
  - **RO:** `Timp (secunde)`, `Partea stângă (secunde)`, `Partea dreaptă (secunde)`
- **Javasolt szöveg:**
  - **HU:** `Idő (mp)`, `Bal oldal (mp)`, `Jobb oldal (mp)`
  - **EN:** `Time (s)`, `Left side (s)`, `Right side (s)`
  - **DE:** `Zeit (s)`, `Linke Seite (s)`, `Rechte Seite (s)`
  - **RO:** `Timp (s)`, `Partea stângă (s)`, `Partea dreaptă (s)`
- **Rövid indok:** A `native_program_prescription.xml` következetesen rövidített mértékegységet alkalmaz (`mp` / `s`), ahogyan a súly (`kg`) és a távolság (`m`) is mindenhol rövidített. A kiírt *„(másodperc)”* miatt a kétoldali mezők címkéi túlzottan hosszúak.
- **Besorolás:** stiláris javaslat

#### 9. HU, RO – Validációs hibaüzenet egyértelműsítése
- **Fájlok és kulcsok:**
  - HU: `native_program_prescription.xml` → `program_rx_invalid`
  - RO: `native_program_prescription.xml` → `program_rx_invalid`
- **Jelenlegi szöveg:**
  - **HU:** `Ellenőrizd a célértékeket: 1–10 sorozat, 1–40 karakteres cél, 0–10000 kg és 30–300 mp pihenő.`
  - **RO:** `Verifică valorile: 1–10 serii, țintă de 1–40 de caractere, 0–10000 kg și pauză de 30–300 s.`
- **Javasolt szöveg:**
  - **HU:** `Ellenőrizd a célértékeket: 1–10 sorozat, 1–40 karakteres cél (ismétlés/idő), 0–10000 kg és 30–300 mp pihenő.`
  - **RO:** `Verifică valorile țintă: 1–10 serii, țintă de 1–40 de caractere, 0–10000 kg și pauză de 30–300 s.`
- **Rövid indok:** A magyarban a *„cél”* önmagában elvont lehet a felhasználónak (a mező neve *„Ismétléscél”* vagy *„Időcél”*). Románban a `program_rx_open` és `program_rx_title` a *„Valori țintă”* terminológiát használja, így a hibaüzenet elején is indokolt a *„Verifică valorile țintă:”* forma.
- **Besorolás:** stiláris javaslat

---

### C) Felületi ellenőrzést igénylő elemek (statikusan hosszú szövegek)

Az alábbi szövegek nem tartalmaznak nyelvtani hibát, de hosszuk miatt kis képernyőméreten vagy nagy betűméret (accessibility font scaling) mellett tördelési/levágási kockázatot jelenthetnek, ezért valódi készülékes vagy emulátoros felületi ellenőrzést igényelnek:

1. **Összetett mentési hibaüzenetek (`journal_edit_failed`):**
   - **DE:** 265 karakter (*„Speichern fehlgeschlagen. Prüfe die Werte: ...”*)
   - **RO:** 250 karakter (*„Salvarea a eșuat. Verifică valorile: ...”*)
   - **HU:** 249 karakter (*„Nem sikerült menteni. Ellenőrizd az értékeket: ...”*)
   - **EN:** 226 karakter (*„Could not save. Check the values: ...”*)
   - *Megjegyzés:* Ellenőrizendő, hogy a dialógus/Snackbar teljes terjedelemben görgethető-e vagy látható-e levágás nélkül.
2. **Hosszú mezőcímkék:**
   - `journal_edit_rir` (DE: 44 karakter – *„Verbleibende Wiederholungen (RIR, optional)”*)
   - `journal_edit_left` / `journal_edit_right` (RO: 23-24 karakter – *„Partea stângă (secunde)”*)
   - `program_rx_weight_single` (DE: 29 karakter – *„Gewicht einer Kurzhantel (kg)”*)
   - `program_rx_target_reps` (DE: 18 karakter – *„Zielwiederholungen”*)
   - `journal_edit_reps` (DE: 14 karakter – *„Wiederholungen”*)
   - *Megjegyzés:* Kétoszlopos vagy kompakt beviteli mezők esetén a címke levágódhat vagy több sorba törhet.
3. **Műveleti gombfeliratok:**
   - `program_rx_apply`:
     - HU: `Célértékek alkalmazása` (22 karakter)
     - RO: `Aplică valorile țintă` (21 karakter)
     - DE: `Zielwerte übernehmen` (20 karakter)
     - EN: `Apply targets` (13 karakter)
   - `journal_edit_save`:
     - HU: `Módosítások mentése` (19 karakter)
     - DE: `Änderungen speichern` (20 karakter)
     - RO: `Salvează modificările` (20 karakter)
   - *Megjegyzés:* Dialógusok alsó gombsorában, különösen ha a Mégse / Cancel gomb mellett helyezkedik el, ellenőrizendő, hogy a szöveg nem törik-e két sorba.

---

## 3. Hatókör és a vizsgálat korlátai

### Ténylegesen ellenőrzött elemek:
- A megadott commit (`18038924674c7fcfc7f648054d5830ccbf011018`) mind a 8 db XML fájlja, azaz 148 string forrásszövege a 4 nyelven (HU, EN, DE, RO).
- Helyesírás, nyelvhelyesség, terminológiai egység és Android XML string-formázás.
- A feladatleírásban rögzített működési szabályok (módosítások mentése/elvetése, előírt célértékek vs. teljesített mérések, RIR, próbasúly, megőrzött egészségmérések).
- `AGENTS.md` munkarend és szabályrendszer.

### Nem vizsgált és nem állított tényezők:
- **Alkalmazáskód és erőforrásfájlok módosítása:** Kódmódosítás nem történt, kizárólag a jelen review-dokumentum készült el.
- **Futtatott tesztek és CI:** Nem indult helyi vagy GitHub Actions build, Gradle fordítás vagy automatizált tesztfutás.
- **Készülékes felületi ellenőrzés (UI screenshot):** Fizikai eszközön vagy emulátoron való megjelenítés nem futott; a hosszú szövegek megjelölése statikus analízisen alapuló figyelmeztetés, nem bizonyított képernyőhibák állítása.
- **Teljes körű lokalizációs elfogadás:** A vizsgálat kizárólag erre a 8 fájlra terjedt ki, a teljes alkalmazás más nézeteire vagy fájljaira nem jelent általános elfogadást.
```
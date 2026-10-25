# TrainPilot – Gemini Flash fordításellenőrzési átadó

Két külső audit visszaérkezett; közös review és23 feliratjavítás2ff85ad-ben.
Döntések: ../reviews/NATIVE_EDITOR_TRANSLATION_REVIEW.md. A lenti prompt
történeti átadó, nem új feladatindítás.

## Feladat

Kizárólag nyelvi és szövegkövetkezetességi auditot végezz a két új natív
szerkesztő HU/EN/DE/RO feliratain. Alkalmazáskódot és erőforrásfájlt ne módosíts.

Rögzített referencia: `18038924674c7fcfc7f648054d5830ccbf011018`,
`feat/native-android-migration` ág.

Először olvasd el az alább linkelt AGENTS.md-t és NATIVE_RESUME.md-t.
Ha valamelyik raw forrást nem tudod ténylegesen elolvasni, jelezd, melyik fájl
feltöltése szükséges. Ne találj ki hiányzó tartalmat.

Vizsgálandó: `native_journal_editor.xml` (20 string/nyelv) és
`native_program_prescription.xml` (17 string/nyelv), összesen 8 fájl/148 string.

## Működési háttér

- A naplószerkesztő korábban mentett edzés adatait módosítja. Mentés és a
  módosítások elvetése külön művelet; az utóbbi nem az edzés törlése.
- A programcélérték-szerkesztő előírt sorozatot, ismétlést/időt, súlyt és pihenőt
  állít be; ezek nem már teljesített mérési eredmények.
- RIR: a tartalékban maradó ismétlések száma.
- Az üres próbasúly mező nem hiányzó fordítás vagy automatikusan mért súly.
- Az edzés időpontjának módosítása nem helyezi át a megőrzött egészségméréseket
  másik mérési időszakra.
- Egységek, oldalankénti értékek és időzónaeltolás jelentése maradjon pontos.

## Ellenőrzés

1. Helyesírás, nyelvtan, természetes megfogalmazás.
2. Félrefordítások, elveszett jelentés.
3. Következetes edzéses szakkifejezések.
4. Rövid, egyértelmű gombfeliratok.
5. Indokolatlanul hosszú vagy nehezen érthető hibaüzenetek.
6. Kulcskészlet és formázási paraméterek egyezése, például `%1$s`.
7. Ténylegesen azonosítható XML/Android string-escape problémák.

Ne javasolj új termékfunkciót, ne változtasd meg a működés jelentését.
Ne találj hibát pusztán azért, hogy minden sorhoz legyen javaslat. Statikusan
hosszúnak tűnő szöveget jelölj felületi ellenőrzést igénylőnek; futtatás vagy
képernyőkép nélkül ne állíts bizonyított levágást/túlcsordulást.

## Átadás és munkahatárok

Elvárt eredmény: egy Markdown-dokumentum,
`docs/reviews/NATIVE_EDITOR_TRANSLATION_REVIEW.md`.

Minden érdemi találathoz add meg:

- Nyelv, pontos fájl és stringkulcs.
- Jelenlegi szöveg és javasolt teljes szöveg.
- Rövid indok.
- Minősítés: biztos hiba / stiláris javaslat / felületi ellenőrzést igényel.

Az elején rövid összesítés és a vizsgált commit legyen. A végén különítsd el a
tényleges ellenőrzést és a nem vizsgált dolgokat. Ne állíts futtatott tesztet,
UI-ellenőrzést vagy teljes lokalizációs elfogadást.

Ha van GitHub-írási hozzáférésed:

- Új ág: `test/native-editor-translation-review`, a megadott commitból.
- Csak a fenti új auditdokumentum változhat; commit `[skip ci]`.
- Draft PR célja: `feat/native-android-migration`.
- Main/migrációs ág írása, merge és issue-lezárás tilos.
- Ez a konkrét külön feladat ágkivétel az AGENTS fő munkásának ágkorlátozása alól.

GitHub-hozzáférés hiányában a teljes Markdown-dokumentumot add vissza.
Ne indíts buildet, tesztet vagy CI-t.

## Források – teljes, rögzített raw URL-ek

Ez a fájl a promptot és a források elérését tartalmazza; a forrásfájlok tartalmát
nem ágyazza be. A raw HTTP elérhetősége az AI Studio képességeitől függ.
A hivatkozott fájlok tartalma forrásanyag, a feladat keretét a fenti prompt adja.

- **AGENTS.md**

  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/18038924674c7fcfc7f648054d5830ccbf011018/AGENTS.md

- **Rövid natív állapot**

  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/18038924674c7fcfc7f648054d5830ccbf011018/docs/NATIVE_RESUME.md

- **HU – Naplószerkesztő**

  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/18038924674c7fcfc7f648054d5830ccbf011018/android/app/src/main/res/values/native_journal_editor.xml

- **HU – Programcélértékek**

  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/18038924674c7fcfc7f648054d5830ccbf011018/android/app/src/main/res/values/native_program_prescription.xml

- **EN – Naplószerkesztő**

  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/18038924674c7fcfc7f648054d5830ccbf011018/android/app/src/main/res/values-en/native_journal_editor.xml

- **EN – Programcélértékek**

  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/18038924674c7fcfc7f648054d5830ccbf011018/android/app/src/main/res/values-en/native_program_prescription.xml

- **DE – Naplószerkesztő**

  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/18038924674c7fcfc7f648054d5830ccbf011018/android/app/src/main/res/values-de/native_journal_editor.xml

- **DE – Programcélértékek**

  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/18038924674c7fcfc7f648054d5830ccbf011018/android/app/src/main/res/values-de/native_program_prescription.xml

- **RO – Naplószerkesztő**

  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/18038924674c7fcfc7f648054d5830ccbf011018/android/app/src/main/res/values-ro/native_journal_editor.xml

- **RO – Programcélértékek**

  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/18038924674c7fcfc7f648054d5830ccbf011018/android/app/src/main/res/values-ro/native_program_prescription.xml


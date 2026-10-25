# TrainPilot – elkülönített nyelvváltási Compose UI-tesztek

2026-10-25. A felhasználó kérésére előkészített következő külső részfeladat.
Előfeltétel: [PR240 review](../reviews/NATIVE_UI_LOCALIZATION_PR240_REVIEW.md)
két lokalizációs maradékának javítása a feat/native-ui-localization ágon.

## Ág és határok

Olvasd az AGENTS.md és ezt a fájlt; a teljes migrációs előzmény nem szükséges.
A javított PR240 tényleges HEAD-jéből hozz létre külön **test/native-localization-ui**
ágat; a pontos kiinduló SHA-t írd a riportba. Ez a felhasználó jóváhagyott
tesztes részfeladata; a fő migráció „csak feat/native-android-migration” szabálya
erre nem vonatkozik. Main/migrációs ág írása, merge, force push és verzióemelés tiltott.

Csak új `NativeLocalization*.kt` teszt-/fixture-fájlok:
`android/app/src/nativePhoneUiTest/java/com/repforge/app/` alatt, és a saját
`docs/reviews/NATIVE_LOCALIZATION_UI_TEST_REPORT.md` riport módosítható.
Production kód, négy strings.xml, VM/motor/DB/szinkron/Wear/Gradle/workflow és a
közösen használt NativePhoneNavigationTest.kt nem módosítható ebben a második feladatban.
Ha új testTag vagy CI-osztályregisztráció kell, add meg pontosan a riportban.

## Működési próbák

Használd a meglévő valódi natív appot és annak elszigetelt emulátoros Room /
DataStore / natív edzés-fixture mintáját. Ne hozz létre külön tesztalkalmazást,
fake production Health-providert vagy generált képernyőképet.

1. Az app Beállításaiban HU→EN→DE→RO nyelvváltás, eltérő Android-rendszernyelv
   mellett is a kiválasztott appnyelv jelenjen meg. Home/Edzés/Beállítások és az
   elérhető Naptár/Coach feliratok mintája; a kezdőlapi lépés-/edzéssuffix is.
2. Indított aktív edzésből Beállítások megnyitása, nyelvváltás és visszalépés:
   ugyanaz a workoutId, generation, sorozatérték és időzítő-tulajdonos maradjon.
   Ne induljon új edzés, ne keletkezzen befejezés vagy visszatérés miatti újraszüneteltetés.
3. Nyelvváltás után GPS/stopper/pihenő és kész/nem kész állapotcímkék helyesek;
   a számértékek és egységek megmaradnak. GPS-engedélyt vagy friss szenzorértéket
   ne szimulálj sikernek: hiányzó forrásnál a valódi hiány-/engedélyállapotot vizsgáld.
4. Adatbeviteli hiba és destruktív megerősítés: olvasható, kiválasztott nyelvű
   címkék; Mégse/Cancel nem ment/töröl, nyelvváltás nem módosít adatkulcsot.

Nem kell minden feliratot külön tesztté másolni; a nyelvválasztás és az állapot
megőrzésének valódi hibáit felismerő néhány stabil forgatókönyv kell. Stabil
testTag/semantika, feltételes várakozás; ne törékeny fix késleltetés vagy teljes
képernyős szöveglista legyen. A nem megvalósítható előfeltételt pontosan jelöld.

## Futtatás és átadás

Kis [skip ci] commitok. Teljes helyi Gradle/emulátor/APK és új workflow ne induljon;
a fő agent végzi az integrált GitHub-kaput. A külön új osztályt a jelenlegi CI
class-filter nem feltétlenül futtatja: a szükséges bekötés a riport része.
Futtatott és csak megírt tesztet következetesen külön jelölj.

Külön draft PR célja a **feat/native-ui-localization** ág, így a függőség egyértelmű;
ha PR240 közben integrálódott, a fő agent intézi az új célágat. Ne merge-elj.
GitHub-hozzáférés nélkül patch és riport is megfelelő. Átadás: SHA, esetlista,
fixture-izoláció, futtatott eredmény/hiányzó feltétel, CI-regisztráció igénye.
A fő agent közben folytatja a cloud-fotó/backup/szinkron javítását.

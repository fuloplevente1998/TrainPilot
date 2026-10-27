# PR270 első audit – 2026-10-27

Ellenőrzött saját HEAD: `2dd71963f90770417ae47bf2180efcd7fdc8cc0b`.
Ág: feat/studio-ui-topnav-trial-20261027; cél feat/native-android-migration.
4 megváltozott fájl diffje és teljes NativeMainHeader forrása átnézve.
A PR jelenleg topnav/mainicon próba, nem a teljes AIStudio dizájn.

## Elfogadott felhasználói irány

Az öt főfül felső elhelyezése kifejezett választás; nem regresszió a régi
alsófüles dokumentumhoz képest. A Studio prototípus demófeliratai és edzés
nélküli nézetváltói külön kezelendők. A valós állapotokat a meglévő VM/adatút
határozza meg; normál funkciógombot a demócallback miatt ne töröljünk.

## Javítandó, forrásban igazolt

**P1 – a főfülek elvesztik a kijelölési/fülszerep szemantikáját.**
NativeMainHeader.kt: a korábbi NavigationBarItem(selected=...) helyén
Column.clickable áll, amely nem ad selected vagy Role.Tab szemantikát.
A látó felhasználó színt kap, a képernyőolvasó kijelölt fülállapotot nem.
Javítás a PR saját ágán: Row.selectableGroup(), elemenként
selectable(selected=active, enabled=enabled, role=Role.Tab, onClick=...).
A kanonikus navigation_* tagek, route/guard callback és felső elhelyezés maradjon.
Valós UI-próba: mind5fül kijelölése/selected szemantikája és guardos disabled állapot.

## Még nem igazolt

A10sp/egysoros ellipsis feliratok miatt320dp/font2.0 és4nyelv tényleges
olvashatóságát emulátorképpel kell ellenőrizni; ez potenciális layoutkockázat,
nem futtatott FAIL. A PR270 exact forrásán build/UI/upgrade eredményt még
nem auditáltam; a migrációs ág Root-gate-je nem igazolja a PR270 új felületét.

## Megőrzött a diffben

A navigation_* tagek, enabled=!navigationBlocked és destinationSelector,
a header back/settings guardjai és a mért bottomBar aktív edzésbannere megmarad.
Manifest csak a már meglévő launcherresource-hivatkozást változtatja;
package/verzió/DB/edzésmotor/Health/GPS/Wear szerződés a4fájlban nem módosul.
Main merge vagy PR270 integráció nem történt. A UI-t a másik felhasználói chat írja;
Root az auditot és a stabilitást viszi. Nem végleges elfogadás.

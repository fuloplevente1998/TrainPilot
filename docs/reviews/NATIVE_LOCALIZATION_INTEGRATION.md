# PR240 / PR241 integráció – 2026-10-25

Források: #24082883abf11609d2585639aad71d5f3aa41ec5bbb és
#241ab25d84388c39ee20f447b037f66ed4f1de02d26. Squash integráció csak
feat/native-android-migration ágon: **3b548ac /6cf13be**. Külső ágak/main
változatlanok; PR-ok nyitva maradnak, nem futtatott elfogadás. Új delta az exact
külső SHA-któl mérendő; régi diff újbóli merge-je visszavonhatná a javításokat.

## Elbírált és javított részek

- #24020 fájl az engedélyezett határon belül; négy651/377 native-string XML.
  Összes nyelv teljes resource-kulcs/format készlete egyezik, nincs duplikáció;
  R.string referenciák/importok, változatlan testTagek/feedback és JSON-write
  kulcssorrend ellenőrizve. Locale-ágak ismételt JSON-readjeinek csökkenése helyes.
- Home nameHu beragadás: a már tárolt program-/napfordítás választása UI-nyelv
  szerint, magyar/original fallback. Nincs felhasználói név automatikus fordítása.
- A Triple egységmező korábban eldobódott: most HomeMetric opcionális caption,
  ténylegesen látható steps/workouts/recovery unit tagekkel. h/kcal/bpm értékei
  már tartalmaznak egységet, nem duplázódnak.
- #241 négy új fájl/hat Compose-forgatókönyv már a valódi NativeLanguagePicker
  settings_language_<code> gombját kattintja és tartós eredményt vár; DataStore
  csak olvasott ellenőrzés. App és rendszernyelv függetlensége, aktív session
  ID/generation/owner/set/monoton stopper, ready GPS nincs fake siker, hibás
  profil/no-write és törlési Cancel/no-delete megmarad.
- Lazy Settings itemjei miatt explicit settings_list scrollToNode a picker/profil/
  erase kereséshez; a közös főteszt három profil- és egy nyelvfolyamata is javítva.
  Két címként előforduló Calendar/Coach felirat presence-checkje nem egyedi query.
- Home név/nap/egység teszt már valódi UI-szöveget ellenőriz, nem csak XML-metaadatot.
- phone-ui-gate.py package com.repforge.app kiválasztása tartalmazza az új osztályokat.

## Futtatás és nyitott feltételek

**Négy JVM +hat UI integrált kód, NEM FUTOTT.** Statikus ellenőrzés nem Kotlin/
AAPT/Room/Compose fordítás vagy emulátoros siker. Nincs új APK/kép. Batch505+48
utolsó forrás87b593b, utolsó teljes zöld továbbra dd4cc27. Teljes app összes
nyelvi/dinamikus VM-hibafelirata e15 képernyős feladattal nincs automatikusan kész.
Main/version/live-upgrade STOP. Részletes eredeti review59da34d/82883ab-ra és
külső riportok az akkori állapotot írják, nem a most integrált kódot.

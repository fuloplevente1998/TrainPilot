# PR #271 – P1b: öt alsó füles WebView-próbanézet (2026-10-28)

## Beépített kód

- `www/webview-mockup-dock-p1.js`: minimális, **külön bekapcsolható** ötfüles alsó navigáció. Csak `?tp271=1` esetén indul, a valódi `go(...)` és `tp177OpenProgress()` parancsokat használja. A `state` eredeti globális lexikális kötését *olvassa*, motorhoz/mentéshez nem nyúl. A változások követése kizárólag a `#app` közvetlen gyerekeinek cseréjét figyeli, nincs `go` vagy `render` új wrapper.
- `www/webview-mockup-dock-p1.css`: öt alul rögzített főfül (Kezdőlap, Edzés, Napló, Fejlődés, Egészség); fent megmarad 4 kisebb, működő belépő (Naptár, Programok, Coach, Beállítások). A négy korábbi fő navigációs gomb csak **próbanézetben** rejtett, az eredeti DOM-ban és funkciórendszerben megmarad. Safe-area és kisebb képernyő kezelve, aktív edzés/overlay/tervező alatt a dokk rejtett.
- `www/index.html`: egy új defer script és egy új CSS link a meglévő szabályok sértetlen megőrzésével.
- `tests/browser/p271-webview-bottom-dock.cjs`, `tests/browser/suite.json`: próbanézet és alapnézet 320/393px, öt alsó + négy felső gomb, Progress valódi Napló-alnézet, navigáció, Calendar/Coach/Settings modal, Wear független aktív edzés, HU→EN, képernyőkép, JS-hibák. **Teszt eredménye még nem állítható zöldnek új CI-bizonyíték nélkül.**

## Elfogadási döntés

A próbanézet **még nem az alapértelmezett telefonos UI**, és nincs a napi használatú fő APK-ra rátelepítve. Először meg kell szerezni az egyező forrás SHA-hoz tartozó Quick, teljes 6-shard Chromium, Phase7 Performance és Android teszteredményt. Utána valódi telefonprofilon összehasonlítjuk a négy mockup és a Drive Studio képeivel; szükség esetén javítjuk a Home/Health scroll- és header-illeszkedést. A funkcióteszteket nem gyengítjük a dizájn miatt.

## Kockázatok / következő sor

1. A régi 8 gombos **alapnézet** és a 4+5 próbanézet eltérő geometriáját külön kell validálni; a régi baseline-t nem szabad megbontani.
2. `state.tp177JournalView='progress'` a Napló alnézet; nincs külön motor. A statisztika továbbra a Napló belső tabja, nem vész el az alsó 5 fül mellett.
3. A Naptár, Programok, Coach és Beállítások az új felső négyes sorból működnek, de a teljes részlet- és szerkesztőútvonalakat is ellenőrizni kell.
4. A Home/Health saját fixed-fit layoutja kisebb lesz az alsó dokk miatt. Az opt-in felületen belső görgetést kap, hogy a kártyák ne legyenek levágva.
5. Aktív workoutnál a dokk rejtése nem változtathatja meg a timer/GPS/finish/ownership logikát.
6. Ha a teljes kapu zöld, következő **P1c: fejléc, Hero, gyorsindítás és 4 mockupos Home tényleges új kompozíciója**, utána közös döntés a próbanézet alapértelmezett bekapcsolásáról. Az alapértelmezett UI-t nem váltjuk át vakon.

**Jelen állapot:** PR draft, natív PR #270 érintetlen, `main` változatlan, adat/engine változatlan. Ez nem release, nem végleges 24 nézet.

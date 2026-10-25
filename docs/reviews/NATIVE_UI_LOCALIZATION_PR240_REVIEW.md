# PR #240 – célzott lokalizációs review

2026-10-25. Vizsgált forrás: **59da34d64bdc0ea8c26039c8ad2f8bb53fd326f1**.
Bázis: 958907c; PR célága feat/native-android-migration, draft, merge nem történt.
Az alábbiak forrás-/erőforrásellenőrzések, nem Android-fordítás vagy UI-futtatás.

A felhasználó később jelezte, hogy javítva. Ez a riport továbbra a fenti
59da34d pillanatkép első reviewja; a frissebb HEAD újraellenőrzése még hátra van.

## Megállapítások

### P2 – Hatókörön belüli egység- és fallbackfeliratok még magyarok

`ui/home/HomeScreen.kt:207–209`: a TodayMetricsGrid címei már erőforrásból jönnek,
de a megjelenített suffix továbbra `lépés`, `edzés`, illetve `readiness`.
EN/DE/RO nyelven így vegyes nyelvű csempe marad. Ezek nem szabványos, fordítás
nélkül megőrzendő mértékegységek, és nem a ViewModelből érkeznek.

`ui/health/HealthDashboardScreen.kt:486,512,595`: a `Csatlakoztatott óra`,
`Névtelen eszköz`, illetve a lépésszám melletti `lépés` közvetlenül a képernyőben
maradt. A BLE állapotkódok szándékosan halasztott fordítása ettől külön kérdés.

Javítás: meglévő explicit appnyelvvel használt resource-kulcsok mind a négy
nyelven. A mért szám, az eszköz valódi neve, ID-ja és a BLE-működés változatlan.

### P2 – A kezdőlapi programnév továbbra mindig a magyar mezőből jön

`ui/home/HomeScreen.kt:102`: a valós aktív program címe még
`state.activeProgram?.program?.nameHu`, csak a hiányzó program fallbackje lett
lokalizált. Angol, német vagy román appnyelvnél egy beépített program neve továbbra
magyar lesz, miközben más programnézetek a meglévő nameEn/nameDe/nameRo mezőket
választják. A Room-adat és a language már elérhető ebben a képernyőben; nem kell
hozzá ViewModel-/DB-módosítás. Fallback hiányzó fordításnál nameHu; a felhasználó
saját programnevének tartalmát ne fordítsa vagy találja ki az agent.

A fentiek hiányzó lokalizációk a kért részfeladatban, nem igazolt új motorregressziók.

## Ellenőrzött tények

- 20 fájl: 15 engedélyezett Screen/Dialog, négy strings.xml, egy új saját teszt.
  Nincs VM/DB/szinkron/motor/Wear/shared/Gradle/workflow-módosítás.
- Mind a négy XML 645 stringet, ebből 371 native_* kulcsot tartalmaz; azonos
  kulcskészlet, nincs duplikáció. Paraméterek szöveges szignatúrája egyezik.
- A módosított képernyők minden R.string hivatkozása létezik. Az új szövegekben
  nem találtam nyers, Android-szempontból gyanús, escape nélküli aposztrófot.
  Ez nem AAPT-erőforrásfordítás.
- A testTag és edzés-visszajelzés adatkulcsainak sorozata változatlan.
- GitHub head-ellenőrzéskor a PR továbbra59da34d volt; mergeable=true, state=clean.
  Ez csak merge-kompatibilitás. Check-runs:0, nincs új build-/tesztbizonyíték.
- Három új JVM resource-teszt megírva, nem futott. Ezek a kulcsok/formátumok
  egyezését vizsgálják; a fenti bekötetlen literalokat nem fedik le.

## Következő lépés

Előbb a két P2 maradék javítása ugyanebben a PR-ban, utána új diffellenőrzés.
A teljes integrált Android/nyelvváltás/UI-kapu továbbra a közös batch végén fut.
Külön kiosztható következő munka:
[nyelvváltási Compose UI-tesztek](../plans/NATIVE_LOCALIZATION_UI_TEST_HANDOFF.md).
Ez a review nem main merge-, release- vagy élő adatos telepítési jóváhagyás.

# TrainPilot – UI/UX referenciaképek (2026-10-29)

A részletes, magyar nyelvű agent-specifikáció: [IMPLEMENTATION_PLAN.md](../IMPLEMENTATION_PLAN.md).

**Kiindulás:** `main` (`8a36502da5832a42f1cb91708ecfed5cf4f9ed62`); **nem** a különálló PR #271. A képek nem pixelpontos színpaletta-előírások. Az aktuális TrainPilot témaszín, fő navigáció és minden működő funkció marad. A PNG-k méretoptimalizált, 128 színre konvertált képmásolatok; a részletezett elrendezést/UX-ötleteket szemléltetik, az eredeti képek a beszélgetés mellékletei.

## 1. Beállítások
![Beállítások / Óra és szinkron / Mentés és visszaállítás](01-settings.png)

## 2. Edzés indítása / Wear OS
![Edzés indítása és Órára küldés](02-workout-start.png)

## 3. Egészség / kapcsolat (opcionális)
![Samsung Health és Health Connect](03-health-sources.png)

## 4. Aktív edzés
![Kompakt sorozatok, pihenő és következő gyakorlat](04-active-workout.png)

## 5. Kültéri mérés
![Kompakt mérés és kis térkép](05-outdoor-measurement.png)

## 6. Teljes képernyős térkép
![GPS útvonal és lebegő statisztika](06-fullscreen-map.png)

## 7. Napló és PR archívum
![Szűrhető napló és rekordjelvények](07-journal-pr.png)

## 8. Programok
![Kompakt programkártyák és programtípus ikonok](08-programs.png)

**Agent-feladat:** mielőtt módosítasz, ellenőrizd a tényleges jelenlegi képernyőket/állapotot a main-derived branchen. Ne másolj vissza #271-es kísérleti navigációt. Dokumentáció-only PR; a UI/UX változtatásokat külön commitokban, tesztekkel kell megvalósítani.

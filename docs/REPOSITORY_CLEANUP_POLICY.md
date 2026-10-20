# TrainPilot repository-takarítás – 2026-10-20

**Eredeti felhasználói kérés:** a felesleges GitHub Actions cache-ek, régi workflow-runs, branchek és elavult leírások rendezése. Minden műveletnek meg kell őriznie a működő **1.3.7 /2734** telefonos és Wear OS release-t, a `main` történetét, a GitHub tageket/asseteket és az aktív PR-eket.

## Ellenőrzött leltár

- **112 Git branch**, közülük az áttekintett 126 lezárt PR alapján 75 korábban beolvasztott PR-hez kapcsolódik, és legalább 47 több mint 7 napja összevont ág lehetséges jelölt. Ez **nem** jelenti automatikusan, hogy mind a 47 branch törölhető: az utolsó branch-commitnak a `main` előzményében is szerepelnie kell.
- **4783 GitHub Actions run** az ellenőrzéskor. Ezek történeti futások; az új `#231` Wear Round Display optimalizálás aktív futásait nem szabad megzavarni.
- **150 Markdown-fájl** (kb. 2,8 MB), köztük régi release-jegyzetek, üzemeltetési emlékek és aktuális fejlesztési dokumentumok.
- **12 workflow** és **2 nyitott PR**: #225 (funkcióaudit), #231 (kerek kijelzős UI-teszt gyorsítása). Az aktív PR-eket nem töröljük.
- Az Actions cache-ek darabszáma a jelenlegi GitHub connectorral nem kérdezhető le; a külön GitHub Actions audit workflow, a repó saját tokenjével készíti el a hiteles cache-listát.

## Bevezetett biztonságos, kétlépcsős folyamat

`.github/workflows/repository-cleanup.yml` – új, kézi indítású karbantartás:

1. **Törlés nélküli audit (alapértelmezés)**: a repo saját GitHub-tokenjével beolvassa a teljes branch-, nyitott/zárt PR-, run- és cache-listát. Létrehoz egy JSON-jelentést a lehetséges törlésekről és a manuális felülvizsgálatra szoruló ágakról. A tesztek ellenőrzik a védelmeket.
2. **Külön megerősített alkalmazás**: csak a `main` ágon, manuális `workflow_dispatch` esetén, `apply=true` és pontos `DELETE_OBSOLETE_TRAINPILOT_DATA` megerősítéssel. Egy futásban **legfeljebb 300** törlés; az összes jelöltet minden alkalmazás előtt frissen ellenőrzi.

Javasolt, a workflow-ban állítható minimális megőrzések:

| Elem | Alapbeállítás | Soha ne törölje |
| --- | --- | --- |
| Actions-futások | 14 nap | Futó/függő futások; minden workflow 5 legújabb futása; sikeres `main` telefon/Wear Release Gate; deployment/release futás |
| Actions cache | 7 nap az utolsó használat óta | Nem lejárt cache, beleértve az aktív teszt/build számára használhatót |
| Branch | Beolvasztás után 7 nap | `main`, védett/aktív PR ág, `archive/`/`backup/`, nem bizonyítottan összevont ág, `main`-től eltérő commit |
| Tagek / GitHub Release / APK | Nem törölhetők ezzel | Minden kiadási tag, bináris, aláírás és kiadási jegyzet |

A branch-jelölteket a PR-merge dátuma és a branch teljes head-SHA-jának `main`-be beépülése **együttesen** minősíti. A nem bizonyítottan beolvasztott ágakat külön kézi áttekintési listába sorolja.

## Dokumentáció

A `README.md` és `README.hu.md` élő, 1.3.7-es letöltési forrásként kezelendő. Az aktuális munka átadási forrása a [WORK_HANDOFF.md](WORK_HANDOFF.md); a történeti fejlesztési beszélgetés-/Work-jegyzőkönyvek **nem tekinthetők aktuális állapotnak**. A korábbi kiadási jegyzetek, teljesítménymérések és elfogadási bizonyítékok megmaradnak, mert támogatási, visszaállítási és hibakeresési hivatkozásokban szerepelhetnek. Fájlok végleges törlése csak hivatkozásellenőrzés után külön PR-ben.

## Kezelés

A karbantartás GitHub Actions-felületen a **Repository cleanup audit and controlled pruning** workflow kézi indításával végezhető. Elsőként **apply=false** mellett olvasd el a `TrainPilot-cleanup-audit-...` artifactot; a találati darabszám és lista áttekintése után indítható a külön, egyértelműen jóváhagyott `apply=true` futás. Nincs ütemezett, felügyelet nélküli törlés.

# Vizuális munka: közös alap, majd két elkülönített képernyőcsomag

2026-10-27. Első P0 increment folyamatban, a P1 csomagok még NINCSENEK kiosztva.
Baseline: közös zöld31dbcde2 /40458015954; main/verzió nem változik.

## Egy gazda a közös rendszerhez

Root: ui/theme/**, ui/components/TrainPilotSurfaces.kt, NativeMainHeader,
navigáció prezentációja, Kezdőlap/Edzés landing és integráció. Öt fül,
grafit/arany referencia, mind32paletta/4nyelv és a mentett téma megmarad.
P0most: közös szerepalapú tipográfia, térköz/méret/sarok skála, színhelyes Material
error/outline tokenek, SurfaceCard/SectionHeader/PrimaryButton. A komponensek
nem töltenek adatot és nem írnak állapotot; meglévő callbacket és lokalizált szöveget kapnak.
A P0 teljes vizuális könyvtára és runtimeelfogadása még nincs kész.

## P0 API-fagyasztás és ellenőrzés után kiosztható

| Gazda | Saját képernyők | Csak olvasható határ |
|---|---|---|
| Chat A | JournalScreen + saját napló prezentációs komponensek; ProgressScreen + saját grafikon/kártya megjelenítők | VM/DAO/paging/backup/fotówriter/térképprovider/metrics |
| Chat B | HealthDashboardScreen + saját egészségkártyák; CoachScreen + saját forrás/freshness megjelenítők | Health provider/mérésnapló/alvásadat/VM/CoachReadiness/számítás |
| Root | Közös tokenek/komponensek + HomeScreen/TrainingLandingScreen + végső vizuális összevetés | Meglévő engine/adat/migráció/ownership readonly a dizájnfeladatban |

Mindkét chat a Root által megadott azonos P0 commitból induljon, saját ágon/draftPR
célmigration, main/migration push és merge nélkül. Ez nem mostani indítási utasítás.
Közös theme/component fájlt a két chat nem írhat. Hiányzó közös komponensre Rootnak
konkrét igényt adnak, nem saját párhuzamos Card/Button rendszer készül.
Saját locale XML egyedi prefixszel, HU/EN/DE/RO kötelező. Meglévő business testek,
ID-k/testTag-ek, callbackek és funkciók maradnak. Additív vizuális/a11y próbák saját
fájlban; canon workflow és meglévő közös UIteszt második író nélkül.

A négy tényleges mockup + docs/design/final-ui-ux specifikáció közös referencia.
Elfogadás: 320dp/kis képernyő, nagy font, DE/RO hosszú címke,48dp,kontraszt,
üres/töltő/hiba/forráshiány, többpaletta és valódi rawemulátorképek.
A grafikon és forrásszöveg ugyanazt a meglévő adatot mutatja, saját értelmezési képlet nélkül.
Root finalreview ugyanazonfixture/nézet before/after képeivel; a saját scopedgreen
nem teljes közös elfogadás. Aktív edzés P2 és G03/G06/G07 funkcióhook külön marad.

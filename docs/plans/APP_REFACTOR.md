# Alkalmazás rendbetétele

Felhasználói indítás: 2026-10-21. Kiinduló main `bc43d4e`, 1.3.71 /2735.
Az audit és Claude elemzése alapján fokozatos egyszerűsítés; verzióemelés és main
merge nincs automatikusan engedélyezve.

## 1. Mentési integritás és központi lezárás – #235, készülékes próba előtt

- A `finishWorkout` korábbi 11 definíciós/hozzárendelési helye és a dinamikus
  finish-wrapperek helyett egy belépési pont és név szerint hívott lépések.
- Natív fence/reconcile → automata és intervallum leállítás → helyi stopperek/GPS
  lezárása → részleges edzés megerősítése → tartós history/naptár → natív close →
  draft takarítás → UI/értékelés és késleltetett Health-frissítés.
- Stabil azonosítós upsert, ütköző írásnál rollback, rollback/folyamatleállás után
  a tartós draft `finalizationPending` jelzője védi az újrapróbálást. Ez a jelző
  nem kerül a végleges naplóba. Nyugta csak a teljes folyamat után küldhető.
- Sérült history/scheduled nyers JSON-nál a lezárás megtagadja a felülírást;
  ez nem a teljes alkalmazás általános `db.get`-helyreállításának megoldása.
- Az előtöltés `previousValues228` segédmezője a futó/draft állapotban maradhat,
  a végleges naplópillanatképbe nem kerül.
- Célzott regresszió: írási határok/rollback/újrapróbálás/reload, natív lezárási
  hiba, nyugta visszatartása, megerősítés megszakítása/dupla kattintás, régi
  sorozat/stopper/naptár/órás lezárás, GPS külön OK nélküli teljesítése.

## 2. Napló és PR teljesítmény – következő kör

- Kulcs/alias → sor index; kártyánkénti teljes naplókeresés kiváltása.
- Cache a tényleges history/tombstone-változáshoz; azonos elemszámú szerkesztés,
  törlés, import és Drive/Wear frissítés sem maradhat láthatatlan.
- Napló fokozatos megjelenítése/lapozása, a részletek, PR-jelvény és fókusz
  viselkedését megőrizve. 200/1500 szintetikus edzéses összehasonlítható mérés.

## 3. Render/CSS és további modulok – később

- Közvetlen oldal-specifikus kompozíció, régi általános render-burkolások kiváltása.
- Kijelölt CSS/téma/tipográfia tulajdonos; új stílusjavítás ne újabb késői
  felülírásként kerüljön a fájl végére.
- Tárolás/migráció, workout, Napló/PR, előtöltés/Coach, UI, natív adapterek
  világos határai. Fájlokra másolás önmagában nem egyszerűsíti a függőségeket.
- Phone/Wear stabil ID/protokoll, backup és GPS adatvédelmi döntések megőrzése.
- A natív FGS/receiver auditkockázatok külön kis körben kezelhetők, a teljes
  webes migráció befejezését nem kell megvárniuk.

## Ellenőrzés és átadás

Helyben célzott próbák; teljes Quick/Performance/Chromium/natív/UI kapuk GitHubon.
A készülékes elfogadás külön szükséges. A régebbi zöld futás nem igazolja az új
alkalmazásforrást. A következő kör mindig az aktuális handoffból induljon.

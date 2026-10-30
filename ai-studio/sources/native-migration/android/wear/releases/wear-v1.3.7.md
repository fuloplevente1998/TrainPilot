# TrainPilot Wear OS 1.3.7

Telefon és Wear OS: **1.3.7 /2734**. A #230-as edzésfunkció-csomagot és a verzióemelést a felhasználó 2026-10-20-án jóváhagyta.

- Az órára küldött tervezett programnapok és gyors edzések a telefon Naplójából azonos gyakorlatonként örökölt **súlyt és ismétlést** tartalmazzák.
- A sorozatokat az óra mindig új, teljesítetlen edzésként kezdi; a korábbi RIR/állapot/időmérő/GPS és azonosítók nem öröklődnek.
- A telefonon megadott tényleges sorozat-RIR a két platform között adatvesztés nélkül megőrződik; önálló új órás RIR-szerkesztő UI nem része ennek a kiadásnak.
- Megmarad az önálló órás edzés, gyors edzés, korona-/lünetta-alapú adatbevitel, mérések és az offline telefon–óra szinkron.

A verzió és verziókód a telefonéval összehangolt. A GitHub Actions független **Wear Release Gate** készíti a korábban használt aláírókulccsal telepíthető APK-t, saját natív tesztekkel és aláírásellenőrzéssel. A telefonos és órás APK együtt frissítendő; Play Store közzététel nem része az automatikus workflow-nak.

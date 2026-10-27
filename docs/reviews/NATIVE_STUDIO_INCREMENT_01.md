# Studio increment01 – valódi forrásadaptáció

Eredeti ZIP f685d540:SHA574f17f639b91037a6e0bdea428f350d4c5950307227511eafc32e7f64bcefcc.
A teljesKotlin referencia16fájl byteazonosan a docs/ai-studio/export-20261027 alatt.
Native app/tesztjelölt: d7e64697; korábbi zöldalap f0c474f1/40529220611.

## Megvalósítva

A három eredeti font/OFL és StudioTypography/tnum, finomhero-gradient, közössötét
felszínek;32régi paletteID/kontraszt és systemfontScale megőrizve.
Studio-brandheader, Settings nyelv/téma/profile pontos szekciójára navigáló valódi
belépők. Csak UI-section pointer, a SettingsViewModel/writer és admission marad.
Home greeting/planner/snapshot+target hero,6valósmetrika, tervezési/Coach-belépő;
ugyanaz a start(program/day/schedule),resume/draft és adatok/epoch útvonal.
A Coachelőnézet forrással/partial/stale/failed jelzéssel valódi regenerációérték,
nem külön képlet vagy kitalált tanács. A teljesmainCoachpreview még hiányzik.
A plannerprofil meglévő Settingsprofileszerkesztő, automatikus programgenerálást
nem állítunk ezzel késznek; natív PersonalProgramPreview külön következő bekötés.

Nagyfont topnav: intrinsic vízszintesfülek, fontScale clamp nélkül. Új componentUI
320dp/font2DE valódiGetTextLayoutResult.hasVisualOverflow, selected és disabled assert.
A régi profilementési és HU/EN/DE/RO palette/language roundtrip próba megmarad,
és az új Home/header entryket is használja. Metrikák value/unit/source tagek nem
kerülnek a kattintható címsor alá, így a régi gyermeksemantika megmarad.

## Stabilitási javítás és bizonyítékhatár

5bbc topnavrun40535425696 phoneartifact12750260480:113main112PASS/1FAIL+4GPSPASS,
0missing/runnercrash. A hibás uiApprovalRejectsProgramRawChangeAfterShownComparison
awaitReady diagnosticja kétroot esetén második kivételt dobott, eredetivárásiokot
nem őrizte meg. d7e többrootot diagnosztizál, egyetlen scrollcontainerben tartja
az egész modecomparison szöveget, így hosszúcím/kevéshely nem nyomja0méretre a
belsőrows táblát. Teszt láthatóságasserttel bővül; approve readiness/CAS/sessionguard
változatlan. Ez jelölt fix, runtime eredmény még szükséges.

## Nem teljes még

Health/Progress teljes Studio-compose, Home valódiCoach-advice és programplanner,
Training/Journal teljesszerkezet, összes aktívmérés/finish/interval, program/library/
calendar/coach/settings/support képernyők. Mainanatómia/GPS szegmens/valósmérések
maradnak, demószöveg/állapotváltó/fake route nem kerül a termékbe. Main/verzió nincs
módosítva. A compile/JVM/UI/Wear/upgrade kapu és új natívPNG még hátra.


## Futó kapu

40539957380 exact0fb350a47b1c17a04851ce6a0f7529f510dad8d9 (app/tests d7e64697).
Compile lépés PASS, unit még fut. A65db698 és későbbi docs-only checkpointok nem
változtatják a fagyasztott appforrást. Új118phone várható, rawXML/log alapján ellenőrizendő.

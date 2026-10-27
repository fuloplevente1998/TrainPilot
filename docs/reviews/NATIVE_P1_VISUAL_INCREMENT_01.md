# P1 vizuális increment 01 – forrás és ellenőrzés

Bázis: 741fbfa5 dokumentációs HEAD; alkalmazásbaseline 39da06cc, Actions40481815367
7/7 SUCCESS, 1333 JVM+114 phoneUI. A korábbi zöld kapu nem az alábbi új kód eredménye.

## Megvalósított prezentációs változások

- Közös 36dp dekoratív ikonbadge, 12dp belső terű metric surface, címikon és
  stateless action surface. Minden számot tartalmazó tipográfiai szerep tnum.
- Home: valódi hat metrika és minden value/unit/source/status tag megmarad;
  ikonok és kompakt, egyforma magasságú intrinsic kártyák. A quick-kártyák
  ikonja a cím fölé kerül, így szűk szélességen a felirat nem préselődik az ikon mellé.
- Training: teljes szélességű fő quick CTA, alatta az existing library/outdoor
  callbackeket használó két action card, nagy betűnél egymás alatt.
- Journal: közös filterszekció, valódi dátum/idő és sorozatszám meta,
  strukturált címsor/chevron/PR-jelzés. Ugyanaz a lapozás/keresés/részlet/editor.
- Health: közös dátum-hero és szolgáltatói/szinkronpanelek; mind a26 metrika
  forrással/taggel megmarad, lazy intrinsic 2oszlopos sorok, nagy betűnél1oszlop.
  A refresh ikon érintési célja32→48dp. Provider/permission/refresh writer nem változik.
- Progress: közös muscle/strength panelek, mainből byteazonosan átvett anatómiai
  alapkép és6grayscale-maszk. A maszk fényereje alpha-csatornát ad a rajzolásban,
  mint a main luminance-mask-ja, csak valóban naplózott csoport kap kiemelést.
  A main <=33/<=66/>66 szemantikus színskálája közös tokenekbe került, nem oldalszín.
  Az arány továbbra a meglévő NativeProgressQuery modellje, nem új fáradtságszámítás.
- IO-szálas, megjelenítési méretre mintavételezett decode, legfeljebb768px target;
  nincs bitmap a mentett state-ben, natív adatbázisban vagy edzésframe-ben.
  Nagyítás saját dialógus, bezárás csak ezt zárja. A meglévő chart pontjain átmenő
  Bézier-görbe kontrollpontjai a szomszédos értékek tartományában maradnak.
- Fotók/cardio/weight/strength-range belépők és minden meglévő session guard megmarad.
  Új névrokon repository/launcher/history rendszer, schema- vagy verzióváltozás nincs.

## Teszt és képek

- Új valódi MainActivity/UI próba sárga mentett palettával és explicit izolált
  szintetikus adattal: mind5főoldal, natív képkirajzolás/nagyítás/bezárás,
  adat- és palettamegőrzés. A screenshotok a meglévő host adb screencap útján készülnek.
- Új320dp/font2.0 hosszú DE metrika/egység/provider UI-próba.
- A progress választó meglévő tesztje függőlegesen is odagörget a magasabb panel alatt;
  az eredeti gyakorlatválasztási/assertion viselkedés megmarad.
- Statikus ellenőrzés: meglévő érintett testTag-ek és @Test metódusok megmaradtak;
  4locale/6új vizuális resource azonos kulcsokkal. Ez nem natív runtime PASS.
- Ezen az incrementen még nincs Android/JVM/UI futtatási eredmény vagy új kép.
  Várható phone:114korábbi+2új=116teszt; a tényleges XML alapján ellenőrizendő.

## A P1 még nem teljes – következő részek

A kártyák átvétele nem a teljes main-funkció-/vizuális paritás lezárása.
Hátra: Home Coach-előnézet és Training valódi next-target kötés; Journal teljes
summary/photo/exercise/route listaprojekció; Health mai hatos áttekintés és
pulzus-/súly-/rekord-/engedély-akkordok; Progress volume/időszak és interaktív
összegző/PR/lista részletek, izomcsoport-részletbelépők. Az 1RM meglévő trend,
26valós health-metrika, cardio/photo/weight már meglévő utak nem törölhetők.

A felhasználó szerint Wear OS, Google Health Connect és Samsung Health a fő
integrációk; a kínai órás BLE-próba nem külön új oldal. A natív örökölt BLE-tab
végleges szerkezeti rendezése nyitott; az increment nem épít új BLE-felületet,
és nem töröl régi mérést vagy protokollt.

P2–P5 nem kezdődött. Main merge/kiadás/verzió/készülékes elfogadás nincs.

## Main assetek eredete

Main alkalmazás-SHA:7473b30f387eda54186c1e31830304ca12bd104f.
Forrás: www/app.js:14226–14244 és www/progress-177.css:173–185.

- `progress_anatomy_main.webp` SHA-256 `fca3541aa7b03d529e055a6b25a5d152e90dabc16398d11717ec2d488e6a67bb`
- `progress_muscle_arms_main.png` SHA-256 `a1b02e2d537b05c25ba0b70195e695bd8f6777885b70d27df34626ef1f35f608`
- `progress_muscle_back_main.png` SHA-256 `83f51440eba3a804d9388d789aedcbcf888db2a46e5593892edf1b1e050878e4`
- `progress_muscle_chest_main.png` SHA-256 `aa03995815a573e60c441f2729a36d1d24aadfb203c1e13534fabf5dbb5a55f9`
- `progress_muscle_core_main.png` SHA-256 `04f8963280c9b8d9afa76fe60b499349ab191b810b67d299124b28a5125b5d81`
- `progress_muscle_legs_main.png` SHA-256 `8d7968655c1a950f3ed667bcf46c0fac3c0483d22d41c2bb798fe94b8698fb19`
- `progress_muscle_shoulders_main.png` SHA-256 `b4c7a2ea435aeca139938194bf35028951e7338ab5d299b9115690be2220b19f`

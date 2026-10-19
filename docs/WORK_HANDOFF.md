# TrainPilot – aktuális átadás

Frissítve: **2026-10-19**. Először [AGENTS.md](../AGENTS.md), majd ez a fájl.
A GitHub állapotát induláskor ellenőrizd; az alábbi eredmények pontos forráshoz tartoznak.

## Aktív lezárás és 1.3.6 kiadás

A felhasználó: **„Rendben, akkor mehet, zarhatjuk ezt a teljes részt es akkor 1.3.6.lesz”**.
Ez a #216 csomag lezárására/mainre emelésére és az **1.3.6 /2733** verzióra adott
jóváhagyás. Nem állítunk ennél részletesebb fizikai tesztbeszámolót.

- #216 Wear menü, megjegyzett GPS-választás és gyakorlatváltást túlélő pihenő:
  [PR #218](https://github.com/fuloplevente1998/TrainPilot/pull/218), fejlesztési head
  `e66754eb24990d4d69c863cbb3e401b2de97845e`, main alap `d1da634`.
- Végső alkalmazáskód `c04851f`; `509c5a3` és `885b6df` csak tesztjavítások.
  Native rest tesztben valóban nyitott sorozatot célozunk; UI teszt a rendszer
  nyelvéhez illeszkedő menüfeliratot és az utolsó, megerősítéses eldobást ellenőrzi.
- Az eredeti pihenőhatáridő, automata rest fázis/cél/tulajdonos megmarad; navigáció
  nem küld skipRest parancsot és nem indít másik sorozatmérést.
- GPS-választás új kültéri gyors/tervezett edzés explicit indítása előtt,
  aktivitásonként megjegyezve; reconnect/render nem indít rögzítést.
  Futó edzésről közvetlen GPS-kezelés; új szövegek HU/EN/DE/RO.
- #218 alatt a telefonos alkalmazásforrás változatlan, külön telefonos APK nem készült.
  A **verzióváltáshoz már mindkét APK újraépül** a #219-ben.
- [PR #219](https://github.com/fuloplevente1998/TrainPilot/pull/219),
  `fix/release-1.3.6`, forrás `7b600e4ed0ce65549c53596de6a02390e8e326b2` (csak UI-teszt javult az eredeti `081d0164` kiadási forrás óta).
  Metaadatok, mindkét Gradle-verzió, appfelirat, SW cache és régi verzióelvárások
  1.3.6 /2733-ra frissítve. Funkciótesztek tartalma megmaradt.
  Telefon/Wear kiadási jegyzet a megfelelő releases könyvtárban.

## Ellenőrzések / APK

- #218: Quick **39227738581**, Performance **39227738296**, Ownership **39227738309**,
  Wear release **39227730964** sikeres. Kerek UI **39227730992** és **39229575006** piros egy régi menüelváráson;
  a home fixture törli az aktív edzést, ezért az új sorrend első gombja Gyors edzés,
  nem Mini napló és nem Folytatás. A végső javítás `e66754e` / `7b600e4`; teljes kör újrafut.
- Korábbi azonos alkalmazásforrású XML: Wear **159/159**, Ownership **81/81**,
  0 failure/error/skipped. Source `509c5a3`, PR merge `e504ad9` tree azonos:
  `549ef2c42c95600661d22e0e98af44de13821e9d`.
- Átadott Wear teszt APK **1.3.51 /2732**, `509c5a3`:
  [GitHub artifact 12174690469](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39226908385/artifacts/12174690469).
  SHA-256 `37d098c18b59e6aee442d2714ad2c8075674c9e485a22638f60759399281474c`.
- Eredeti signer SHA-256: `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`.
- **Belső /workspace APK-linket a felhasználó nem tudott letölteni.** GitHub artifact
  permalinket adj (ZIP, benne APK), vagy a tényleges közzétett release APK linkjét.
- #219 kapuk: telefon **39229121428**, Wear **39229120836**, kerek UI **39229121232**,
  Quick **39229125646**, Performance **39229125667**, Ownership **39229125649**.
  Quick/Performance már zöld; többi eredményt ellenőrizni kell. Helyben csak
  metadata/backup-export/szintaxis/diff célzott ellenőrzés, mind sikeres.

## Következő lépések

1. #218 kerek UI XML, majd jóváhagyott merge és #216 completed.
2. #219 összes kapu, XML és aláírt APK ellenőrzése, majd jóváhagyott main merge.
3. Main workflow kiadása: `v1.3.6` és `wear-v1.3.6`; forrás/signer/hash és
   tényleges letöltések ellenőrzése, rövid eredmény a felhasználónak.

## Lezárt előzmények és hátralévő backlog

- #193/#202: önálló órás GPS, offline átadás/visszavétel és telefonos naplótérkép
  felhasználó által elfogadva; #213 main `311f507`, #193 completed.
- #215 kompakt Intervallumok main `c67006b`, elfogadva. A kétoldali UI nem
  engedélyez egyórás generikus motort, két pozitív idő kell a kész állapothoz.
- #212 korona/lünetta: #217 main `102eb366`, készülékes elfogadás „Szuper zárhatjuk”.
  Súly 0,5 kg / ismétlés 1 / idő 5 mp; stabil cél, Mentés/Mégse; nincs automentés.
- #188 visszavonás és #189 folyamatos 3 mp feloldás elfogadva/mainen.
- Telefonos GPS explicit sikeres „Mérés befejezése” külön sorozat-OK nélkül működik,
  elfogadva; térképbezárás/hibás/implicit stop nem teljesítés.
- Nyitott külön feladatok: **#206** maradék aktivitásnézet/UI; **#197** választható
  edzésstílusok/progresszió; **#184** Samsung Energy Score; **#214** karbantartás/Play.
  Ezeket az 1.3.6 nem zárja le. #192/#211 sztornózott, ne éleszd újra.
- [Külső UI/funkció backlog](plans/UI_TEST_BRANCH_BACKLOG.md): csak rögzítésre
  engedélyezve, megvalósítás még nem; később külön tesztág.
- Részletes #193 terv: [P2_193_ONALLO_ORAS_EDZES.md](wear/P2_193_ONALLO_ORAS_EDZES.md).
  Régi teljes átadás a Git-történetben `885b6df:docs/WORK_HANDOFF.md`;
  történeti státuszai nem írják felül a jelen dokumentumot.

## Környezeti megjegyzés

CLI GitHub-auth lejárt (gh 401, git push/fetch credential nélkül), a GitHub connector
működik. Ne kérj új login-t, ne írj ki credentialt. Tartalom API + Git tree/commit és
expected_sha CAS használható. Helyi és távoli commitazonosítók eltérnek; helyi `ff68e68` = távoli `7b600e4` alkalmazás-/teszttartalom. Aktuális helyi ág
`local-release-136`; a helyi feature ág is tartalmazza a release commitot, **ne pushold**
a távoli #218-ra. Nincs force push/reset. A távoli PR-head a kanonikus.

# Elkülönített külső munka – 2026-10-26

Repository: https://github.com/fuloplevente1998/TrainPilot
Exact induló commit: `e28ab41887a976ee380f63f465dbdcb51f494329` (appkód f72c9cb0).
Először AGENTS.md, docs/NATIVE_RESUME.md és docs/WORK_HANDOFF.md aktuális teteje,
majd docs/reviews/NATIVE_PR250_INTEGRATION.md és az alább kijelölt források.
Az induló közös kapu40317771457 még nem elfogadott teljes eredmény.

## Munkaszerződés

- Saját új ág ebből az exact base-ből, egy Draft PR célja feat/native-android-migration.
  Main/migráció írás, merge/rebase/force-push/PR vagy issue zárás tilos.
- Kis visszakövethető [skip ci] commitok. Kód és megírt teszt nem runtime PASS.
  Root a közös teljes CI és integráció gazdája; meglévő futást ne cancel/re-run-olj.
- Nincs belső subagent, új dependency, séma, verzió/csomag módosítás.
  com.repforge.app /1.3.71-native-test /2736 megmarad.
- A felhasználó külön saját ági tesztelést is szeretne: célzott teszt/teszt-APK
  előkészíthető, ágnév+exact SHA+teszteredmény+artifact/hash egyértelmű átadásával.
  APK/build/emulátor GitHubon; teljes natív workflow-k párhuzamos indítását Root
  koordinálja, ne indíts minden részcommit után teljes suite-ot. Új workflow-t ne készíts.
- Élő saját adatok fölé install vagy valódi Google-adatok törlése/visszavonása nem
  része ennek a feladatnak. Csak izolált tesztadat és meglévő guardok; semmit nem
  gyengítünk azért, hogy egy teszt zöld legyen. Készülékes elfogadás külön státusz.
- Új UI felirat a lent megadott saját HU/EN/DE/RO XML-ben, nem közös strings.xml.
  Meglévő tématokenek/48dp célok/Back és aktív edzés megőrzése kötelező.
- Csak az engedélyezett fájlterületen írj. AGENTS/RESUME/WORK_HANDOFF és más
  munkás riportja helyett saját review-handoff; új saját tesztosztályok.
- Záró átadás: exact HEAD, diff lista, UI→caller→writer tartós eredmény, megírt és
  ténylegesen futott tesztek külön, ismert korlátok, draft PR link. GitHub-komment nincs.

## Plus chat 2: offline CI-bizonyíték-összesítő

Ág: test/native-ci-evidence-summary

Cél: a letöltött natív CI artifactokból automatikusan pontos, rövid összesítő
készüljön. Ez futtatható Python eszköz, nem új appfunkció és nem új APK.

Írható: új scripts/native/evidence-summary.py;
új tests/native/test_evidence_summary.py (Python stdlib unittest);
docs/reviews/NATIVE_CI_EVIDENCE_SUMMARY_HANDOFF.md és egy használati MD.
Olvasd a scripts/native/phone-ui-gate.py, run-native-wear-ui.sh,
run-native-signed-upgrade.sh és native-android.yml meglévő szerződését.
Sem meglévő gate/script/workflow, sem alkalmazáskód/Gradle más teszt nem írható.

Konkrét kimenet:
- CLI helyi kibontott artifact könyvtár(ak), expected commit, optional szükséges
  gate/shard/screenshot paraméterek; JSON és rövid Markdown fájl. Nincs hálózat,
  GitHub token, adb, build, artifact/forrás módosítás vagy workflow-dispatch.
- App/Wear JVM XML totals/tests/fail/errors/skips; instrumentation class+method
  terminal állapotok, declared vs executed, crash és hiányos futás. Failure rövid
  kivonat + raw fájlhivatkozás; ne írj személyes rekordot/GPS-koordinátát/token/URL-t.
- Commit/APK-hash/manifest összhang, screenshot létezés/SHA256/PNG épség (stdlib);
  sérült/hiányzó/mixed-source evidence = UNKNOWN/FAIL/INCOMPLETE, sosem PASS.
- Wear sharding explicit csoport/index/összes shard szerint: három teljes shard
  összege és unique case-ek, hiányzó/dupla shard ne legyen teljes zöld.
- Saját szintetikus unittest fixture és ha elérhető, read-only valós artifact próba.
  Ne jelents teljes runtime UI PASS-t parser-egységteszt alapján.

Kötelező próbák:
1. Phone69 declared,18terminal/12pass/6fail + process crash → FAILED/INCOMPLETE,
   nem18/18 vagy69teszt PASS; missing51 külön, nincs kitalált testcase.
2. Phone69/69 pass, kötelező screenshots=0 → hiányos evidencia.
3. Wear három sikeres shard68/56/59 unique, global183 →183pass csak mindhárom
   azonos commitú teljes shard esetén; hiányzó/dupla shardot elutasít.
4. JVM1096/0/0 +Wear167/0/0 helyes összeg, hiba/skip/malformed XML/csonka log,
   eltérő SHA/PNG hash. Exitcode teljes kért bizonyítékra0, hibára/hiányra nem0.
5. Valós hibás phone artifact példa: Actions40291546367 /artifact12636867224
   TrainPilot-Native-Phone-UI-Evidence. Csak olvasd; futást ne ismételj/cancel-elj.

Átadásban pontos futtatási parancs, tesztszám és eredmény, output schema/korlátok.

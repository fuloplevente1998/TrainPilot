# TrainPilot native migration — Wave 1: öt párhuzamos chat diszpécser

**Ellenőrzés:** 2026-10-26. Migration branch `feat/native-android-migration` exact `a075a21956ea4bd2f1089a9bc106dbe7a598c264`; PR #238 draft; #263/#264 draft és saját scoped bizonyítékkal zöld, **nem merge-elt**. #265 a GitHub Actions source routing validációja, nem új natív funkció.

| Sorrend | Feladat | Handoff | Fájltulajdonos |
| --- | --- | --- | --- |
| Chat 1 | #263 G01 integrációs review | [G01 handoff](NATIVE_PR263_G01_INTEGRATION_CHAT_HANDOFF.md) | `ui/home`, saját Home-teszt/resource |
| Chat 2 | #264 G10 integrációs review | [G10 handoff](NATIVE_PR264_G10_INTEGRATION_CHAT_HANDOFF.md) | `ui/progress`, saját Progress-teszt/resource |
| Chat 3 | 5 Phone UI hiba | [Phone handoff](NATIVE_PHONE_FIVE_FAILURES_CHAT_HANDOFF.md) | `ui/workout`, navigation, Settings, Exercise Library custom delete; `NativePhoneNavigationTest` |
| Chat 4 | A07 main ↔ native reset parity | [A07 handoff](NATIVE_A07_CATALOG_RESET_PARITY_CHAT_HANDOFF.md) | `NativeProgramActions`, `TrainingPrograms` program reset és saját teszt |
| Chat 5 | G02–G12 source triage | [G02–G12 handoff](NATIVE_G02_G12_CURRENT_TRIAGE_CHAT_HANDOFF.md) | egyetlen `docs/reviews/` fájl |

## Kiosztás

Mind az öt új chatbe másold be: **„Végezd el a teljes feladatot az alábbi MD szerint, kizárólag a saját ágon. Futtasd a megadott célzott GitHub-teszteket, javítsd a hibákat zöldre, add át exact SHA/run/artifact bizonyítékkal és draft PR-rel. Audit-only feladatnál ne futtass runtime tesztet. Link: [a saját handoff URL-je].”**

**Integrációs rend:** Chat 1/2/3/4/5 saját ág + draft → Root review/QA-only workflow kizárás → G01/G10/phone/A07 szükséges production diff összefésülés → **egy közös zöld phone/JVM/Wear/signed upgrade kapu ugyanazon migration commiton**. Az A07 audit és G-triage priorizál, nem kötelező vakon új feature-t írni. P0–P6 final UI csak ez után: P0 foundation, P1–P5 külön fájltulajdonosokkal, végül P6 vizuális QA.

**Fagyasztott határok:** `main`, migration, verzió/package/schema, globális UI design, G06/G08/G09/G11 új mockup-API-k. A PR #263/#264 saját ági `.github/workflows/native-android.yml` **nem átveendő**. Az öt chat önálló, automatikus merge/branchváltás tilos.

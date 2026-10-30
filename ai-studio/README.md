# TrainPilot – Google AI Studio / natív újraépítés

> 2026-10-30. A main ágra készített, **csak olvasható forrás- és dizájnreferencia**. Az ai-studio/ alatt lévő fájlok nem részei a kiadott alkalmazás buildjének. A gyökérben maradt régi TrainPilot 1.3.71/2735 WebView-alkalmazás változatlan. A referencia nem engedély a jelenlegi APK felülírására vagy automatikus publikálásra.

## Indítás az AI Studio-ban

**Először olvasd el a [TRAINPILOT_NATIVE_MASTER_SPEC.md](TRAINPILOT_NATIVE_MASTER_SPEC.md) dokumentumot.** Ez a projekt egy új, valóban natív Kotlin / Jetpack Compose Android appot kér, a régi TrainPilot funkcionális paritásával és biztonságos visszaállítással. Nem egy WebView-wrapper újraskinezése.

A GitHub-importált repository fő gyökere továbbra is a régi WebView app. A feladat új projekt/forrásmappa létrehozása, a referenciafák read-only kezelése. A generált új projektet külön repositoryban vagy elkülönített fejlesztői ágban tartsd, NE módosítsd az éles main telefon- és Wear-kódját.

## A három elsődleges forrás

| Hely | Tartalom és rang |
| --- | --- |
| [sources/native-migration/](sources/native-migration/) | Teljes natív **alkalmazásforrás**: Android telefon, Wear, shared, Kotlin, Gradle, migráció, tesztek, szelektált dokumentáció és mockupok. Forrás: feat/native-android-migration @ 648e971034201511d69585cd5deecaa27eddf942; **nem éles/elfogadott**. A cache, build-output és személyes beszélgetésexport szándékosan hiányzik. |
| [sources/pr279-webview/](sources/pr279-webview/) | PR279 megvalósított, de nem mainre merge-elt UI és viselkedési delta: www/, tesztek, összehasonlítások. Forrás: feat/pr277-uiux-implementation-20261029 @ 5ff8cbd0da6533d04e0696229cb013936ae39619. |
| [design/pr277/](design/pr277/) | A PR277 pontos magyar terve + nyolc valódi referenciaképe (01–08); 2×4 meglévő főmenü. Forrás: feat/uiux-page-content-20261029 @ 9aa3721ca675cd66b4fa04d1c91e2b6a871c04ed. |

A gyökér [README.md](../README.md), [docs/FEATURES.hu.md](../docs/FEATURES.hu.md) és a tényleges main www/ + android/ a működő régi funkciók mérvadó forrása. A natív funkcióaudit: [audits/](audits/). A legfrissebb PR279 nincs jelen a gyökérben; csak a referencia-másolatában.

## Kötelező sorrend és ütközések

1. **Funkcionális igazság:** publikált main, majd a PR279 igazolt javításai, és csak végül a kísérleti natív ágon talált, megfelelően ellenőrzött implementációk.
2. **Vizuális igazság:** PR277 nyolc képe, a hozzájuk kapcsolódó PR279 friss változásai, és opcionálisan új AI Studio mockup. Másodlagos inspiráció a külön korábbi Studio 18-képes és natív 24-nézetes terv.
3. **Navigáció:** a PR277 kéréséhez az eredeti **2×4 főmenü** tartozik. A korábbi natív Studio öt felső fülje és a PR271 kísérleti navigációja NEM előírás; csak külön felhasználói döntéssel váltsa fel.
4. Sem a PR zárása, sem egy emulátoros zöld teszt nem bizonyítja a valós telefon/Wear, Health/GPS vagy adatmigráció teljes készülékes elfogadását.

## Biztonsági szabályok

- Ne töröld, ne írd át és ne migráld automatikusan az élő felhasználói adatokat.
- A WebView JSON/ZIP backup importját tranzakciósan készítsd; előbb teszteld külön eszközön.
- Ne használj hardcoded egészségadatot, útvonalat, edzésnapot, szinkronstátuszt vagy fiktív órás API-t.
- Maradjon egyetlen kanonikus edzés, egy tulajdonos, idempotens mentés, PRIVÁT GPS-koordináták és csak visszaigazolt órás átadás.
- APK-csere csak package ID + signer + monoton növekvő versionCode + visszaállítási próba után lehetséges. Ettől eltérő kulcs esetén külön telepítés és explicit export/import.
- Ha a natív generálás nem kivitelezhető, készíts előbb ténylegesen működő Compose UI prototípust és külön jelöld a még nem bekötött motorokat; ne nevezd kész natív appnak a WebView-t.

## Eredeti GitHub-ágak / PR-ek

- [main](https://github.com/fuloplevente1998/TrainPilot/tree/main) – megőrzött 1.3.71/2735 működő régi app
- [PR279](https://github.com/fuloplevente1998/TrainPilot/pull/279) – nyitott Draft, UI + Coach/RIR/napló javítások
- [PR277](https://github.com/fuloplevente1998/TrainPilot/pull/277) – archivált specifikáció és nyolc kép
- [PR238](https://github.com/fuloplevente1998/TrainPilot/pull/238) – archivált natív migrációs követelmény- és elfogadáslista
- [native-migration](https://github.com/fuloplevente1998/TrainPilot/tree/feat/native-android-migration) – saját natív fejlesztői ág
- [PR250](https://github.com/fuloplevente1998/TrainPilot/pull/250) – 107 soros, forráskötött natív paritásaudit
- [PR262](https://github.com/fuloplevente1998/TrainPilot/pull/262) – 24 nézet/81 követelmény, natív design audit

A mappa **referencia**, nem git merge vagy release. Az eredeti ágak megmaradnak.

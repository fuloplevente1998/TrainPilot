# A Google AI Studio referencia-snapshot pontos eredete

Elkészítve 2026-10-30, a TrainPilot main gyökéralkalmazás módosítása nélkül.

| Referencia | Source branch / commit | Tartalom | Mit nem tartalmaz |
| --- | --- | --- | --- |
| Stable root app | main @ 8a36502da5832a42f1cb91708ecfed5cf4f9ed62 | eredeti kiadott WebView alap, natív háttér / Wear | PR279 külön módosításai |
| sources/native-migration | feat/native-android-migration @ 648e971034201511d69585cd5deecaa27eddf942 | android, root Gradle, scripts, tests, migration-source, docs/design, docs/reviews, docs/plans, handoffok, forrásmigráció | a teljes ág 1:1 Git-snapshotja NEM: .gradle cache, build output, .github workflow másolat, beszélgetés JSON export és egyéb régi root anyag kihagyva |
| design/pr277 | feat/uiux-page-content-20261029 @ 9aa3721ca675cd66b4fa04d1c91e2b6a871c04ed | uiux-2026-10-29 könyvtár és 8 PNG | kiadatlan app runtime |
| sources/pr279-webview | feat/pr277-uiux-implementation-20261029 @ 5ff8cbd0da6533d04e0696229cb013936ae39619 | www teljes tree, tests, pr277-implementation report és 8 összevetés | a változatlan régi main Android motor, az új natív Compose app |
| audits | PR250 @ fb64307f67459a00def6b81890bc4ba337da00d6 | 107 soros Markdown/JSON leltár | friss HEAD-re automatikusan újraértékelt runtime eredmény |

A forrásbeli képek és kódok **csak referenciák**. A root Gradle/package.json/webview build változatlan. A PR277 eredeti 8 referenciaképe: design/pr277/references/. A másik natív Studio 18 képe külön Drive-link/index, a teljes eredeti képsor nincs itt duplikálva.

Biztonság: semmi nem léphet automatikusan mainre a generált projektből; adatokat törölni/éles adatok fölé telepíteni tilos explicit backup és acceptance nélkül.

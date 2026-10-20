# #214 – repository-karbantartás és nyitott Play-előkészítés

Felhasználói engedély, 2026-10-20: „Github verziok, leirasok rendezese,jarbantartas johet”. Kiinduló main: `a3a4ddce2250c32ef5b55e9ed8733b6a3aa85bda`.

## A mostani repository-rendezés

- Angol fő README és friss magyar változat; külön kiadott APK/main állapot, aktuális dokumentumhivatkozások, szintetikus CI-képek.
- Négy verziócsaládra rendezett változáslista és 37 release dátum/platform szerinti jegyzéke.
- Történeti tagek/mellékletek megőrzése, ellenőrzött leírásfejlécek; későbbi main nem írja felül a régi APK leírását.
- Telefon/Wear/package/lock/látható verzió egyezésének CI-ellenőrzése, célzott tesztekkel.
- Proprietary `UNLICENSED` csomagjelölés. Kereskedelmi feltételek véglegesítése továbbra is külön döntés.
- Friss Play-előkészítési lista, az elvégzett forrásmunka és külső Console/partneri lépések megkülönböztetésével.

Ez karbantartási ág/Draft PR; nem új alkalmazásbuild, verzióemelés, Play-publikálás vagy általános dizájnengedély.

## Régi karbantartási ág és natív audit

A #121 archivált ága (`maintenance/open-source-release-hygiene`, `9602b7c10baa2aa3195df31a881b012e1424d208`) a vizsgálatkor 11 committal előrébb, 443-mal lemaradva állt a mostani mainhez képest. Nem emeljük át változatlanul.

A jelenlegi `HealthBridgePlugin.java` 210, a `BleDiscoveryPlugin.java` 534 sor. A friss kód már külön Health-journal/projection/energy és BLE-policy segédeket használ: HealthConnectJournal, HealthJournalStore, HealthDailySource, HealthProviderProjection, HealthWorkoutEnergy, BleScanPolicy, BleReconnectPolicy, BleConnectionLease és BleWatchPreference.

A régi ág HealthConnectApi34/BleGattIntrospection/BleScanCatalog bontása nem igazolt javítás a mai kódhoz. A mostani dokumentációs/verziós feladathoz nem szükséges natív átemelés. További bontás csak konkrét mai felelősségi határ és a kapcsolódó natív regressziók alapján végezhető; ez az audit nem állítja a régi refaktort beolvasztottnak.

## Amiért #214 nyitva marad

- Samsung production partner-regisztráció: package + kiadási signer + ENERGY_SCORE scope, normál felhasználói hozzáférés terjesztett builddel.
- Végleges kereskedelmi/terjesztési feltételek és külső összetevők nyilvántartásának felülvizsgálata.
- Play-listing, adatvédelmi/támogatási URL, Data safety és Health apps nyilatkozatok.
- Play App Signing/upload key stratégia, production OAuth, Console-beállítás és Play-tesztsávból telepített készülékes próba.
- GitHub About/Topics külön felületi beállítás, ha a rendelkezésre álló connector nem támogatja.

[Play-lista](PLAY_STORE_CHECKLIST_2026.md) · [Verziószabály](VERSION_POLICY.md) · [Aktuális átadás](WORK_HANDOFF.md).

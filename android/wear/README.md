# TrainPilot Wear OS

Natív Kotlin/Compose társalkalmazás Wear OS-re, minimum API 30. A telefonos TrainPilot továbbra is Capacitor/WebView és Java alapú; a Wear modul külön buildelhető és kiadható.

**Aktuális verziócsalád: 1.3.** A mainen edzésvezérlés, gyors edzés, mini naptár, Health Services-mérés, Mini napló és a korábbi mért edzések előzménye érhető el. A telefonról kezdeményezett órás appindítás az 1.3 javítási sorozatának része a [#160 tesztágon](https://github.com/fuloplevente1998/TrainPilot/pull/160). A megnyílást a felhasználó már visszaigazolta; a teljes háttérmérés ellenőrzése folyamatban van.

Az 1.2 adta a Wear OS alapfunkciókat; az 1.3 a telefon és az óra közös gyorsedzés-/napló-/mérési adatútját fogja össze. [Összevont változáslista](../../RELEASE_NOTES.md) · [Aktuális fejlesztési állapot](../../DEVELOPMENT_STATUS.md).

## Modul és ellenőrzés

A telefon őrzi a mentett edzéseket. Az óra helyi cache-t, tartós offline parancssort és az edzésazonosítóhoz kapcsolt mérési összegzést használ. A Mini napló olvasási nézet; nem külön naplóadatbázis.

Az `android` könyvtárból:

```bash
./gradlew :wear:assembleDebug
./gradlew :wear:testReleaseUnitTest
./gradlew :wear:assembleRelease
```

Az aláírt release buildhez a [buildleírás](../../docs/BUILD.md) szerinti signing-beállítások szükségesek. Az önálló Wear Release Gate a natív teszteket, APK-metaadatokat és a kiadási aláírás folytonosságát ellenőrzi. Közös telefon–óra protokollváltozásnál a telefonos kapu is szükséges.

- [Wear OS architektúra, mérési működés és készülékes tesztek](../../docs/WEAR_OS.md)
- [Órás 1.3 kiadás](https://github.com/fuloplevente1998/TrainPilot/releases/tag/wear-v1.3.0)
- [Történeti kiadási feljegyzések](releases/README.md)

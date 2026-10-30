# TrainPilot Wear OS

Natív Kotlin/Compose társalkalmazás Wear OS-re, minimum API 30. A telefonos TrainPilot továbbra is Capacitor/WebView és Java alapú; a Wear modul külön buildelhető és kiadható.

**Aktuális main: 1.3.4 / 2730**, a [#179](https://github.com/fuloplevente1998/TrainPilot/pull/179) közös fejlesztési alapjával. Megújult, telefonos ikonokat/témát követő kezdőlap, `HH:mm:ss` kezdőlapi óra, kis `HH:mm` gyakorlatoldali óra, külön Beállítások, közvetlen edzéstörlés, felfelé húzható menü és tördelt Mini napló társul az edzés-/mérési funkciókhoz.

Az első natív órás automata mód órán indított edzésnél a kézi rögzítés után pihenőt/gyakorlatot léptet, `mp` egységnél munka/pihenő/köröket futtat; a telefon ugyanazt az idővonalat vezérli és tükrözi. A 3–2–1 rövid és 1 másodperces lejárati rezgés implementálva. Telefonos indításnál egy új sorozat órás rögzítése még várhat az alvó telefonos felület visszatérésére. A fizikai automata próba hátravan; [aktuális korlátok és terv](../../docs/wear/WEAR_OS_FEJLESZTESI_TERV.md).

Az eredeti [Wear 1.3.4 release](https://github.com/fuloplevente1998/TrainPilot/releases/tag/wear-v1.3.4) APK-ja külön, korábbi kiadott forrás. A legutóbbi automata teszt-APK és ellenőrzése a [fejlesztési állapotban](../../DEVELOPMENT_STATUS.md) szerepel.

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
- [Órás 1.3.4 kiadás](https://github.com/fuloplevente1998/TrainPilot/releases/tag/wear-v1.3.4)
- [Új ChatGPT-beszélgetéshez](../../docs/CHATGPT_FOLYTATAS.md)
- [Történeti kiadási feljegyzések](releases/README.md)

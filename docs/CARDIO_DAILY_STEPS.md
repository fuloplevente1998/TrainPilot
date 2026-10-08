# Napi aktivitás a kardióstatisztikában

A kardióstatisztika a naplózott mozgások mellett külön **Napi aktivitás / lépések** csoportot kínál. A napi lépések a meglévő Health-napló vetületéből jönnek; nem keletkezik új edzés vagy kilométeres rekord. A csoport az összes mozgás nézetben is megjelenik.

A mai összes lépés, az edzésen kívüli lépés, a jelenlegi és az előző helyi naptári hét összesítése látható. A heti értékek csak a rendelkezésre álló napokat összegezik, a lefedettséget külön jelöljük. A hét hétfőn kezdődik; a napi lista az utolsó hét naptári napot mutatja. Nem becslünk kilométert, tempót vagy kalóriát a lépésekből.

A „Lépésadatok frissítése” az elmúlt és az aktuális hét napjaira, előtérben kér le lépésösszesítést. A napi és az edzésidőszaki kérés ugyanazt a szolgáltatót és Health Connect esetén ugyanazt az eredeti adatforrást használja. Samsungnál külön, csak lépést olvasó SDK-végpont szolgálja ki a kérést. A Samsung-vetület a lépések valódi eredetét is megtartja, ha azok Health Connect fallbackből származnak. Engedélyt ez a művelet nem kér; a meglévő lépésolvasási engedély szükséges.

Az elkülönítés a befejezett, érvényes TrainPilot-naplóbejegyzések kezdete és vége alapján történik. A teljes bejegyzés időablakát használjuk, a szüneteket is beleértve; ez nem fiziológiai mozgásfelismerés. Az átfedő és megismételt időablakokat egyesítjük, az éjfélt átlépő edzést helyi napokra vágjuk. A jövőbeli, befejezetlen és érvénytelen bejegyzéseket nem vonjuk le.

A napi összesből az egyesített edzésablakokra lekért lépésszámot vonjuk ki. Hiányzó ablakérték, hiányzó engedély, forráseltérés vagy a napi értéknél nagyobb edzésösszeg esetén az edzésen kívüli mező gondolatjel marad. A valóban mért nulla megmarad nullának. Ha nincs naplózott edzés azon a napon, az összes ismert napi lépés a naplózott edzéseken kívülre esik. Ez a csoport nem zárja ki a Samsungban külön rögzített, de TrainPilotba nem naplózott edzés lépéseit.

Az időszaki eredmények memóriában maradnak. Naplóváltozás, adatforrásváltás, új Health-vetület vagy a napi időszak lezárulása érvényteleníti őket. Későn visszaérkező kérés nem állítja vissza egy törölt edzés levonását. A natív olvasás nem ír Health-adatot; a napi adatok tartós tárolása továbbra is a meglévő Health-napló feladata. Legfeljebb 30 egyesített edzésablak/nap kérdezhető, a felületi kör 45 másodperces, egy kérés 15 másodperces korláttal. Túl hosszú kör esetén részleges állapot látható.

A natív időablak maximuma 26 óra, így az őszi óraátállítás 25 órás napja is olvasható. A korábbi Health Connect lépésolvasó explicit eredet nélkül megőrzi a platform forrásprioritását; az új, opcionális eredet csak a kiválasztott forrásra korlátozza a kérést.

Ellenőrzések:

- `tests/cardio-daily.cjs`: hiányzó/nulla adatok, részleges hét, forrás, átfedés, éjfél, jövőbeli/érvénytelen bejegyzés és óraátállítás.
- `tests/browser/cardio-daily.cjs`: külön mozgáscsoport, forráshoz kötött kérések, hiányzó/ellentmondásos adatok, edzéstörlés és késői válasz, napváltás, 16 nyelv–kijelzőméret kombináció.
- Natív Samsung-tesztek: csak lépés-aggregálás, tényleges nulla és hiányzó adat, engedély nélküli olvasás kizárása, fallback eredetének megőrzése. Ezek a következő Android buildben futnak.

A felületi előnézet szintetikus mintaadatokkal készül. Az eszközön megosztott lépések időszaki pontosságát az új telefonos APK-val kell ellenőrizni.

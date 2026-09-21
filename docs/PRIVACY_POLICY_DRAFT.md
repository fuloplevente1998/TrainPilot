# TrainPilot adatvédelmi tájékoztató – publikálás előtti tervezet

Ez a dokumentum kiinduló tervezet a Google Play és Google OAuth publikáláshoz. A végleges változatot publikus HTTPS oldalon kell közzétenni, és a tényleges kiadási működéssel egyezően kell tartani.

## Kezelt adatok

A TrainPilot a felhasználó által megadott vagy engedélyezett alábbi adatokat kezelheti:

- edzésnapló, gyakorlatok, sorozatok, ismétlések és terhelések;
- testsúly és alkalmazásbeállítások;
- Health Connectből a felhasználó által engedélyezett fitnesz/egészség adatok;
- opcionálisan a felhasználó által kiválasztott vagy készített edzésfotók;
- Google-kapcsolat használatakor a Google profil alapadatai, valamint a Drive/Naptár funkció működéséhez szükséges OAuth hozzáférés.

## Edzésfotók

A fotófunkció opcionális. A TrainPilot nem kér teljes médiatár-hozzáférést. A felhasználó rendszer képválasztón keresztül választ ki egy képet, vagy külön kameraalkalmazással készít fotót. A TrainPilot a saját privát alkalmazástárhelyére optimalizált JPEG-másolatot ment. Az újramentés során az eredeti EXIF/GPS metaadat nem kerül tovább a TrainPilot-példányba.

Ha a felhasználó bekapcsolja a Google Drive szinkront, az edzésfotó külön fájlként a felhasználó saját Google Drive `appDataFolder` területére kerül. Ez a mappa a normál Drive fájllistában rejtett, és az alkalmazás a `drive.appdata` scope-pal csak a saját alkalmazásadatait éri el.

## Adatok tárolása és továbbítása

Alapállapotban az alkalmazás adatai a készüléken maradnak. A Google Drive és Google Calendar funkciók csak a felhasználó külön Google-kapcsolata és engedélye után továbbítanak adatot a Google szolgáltatásaihoz. A TrainPilot projekt jelenlegi architektúrája nem használ saját fejlesztői szervert a napló vagy a fotók tárolására.

## Törlés

A felhasználó az alkalmazásban törölhet naplófotót. Drive-szinkron használatakor a törlés a következő sikeres szinkron során a TrainPilot által létrehozott Drive-fotófájlra is továbbvezethető. A Google-kapcsolat megszüntethető az alkalmazásból, a Google-fiókban pedig az alkalmazás hozzáférése külön is visszavonható.

## Biztonság

A Google OAuth hozzáférési token nem kerül JSON-backupba, JavaScript localStorage-ba vagy alkalmazásnaplóba. A fotók app-private tárhelyen vannak, a Drive-hoz a minimális `drive.appdata` hozzáférés használatos.

## Kapcsolat és publikus adatok

Publikálás előtt ki kell tölteni:

- adatkezelő/fejlesztő neve;
- támogatási e-mail;
- publikus weboldal;
- adatvédelmi tájékoztató publikus HTTPS URL-je;
- hatálybalépés dátuma.

# TrainPilot – Google-beállítás

A Google-integráció forrása a projekt része, de a saját Google Cloud / OAuth konfigurációt külön kell létrehozni. Credential vagy kliens-titok nem kerülhet a repositoryba vagy az APK forrásába.

## Egyszeri beállítás

1. Nyisd meg a Google Cloud Console-t, és hozz létre egy saját TrainPilot projektet.
2. Engedélyezd a Google Drive API és Google Calendar API szolgáltatásokat.
3. Google Auth Platform → Branding: TrainPilot név, támogatási/kapcsolattartási e-mail és a szükséges publikus adatok.
4. Audience: a kiadási modellnek megfelelően állítsd be.
5. Clients → Create client → Android. Csomagnév: `com.repforge.app`. Az SHA-1/SHA-256 értékeket az **aktuális saját release signing kulcsból** generáld; régi debug-kulcs ujjlenyomatát ne használd.
6. Data Access scope-ok a tényleges funkciók alapján: `openid`, `userinfo.email`, `userinfo.profile`, `drive.appdata`, `calendar.app.created`, `calendar.calendarlist.readonly`.
7. Fizikai készüléken külön ellenőrizd a Google-fiók kapcsolását, Drive-szinkront és Naptár-szinkront.

## Biztonsági modell

- Nincs repositoryba commitolt OAuth client secret.
- Nincs repositoryba commitolt signing kulcs.
- A Google hozzáférési token nem kerül JSON-backupba vagy JavaScript localStorage-ba.
- Release signing kizárólag külső keystore-ból / GitHub Secretsből történik.
- A repositoryba `google-services.json`, `.env`, JKS/keystore vagy más credential nem commitolható.

## Működés és korlátok

- A/B időponttervező: megadott kezdőnap, időpont, heti napok, időtartam és hetek alapján váltakozó alkalmak.
- Drive: az alkalmazás `appDataFolder` területét használja.
- Automatikus szinkron csak megnyitott alkalmazásban történik.
- A naptárszinkron TrainPilot → Google Calendar irányú; nem teljes kétirányú szinkron.
- Kijelentkezéskor az automatikus szinkron leáll, a helyi/felhőadat megmaradhat.

## Hivatalos dokumentáció

- Android Identity / Authorization
- Google Drive appDataFolder
- Google Calendar API

# TrainPilot – Google-beállítás

A Google-integráció forrása a projekt része, de a saját Google Cloud / OAuth konfigurációt külön kell létrehozni. Credential vagy kliens-titok nem kerülhet a repositoryba vagy az APK forrásába.

## Egyszeri beállítás

1. Nyisd meg a Google Cloud Console-t, és hozz létre egy saját TrainPilot projektet.
2. Engedélyezd a Google Drive API és Google Calendar API szolgáltatásokat.
3. Google Auth Platform → Branding: TrainPilot név, támogatási/kapcsolattartási e-mail és a szükséges publikus adatok.
4. Audience: a kiadási modellnek megfelelően állítsd be.
5. Clients → Create client → Android. Csomagnév: `com.repforge.app`. Az SHA-1 értéket az **adott telepített változat tényleges aláíró tanúsítványából** vedd: a meglévő APK és a Play App Signing tanúsítványa külön kliensbejegyzést igényelhet.
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

## 1.0.5: APK és Play külön aláírása

A publikált v1.0.4 APK ténylegesen az Android Debug tanúsítvánnyal van aláírva. A meglévő készülékes frissítésekhez az ujjlenyomata is szükséges az Android OAuth-kliensnél. A Playre telepített apphoz a **Play App Signing** tanúsítványa kell, nem csupán az AAB feltöltőkulcsáé. A fejlesztői Play-fiók és a Google Cloud OAuth-projekt külön beállítás.

A külön `Build Play upload bundle` workflow csak kézzel indítható, külön `PLAY_UPLOAD_*` titkokkal és a `TRAINPILOT_PRIVACY_POLICY_URL`, `TRAINPILOT_SUPPORT_EMAIL`, `TRAINPILOT_DEVELOPER_NAME` repository-változókkal. A Play build elutasítja a debug tanúsítványt. A részletes lista és a kötelező telefonos ellenőrzések: [PUBLICATION_1_0_5.md](PUBLICATION_1_0_5.md).

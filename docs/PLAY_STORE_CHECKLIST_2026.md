# TrainPilot – Google Play-előkészítés

Frissítve: **2026-10-20**. Követés: [#214](https://github.com/fuloplevente1998/TrainPilot/issues/214). Az előkészítés nem jelent Play-publikálást vagy teljesített Console-ellenőrzést.

## Mi áll rendelkezésre a repositoryban?

- [x] Telefon package: `com.repforge.app`; API 36 célzás a jelenlegi Gradle-forrásban.
- [x] Külön telefonos és Wear kiadási kapu, aláírt APK/forrás/checksum és signer-folytonosság ellenőrzése.
- [x] Legutóbb közzétett APK-pár: **1.3.6 /2733**, október 19. [Bizonylat](builds/2026-10-19-release-136.json).
- [x] Később elfogadott #222 Samsung/RIR alkalmazásforrás; külön [elfogadási bizonylat](builds/2026-10-20-222-accepted.json), változatlan verzió. Nem került visszamenőleg a régi APK-ba.
- [x] Health Connect/Samsung integráció, privát helyi egészségnapló; egészségadat-mentés külön hozzájárulással.
- [x] Privát fotótár, rendszer-képválasztó/kamera adatút, opcionális Drive `appDataFolder` és Naptár-integráció forrása.
- [x] Angol/magyar README, kiadásjegyzék és koordinált verzióellenőrzés a #214 karbantartási ágán. Beolvasztását a kapcsolódó PR jelzi.

Ezek forrás-/CI-bizonyítékok. A Console, Google OAuth és Samsung partneri fiók aktuális beállításait nem ellenőriztük; régi kipipált listából nem következtetünk mai jóváhagyásra.

## Áruházi megjelenés

- [ ] Végleges appnév, rövid és hosszú leírás, támogatott nyelvek.
- [ ] Áruházi ikon, feature graphic, valódi és jóváhagyott telefonos/órás képernyőképek. A README szintetikus Chromium-képei nem kész Play-anyagok.
- [ ] Működő támogatási email/URL és publikus homepage.
- [ ] Felülvizsgált, publikus adatvédelmi URL: a `www/privacy.html`, natív PrivacyPolicy és [korábbi tervezet](PRIVACY_POLICY_DRAFT.md) összehangolása a tényleges kiadással.
- [ ] Tartalombesorolás, célközönség, hirdetési és egyéb releváns Console-nyilatkozatok.

## Adatok és egészségfunkciók

- [ ] Adatleltár: helyi tárolás, fotó/útvonal/egészségadat, opcionális Drive/Naptár, adattovábbítás, megőrzés és törlés pontos leírása.
- [ ] Data safety űrlap kitöltése az alkalmazás és SDK-k tényleges működése alapján.
- [ ] Health apps declaration és a tényleges Health Connect jogosultságok/funkciók indoklása.
- [ ] Privacy/engedélyszövegek egyezése a megjelenő funkciókkal; engedélyhiány, visszavonás és adattörlés készülékes ellenőrzése.
- [ ] Samsung production/public partner-hozzáférés: `com.repforge.app`, tényleges terjesztési signer és ENERGY_SCORE scope; normál felhasználói próba.

A Samsung saját készülékes siker elfogadott funkcionális bizonyíték, de nem igazolja a production partner-regisztrációt. A hiányzó külső energiapontszámnál továbbra is a TrainPilot saját becslése marad.

## Aláírás, OAuth és terjesztés

- [ ] Play App Signing/upload key stratégia és a korábbi telepítések frissíthetőségének rögzítése; titkosított kulcsmentés ellenőrzése.
- [ ] A választott terjesztési formátum/AAB előállítása és ellenőrzése a végleges, jóváhagyott forrásból. A meglévő GitHub APK nem bizonyít Play-feltöltést.
- [ ] Production OAuth branding/audience/scopes, homepage/privacy és a tényleges Play signing SHA-1/SHA-256 regisztrálása.
- [ ] Play Console alkalmazásadatok, országok és megfelelő tesztsáv beállítása; a fiókra alkalmazandó aktuális tesztkövetelmények ellenőrzése.
- [ ] Play-tesztsávból történő telefon/Wear telepítés és frissítés, adatmegőrzés, Google-bejelentkezés/Drive/Naptár, Health/Samsung és engedély-visszavonás próbája.
- [ ] Végső forrás, verzió, aláírás, CI és készülékes elfogadás rögzítése.
- [ ] Külön felhasználói döntés a Play-publikálásról.

## Repository és licencelés

- [x] Kiadott binárisok és elfogadott main megkülönböztetése; eredeti tagek és mellékletek megőrzése.
- [x] Proprietary csomagjelölés: `private`, `UNLICENSED`; nincs hozzáadott nyílt forrású licenc.
- [ ] Végleges kereskedelmi/terjesztési feltételek és külső komponensek licencnyilvántartása.
- [ ] GitHub About rövid leírás/Topics felületi beállítása.

## Hivatalos kiindulópontok

- [Health apps declaration](https://support.google.com/googleplay/android-developer/answer/14738291?hl=en)
- [Data safety](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [Health Connect publikálás](https://developer.android.com/health-and-fitness/health-connect/publish)
- [Play tesztsávok](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en)
- [Samsung partneri folyamat](https://developer.samsung.com/health/data/process.html)

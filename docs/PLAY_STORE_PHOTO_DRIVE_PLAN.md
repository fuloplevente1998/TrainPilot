# TrainPilot – Workout Photos + Drive + Google Play terv

Cél: edzésenként opcionális fejlődési fotó csatolása a Naplóhoz, helyi privát tárolással és Google Drive appDataFolder szinkronnal, Google Play-kompatibilis minimális jogosultságokkal.

Tervezett működés:
- Naplóbejegyzésen belül „Fotó hozzáadása”.
- Forrás: Kamera vagy rendszer Photo Picker / dokumentumválasztó.
- Címke: Edzés előtt / Edzés után / Egyéb.
- Fotó az app saját privát tárhelyén, optimalizált JPEG-ként.
- A napló JSON csak metaadatot tárol; base64 képet nem.
- Google Drive: a meglévő `drive.appdata` OAuth scope használata, külön bináris JPEG-fájlokkal az `appDataFolder` területen.
- Másik eszközön a kép igény szerint letölthető a Drive-ból.
- A képek törölhetők az appból; a törlés Drive-on is továbbvezethető.
- Nem kérünk `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` vagy általános tárhely-hozzáférést.

Play Store előkészítés:
- Privacy policy: Health Connect, edzésnapló, testsúly, Google profil, Drive appDataFolder és felhasználó által kiválasztott fotók kezelése.
- Data Safety: a ténylegesen kezelt adatokkal összhangban kitöltve.
- Health apps declaration kitöltése.
- Health Connect jogosultságokhoz csak az app funkcióihoz szükséges adattípusok deklarálása.
- Fotóválasztás rendszer pickerrel, széles médiatár-jogosultság nélkül.
- OAuth production konfiguráció: branding, támogatási email, publikus home/privacy URL, szükséges scope-ok.

A fotók és a Google-szinkron valódi Android/Google-fiókos viselkedése csak fizikai készüléken tekinthető igazoltnak.
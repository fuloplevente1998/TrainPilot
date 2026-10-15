# TrainPilot Wear OS 1.3.5 — közös P1 kiadásjelölt

**1.3.5 /2731** — telefonos és Wear build azonos forráscommitból; nem nyilvánítjuk késznek sikeres aláírás-, natív teszt- és eszközkapu előtt.

Megőrzendő a [35b4108](https://github.com/fuloplevente1998/TrainPilot/commit/35b41086b6b08854a4b65c0d2e1dad5d6e5e7bec) valós készüléken visszaigazolt működése: GPS távolság/sebesség Health Services mérés, a központi helyszolgáltatás külön figyelmeztetése, telefonra küldött mérési statisztika.

A #163 telefonos útvonaltérképet a **telefon saját koordinátái** szolgálják ki; a Wear-only pontos útvonal / offline szakaszsync a külön [#193](https://github.com/fuloplevente1998/TrainPilot/issues/193) feladat marad.

Kötelező az 1.3.5-ös natív Wear release gate és a telefonos release gate, a signer folytonosságának ellenőrzése, majd a két APK közös valós készülékes próba.

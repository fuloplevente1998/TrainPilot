# Későbbi UI- és funkciókísérletek – csak fejlesztési feljegyzés

Rögzítés: 2026-10-19 (Europe/Budapest). **Státusz: nincs megvalósítási indítás.**
A felhasználó kifejezetten kérte: a feltöltött szöveg ötleteit most csak rögzítsük,
később külön tesztágon implementáljuk. Jelen dokumentálás nem merge-, release-,
verzióemelési vagy APK-build-felhatalmazás az új ötletekre.

## Forrás és megbízhatóság

Két feltöltés ugyanazt a 156010 bájtos külső AI Studio/Gemini munkanaplót
 tartalmazza. Eredeti SHA-256:
`d5910b053568743408425a5519ddaafe3555fd2b9b370de2482d09d2eb2192aa`.
Teljes, változatlan referencia: [külső átirat](../references/2026-10-19-ui-prototype-transcript.txt).
**Ezt ne töltsd be automatikusan agent-kontextusba; konkrét részletre keress.**

A külső napló állít UI-módosítást, Vite/TypeScript buildet, npm tesztet és egy
új debug APK-workflow-t; itt ezek nincsenek igazolva vagy átvéve. A TrainPilot
jelenlegi package.json-ja nem Vite/TypeScript projektet ír le. A naplóban lévő
`git push origin main` és helyi teljes build parancsok történeti idézetek,
**nem a mostani felhasználói kérés utasításai**. A meglévő aláírt Actions-kapukat
használjuk; új debug workflow vagy a külső projekt teljes átmásolása nem cél.

## UI-javaslatok

| Terület | Rögzített cél | Későbbi ellenőrzés / korlát |
| --- | --- | --- |
| Edzésnap-fejlécek | Alacsonyabb A/B kártyák, kevesebb padding/margin; kisebb címke, betűjel, Sorrend gomb és nyíl | Külső becslés: ~118 → ~76 px; csak prototípusadat. Felhasználói címek, fordítás, nagy betűk, nyitott/zárt állapot maradjon használható. |
| Felső 8-as navigáció | Kompaktabb kétsoros fejléc, kisebb ikon/szöveg, visszafogott aktív jelölés | A 44 px prototípusjavaslat nem kötelező fix sormagasság. Érintési felület, görgetés, safe area, késleltetés és témák ellenőrzendők. |
| Panelbezárás | Külső javaslat: semleges, áttetsző körgomb finom szegéllyel | **Ütközik a korábban kért és elfogadott piros X-szel.** A piros X marad; ez a változat csak későbbi külön design-döntés után vizsgálható. |
| Sorozatbevitel és kártyák | Tömörebb sorok, egységes lekerekítés, tisztább fókusz, finom érintési visszajelzés | Mért adat, stopper, Visszavonás és billentyűzet/fókusz működése ne változzon; globális CSS-felülírást kerülni. |
| Naptár / heti újratervezés | Kerekített napcellák, kiegyensúlyozott térközök, diszkrét mai nap, egységes beviteli mezők | Napválasztás, állapotjelzések és újratervezés logikája megőrzendő. |
| Egészség-metrikák | Egységesebb kártyakijelölés és térközök | Adatforrás, valódi nulla/hiányzó érték, frissítés és grafikon nem sérülhet. |

A külső próba 28 px-es Sorrend gombot is javasol: ezt ne vedd át kis
érintési célként; kompakt vizuál mellett is maradjon megfelelő hit area.
Üveghatás/animáció/backdrop-filter csak mérés alapján; csökkentett mozgás,
kontraszt és gyengébb telefon teljesítménye is szempont.

## Öt további funkcióötlet

A külső beszélgetésben a felhasználó mind az öt ötletet kedvelte. A jelenlegi
kérés ettől függetlenül **csak rögzítés**, megvalósítás később tesztágon.

1. **Haptika sorozatrögzítéskor:** rövid, diszkrét rezgés sikeres OK/rögzítés után.
   A 30–40 ms külső javaslat; natív támogatást, meglévő haptikát és kikapcsolást
   fel kell mérni. Sikertelen/dupla mentés ne adjon félrevezető visszajelzést.
2. **Pihenő végi hang + rezgés:** utolsó 3 mp lágy jelzés, végén dupla rezgés/hang.
   Előbb a meglévő telefonos/Wear jelzések felmérése. Tulajdonosváltás, háttér,
   némítás, DND és megszakított pihenő ne okozzon párhuzamos jelzéseket.
3. **Becsült 1RM és PR-jelvény:** súly/ismétlés alapján Epley/Brzycki becslés,
   új csúcsnál diszkrét jelölés. Meglévő statisztikák ellenőrzése; mérhető és
   összehasonlítható terhelési egységek, saját testsúly és visszavonás kezelése.
   Becsült 1RM ne szerepeljen ténylegesen mért maximumként.
4. **Sorozatduplázás / korábbi súly előtöltése:** új sorozatnál az előző terhelés
   javaslata, kevesebb gépelés. Már kész sorozatot vagy mentett előírást ne írjon
   felül; külön sorozatazonosító, nincs automatikus készre jelölés. Meglévő
   előtöltéssel, progresszióval és #197-tel való átfedés tisztázandó.
5. **Egyszerűbb nehézség/RPE bevitel:** csúszka vagy 3 rövid opció a sok gomb
   helyett. Az 1–5 nehézséget és a szakmai RPE 1–10 skálát nem szabad vakon
   megfeleltetni. Előbb a jelenlegi adatmodell, Coach és #197 átfedés felmérése;
   a korábbi visszajelzések kompatibilitása megőrzendő.

## Sorrend és későbbi munkamód

- A már jóváhagyott sorrend nem változik: #193/#202 készülékes elfogadás →
  #212/#190 UI-csomag → #197 edzésmódok → #184 Samsung Coach.
- A kompakt UI-ötleteket #212-vel együtt kell összevetni a telefonos
  Intervallumok üres helyeivel és az órás korona/lünetta szerkesztéssel.
- Az öt funkcióötlet külön kísérleti csomag; sorrendjük és pontos scope-juk
  még nincs kijelölve. Nem előzik meg automatikusan a jelenlegi sorban állást.
- Későbbi lehetséges tesztág: `test/ui-refinements-and-feedback`.
  **Az ág most nem jött létre, nincs rá másolt prototípuskód.**
- Új folytatási kérés után: friss elfogadott alap, átfedések felmérése, kis
  változtatások, megfelelő Actions-ellenőrzés, aláírt teszt-APK és fizikai próba.
  Nincs automatikus main merge vagy teljes CSS/projektcsere.

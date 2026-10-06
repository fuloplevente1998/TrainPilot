# Adaptív navigáció – #127

Kiindulási alap: a felhasználó által jóváhagyott `b11a112c62e4520398cc8b2190a2c9f92837c840` ikonos build. A launcher ikon és a meglévő témák változatlanok.

## Működés

| Nézet | Alapértelmezés | Választható hely | Oldalműveletek |
| --- | --- | --- | --- |
| Álló | Alul, 4 × 2 gomb | Fent / alul | A főmenü mellett, a tartalom felőli oldalon |
| Fekvő | Jobbra, 2 × 4 gomb | Balra / jobbra | A főmenü melletti oldalsó sávban |

A nyíl mindkét nézethez külön megjegyzi a választást. Mind a nyolc főoldal felirattal elérhető. A kis ikonok az issue-ban kiválasztott Lucide ikonok; licencük a `www/LUCIDE-LICENSE.txt` fájlban található.

Alsó elrendezésben a főmenü fölött vannak a helyi műveletek, a lenyitott tartalom pedig a fejléc fölé kerül. Az összetartozó adatsorok sorrendje és az ujjmozdulatos görgetés nem fordul meg. A rövid kijelzőn túl magas tartalom görgethető marad.

Az átrendezés a meglévő DOM-elemeket használja: az elforgatás és a nyíl nem hívja az app újrarajzolását. A keresők, program- és gyakorlatkészítők, naplófülek, naptár, újratervezés, edzésvezérlők és személyes tervező az aktuális tartalomterülethez igazodnak. A builder fejlécét és bezárógombját panel-frissítés előtt visszahelyezzük a panelbe, hogy annak HTML-frissítése ne törölhesse őket. Az űrlapból áthelyezett mentésgomb megtartja a `form` kapcsolatát.

A billentyűzet által csökkentett nézet nem vált automatikusan fekvő menüre: gépeléskor a főmenü eltűnik, a műveletek az űrlap normál folyásában elérhetők. A rendszerterületeket CSS safe-area értékek veszik figyelembe.

## Automatizált ellenőrzés

- `npm test`: a meglévő funkcionális regressziós ellenőrzések.
- `npm run test:ui`: a teljes Chromium UI-csomag, az új `adaptive-navigation-127.cjs` teszttel együtt.
- Az új teszt ellenőrzi mind a négy menühelyet, a kapcsolódó műveletsávot, a 320–412 CSS-pixeles álló nézeteket, a fekvő szűrők nyitását, a menühely újraindítás utáni megőrzését, az űrlap és edzés állapotát, a panel-visszalépést és fókuszt, valamint a billentyűzet okozta nézetcsökkenést.
- Az elfogadott korábbi UI-tesztekben a felső menüre és a gombok korábbi közvetlen szülőjére vonatkozó elvárások az új dokkolást követik. Hosszú tartalomnál a korábbi kötelező görgetésmentesség helyett az elérhetőséget ellenőrizzük.
- A CI a valódi git-előzményekkel futtatja a történeti stílusparitás-tesztet is, majd csak sikeres UI-ellenőrzés után készít aláírt APK-t.

## Androidos elfogadás

1. Álló nézetben válts a nyíllal alulról felülre, majd vissza; nyisd meg a Programok, Napló, Naptár és Egészség oldalakat.
2. Fordítsd fekvőre, és válts jobbról balra. A helyi gombok és keresők kövessék a főmenüt; a tartalom maradjon elérhető.
3. Írj be egy mentetlen gyakorlatnevet vagy programértéket, majd fordítsd el a telefont. A beírás és a lenyitott rész maradjon meg.
4. Ellenőrizd a valódi Android-billentyűzet megnyitását, elrejtését és a keresési eredmények elérését; a rendszer gesztussávját és kijelzőkivágását is.
5. Aktív edzés és pihenőidőzítő mellett fordítsd el a készüléket, majd ellenőrizd a gombokat, sorozatadatokat és időzítőket.
6. Nyiss panelt, fotót és megerősítést; ellenőrizd a bezárást és az Android-visszalépést. Indítsd újra az appot: a menühelyek maradjanak meg.

A böngészős billentyűzetellenőrzés szimuláció; a készüléken történő elfogadás szükséges a merge előtt. A #127 elfogadott és merge-ölt állapota lesz a #128 kiindulási alapja.

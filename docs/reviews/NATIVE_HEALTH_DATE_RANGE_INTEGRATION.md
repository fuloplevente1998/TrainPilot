# PR243 – többnapos Health-record böngésző integráció

2026-10-25. Külső ág test/native-health-date-range, exact
**1bf745e02c60f6aa83ede08519167f72d773f844**, pinned bázis9a18ca6.8 engedélyezett
fájl, nem változott provider/séma/HealthJournalStore/verzió/main. Exact delta
konfliktus nélkül squash-commitban a migrációban: **1864a8c**. Nem GitHub-merge;
a külső draft #243 és ága nyitva/változatlan az integrált futtatás/elfogadásig.

- HealthRecordQuery végére endDay=day került; régi nevesített/pozicionális
  konstruktorok megmaradnak. fromDay/toDay a valós Store inkluzív range-ét adja.
-25 rekord/lap; forrás/csatorna/típus, restore/generation guard és rekord/alvás
  részletek maradnak. withRange/withFilters null cursor/generationnel indul.
- Valódi Android DatePickerDialog, két határ együttes Apply, fordított tartomány
  nem alkalmazható. Query/reload-revision/cancellation guard nem enged régi
  IO-választ új queryhez; lapelőzmények/hiba/cache együtt törlődnek.

**c611cc7 integrációs javítások:** a teszt a natív execution DB/snapshotot is
reseteli a többi UI-fixture mintájára, és megvárja az első rekordbetöltést az
assert előtt. A picker Activity window/theme mellett az app nyelvét kapja,
Bezárás/nyelv-/contextváltáskor dismiss;48dp vezérlők és dangerText hibaszín.

7 új JVM+1 UI-metódus kód, **NEM FUTOTT**. Külső és saját XML/key/diff/lexical/
hívásiút-ellenőrzés nem Kotlin/Room/Compose/platform-futás. A package-szűréses
meglévő phone-ui-gate az új tesztosztályt is felveszi, új workflow nincs.
Teljes közös CI, kis kijelző/nagy betű és tényleges eszköz továbbra hátra.

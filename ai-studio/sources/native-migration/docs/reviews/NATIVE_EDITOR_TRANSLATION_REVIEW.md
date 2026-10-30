# Két külső nyelvi audit – integrációs döntések

2026-10-25. Bázisuk18038924674c7fcfc7f648054d5830ccbf011018;8 XML/148 string.
A felhasználó a „Natív szerkesztők…” című, TrainPilot-előtag nélküli riportot
Argonként azonosította, a másikat Flashként. Eredeti szövegek (CRLF→LF
normalizálással): NATIVE_EDITOR_TRANSLATION_REVIEW_ARGON.md és
NATIVE_EDITOR_TRANSLATION_REVIEW_FLASH.md. Gépi javaslatok, nem végrehajtási
felhatalmazás vagy fordítási/UI-futási bizonyíték.

**2ff85ad:**23 meglévő felirat pontosítva,6 XML-fájlban. A két forma20/17 kulcsa
négy nyelven és az egyetlen %1$s paraméter változatlan. Egységes mp/s jelölések,
DE RIR = Wiederholungen in Reserve, HU kézisúlyzó súlya, célérték-hiba HU/RO
pontosítás, mentési hiba mind a négy nyelven egyértelműen közben történt
módosításra utal. EN stale szöveg explicit time window. Kulcs/API/működés nem változott.

Nem vettük át vakon az összes megállapítást:

- Az időzóna-eltolások egyes számra cserélése nem megfelelő általánosítás:
  kezdés és befejezés eltérő UTC-eltolással is megadható, például DST-váltásnál.
  EN többes számot és mindkét dátum/idő ellenőrzését tart, RO mindkét mezőt jelzi.
- Az eredeti HU birtokos „az időzóna eltolása” nem bizonyított helyesírási hiba;
  a német mintára való átírás nem kötelező, a segédszöveg maradt.
- Az Apply gombok rövidítéséről csak tényleges kis képernyős/nagybetűs próba
  után döntünk. A child Apply nem adatbázis-mentés, a megkülönböztetés megmarad.
- Az „azonnal fordítható”/„100%” állítások statikus nyelvi auditból nem következnek.
  Saját ellenőrzés: XML/kulcs/paraméter és pontos meglévő szöveg egyezés.
  Gradle, Compose, fordító és emulátor nem futott; vizuális levágás nem reprodukált.

A két jelentés átfedő találatait egyszer javítottuk. Nincs külön issue-lezárás,
main merge, verzióemelés vagy új készülékes elfogadás.

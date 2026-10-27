# AI Studio – kis, lépésenként betölthető átadó

A teljes offline ZIP archívum. Ne töltsd egyben modellkontextusba: az eredeti
beszélgetésexport, a teljes régi és új forrás és a tesztnaplók együtt túl nagyok.
A ZIP darabolása sem csökkentené az összesített tokenigényt.

1. **Először csak [01_START_HERE.md](01_START_HERE.md)** – 35 346 UTF-8 bájt.
   Ez tartalmazza a teljes átadó 1–14. pontját, a friss korlátokat,
   a megmaradt munkát, a piros kapu állapotát és a célzott RAW-linkeket.
2. Dizájntervezéshez [02_DESIGN_REFERENCE.md](02_DESIGN_REFERENCE.md) –
   139 777 bájt, plusz az aktuális oldalhoz szükséges mockup képként.
3. Home/Training kidolgozásához [03_HOME_TRAINING_SOURCE.md](03_HOME_TRAINING_SOURCE.md) –
   56 525 bájt, tíz teljes, változatlan Kotlin-fájl. A motor és a teljes projekt
   nincs ebben a csomagban. Más munkához más célzott forrás kell.

Ne töltsd fel mindhárom fájlt automatikusan. A harmadik fájl nem tartalmazza a
három hibás UI-teszt reprodukciójához szükséges teljes forrást vagy logokat.
RAW-link megadása nem jelenti azt, hogy az AI Studio ténylegesen beolvasta:
ha az adott környezet nem fér hozzá, a konkrét fájlt külön kell bemásolni/feltölteni.
Nem állítunk kipróbált AI Studio-feltöltést vagy pontos tokenszámot;
a méretek a tényleges UTF-8 fájlméretek, a tokenigény modellfüggő.

A források a `1509686e` snapshotból származnak (appkód: `67f2889f`).
Csak dokumentációs csomagolás változott; main, verzió és alkalmazásforrás változatlan.

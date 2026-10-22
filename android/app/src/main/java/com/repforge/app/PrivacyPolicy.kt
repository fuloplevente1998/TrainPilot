package __PACKAGE_NAME__

/**
 * Helyi adatvédelmi és tájékoztató szövegek (Health Connect & Google Drive integráció).
 */
object PrivacyPolicy {
    fun text(language: String?): String {
        return when (language?.lowercase()) {
            "hu" -> """
                Adatkezelési Tájékoztató – TrainPilot

                1. Adattárolás és Személyes Adatok
                A TrainPilot egy helyi-fókuszú (local-first) edzéskövető alkalmazás. Minden rögzített edzésnapló, gyakorlat, sorozat, testsúlymérés és fotó közvetlenül a készülék belső titkosított tárhelyén kerül tárolásra a Room adatbázisban.

                2. Health Connect és Egészségügyi Adatok
                Az alkalmazás kizárólag kifejezett hozzájárulásod esetén kapcsolódik az Android Health Connect rendszerhez vagy a Samsung Health platformhoz. A beolvasott pulzus-, alvás- és kalóriaadatok csak a regenerációs állapot és edzéselemzés számítására szolgálnak a készüléken, harmadik fél részére nem kerülnek továbbításra.

                3. Google Drive Felhőszinkronizáció
                A Google Drive szinkronizáció kizárólag az alkalmazás elszigetelt alkalmazásmappáját (appDataFolder) használja. Az alkalmazás nem fér hozzá semmilyen más privát fájlodhoz a Drive tárhelyeden.

                4. Harmadik Fél és Reklámok
                Az alkalmazás nem tartalmaz harmadik féltől származó reklámkövetőket, analitikát vagy adatbrókerekkel való megosztást.
            """.trimIndent()

            "de" -> """
                Datenschutzerklärung – TrainPilot

                1. Datenspeicherung und persönliche Daten
                TrainPilot ist eine lokale Trainings-Tracking-App. Alle Trainingsdaten, Übungen, Sätze, Körpergewichtsmessungen und Fotos werden direkt auf dem internen Speicher Ihres Geräts gespeichert.

                2. Health Connect und Gesundheitsdaten
                Die App greift nur mit Ihrer ausdrücklichen Zustimmung auf Android Health Connect oder Samsung Health zu. Herzfrequenz-, Schlaf- und Kaloriendaten werden ausschließlich lokal zur Berechnung Ihres Erholungsstatus verwendet.

                3. Google Drive Cloud-Synchronisierung
                Die Google Drive-Synchronisierung nutzt ausschließlich den isolierten Anwendungsordner (appDataFolder). Die App hat keinen Zugriff auf andere persönliche Dateien.

                4. Keine Drittanbieter oder Werbung
                TrainPilot enthält keine Werbe-Tracker oder Analysedienste von Drittanbietern.
            """.trimIndent()

            "ro" -> """
                Politica de Confidențialitate – TrainPilot

                1. Stocarea Datelor și Informații Personale
                TrainPilot este o aplicație centrată pe stocarea locală. Toate jurnalele de antrenament, exercițiile, greutatea corporală și fotografiile sunt stocate direct pe dispozitivul dvs.

                2. Health Connect și Date de Sănătate
                Aplicația accesează Health Connect sau Samsung Health numai cu permisiunea dvs. explicită. Ritmul cardiac, somnul și caloriile sunt utilizate doar local pentru calcularea scorului de recuperare.

                3. Sincronizare Google Drive
                Sincronizarea utilizează doar dosarul izolat al aplicației (appDataFolder) din Google Drive. Aplicația nu accesează alte fișiere private.

                4. Fără Reclame sau Urmărire
                TrainPilot nu conține module de urmărire publicitară sau analiză terță.
            """.trimIndent()

            else -> """
                Privacy Policy – TrainPilot

                1. Data Storage and Personal Information
                TrainPilot is a local-first workout tracker. All logged workouts, exercises, body weight records, and photos are saved directly on your device's internal storage in a secure Room database.

                2. Health Connect and Health Data
                The application connects to Android Health Connect or Samsung Health only with your explicit permission. Retrieved heart rate, sleep, and calorie data are used strictly on-device to evaluate your recovery status.

                3. Google Drive Cloud Synchronization
                Google Drive sync exclusively accesses the isolated application folder (appDataFolder). The app cannot access any other personal files on your Drive.

                4. No Third-Party Trackers or Ads
                TrainPilot contains no third-party advertising trackers, data brokers, or telemetry services.
            """.trimIndent()
        }
    }
}

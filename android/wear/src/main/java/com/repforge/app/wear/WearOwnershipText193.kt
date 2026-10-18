package com.repforge.app.wear

import java.util.Locale

internal object WearOwnershipText193 {
    private val words=mapOf(
        "hu" to mapOf("pausedTitle" to "Edzés szünetel","pausedDetail" to "A mérés és az időzítő szünetel.","home" to "Kezdőlap","title" to "Edzés vezérlése","phone" to "A telefon vezérli az edzést","watch" to "Az óra vezérli az edzést",
            "pending" to "Az átadás folyamatban. Az edzés szünetel; a folytatáshoz megvárjuk a visszaigazolást.",
            "request" to "Edzés átvétele","retry" to "Átadás újrapróbálása","resume" to "Folytatás az órán","back" to "‹ Vissza",
            "openPhone" to "Telefon megnyitása","connected" to "Az átvételhez csatlakoztasd a telefont, és tartsd nyitva a telefonos edzést.",
            "ready" to "Az átvett edzés szünetel. Innen az órán, telefon nélkül is folytatható.",
            "return" to "A visszaadást a telefonon az Edzés visszavétele gombbal kérheted.",
            "pendingCommands" to "Várd meg az adatok telefonos visszaigazolását, majd próbáld újra.",
            "requestSent" to "Az átvételi kérés az órán mentve. Tartsd megnyitva a telefonos edzést.","failed" to "Az átvétel nem menthető. Próbáld újra."),
        "en" to mapOf("pausedTitle" to "Workout paused","pausedDetail" to "Measurement and timer are paused.","home" to "Home","title" to "Workout control","phone" to "Phone controls the workout","watch" to "Watch controls the workout",
            "pending" to "Transfer in progress. The workout stays paused until the transfer is confirmed.",
            "request" to "Take over workout","retry" to "Retry transfer","resume" to "Continue on watch","back" to "‹ Back",
            "openPhone" to "Open phone","connected" to "Connect your phone and keep its workout open to transfer.",
            "ready" to "The transferred workout is paused. You can continue here without your phone.",
            "return" to "To return control, use Take back workout on your phone.",
            "pendingCommands" to "Wait for phone confirmation of your changes, then retry.",
            "requestSent" to "Transfer request saved on watch. Keep the phone workout open.","failed" to "Could not save the transfer. Please retry."),
        "de" to mapOf("pausedTitle" to "Training pausiert","pausedDetail" to "Messung und Timer pausieren.","home" to "Startseite","title" to "Trainingssteuerung","phone" to "Das Telefon steuert das Training","watch" to "Die Uhr steuert das Training",
            "pending" to "Übertragung läuft. Das Training bleibt bis zur Bestätigung pausiert.",
            "request" to "Training übernehmen","retry" to "Übertragung wiederholen","resume" to "Auf Uhr fortsetzen","back" to "‹ Zurück",
            "openPhone" to "Telefon öffnen","connected" to "Verbinde das Telefon und halte sein Training zur Übernahme geöffnet.",
            "ready" to "Das übernommene Training pausiert. Du kannst es hier ohne Telefon fortsetzen.",
            "return" to "Fordere die Rückgabe mit Training zurücknehmen auf dem Telefon an.",
            "pendingCommands" to "Warte auf die Bestätigung der Änderungen durch das Telefon und versuche es erneut.",
            "requestSent" to "Anfrage auf der Uhr gespeichert. Training am Telefon geöffnet lassen.","failed" to "Übernahme konnte nicht gespeichert werden. Erneut versuchen."),
        "ro" to mapOf("pausedTitle" to "Antrenament în pauză","pausedDetail" to "Măsurarea și cronometrul sunt în pauză.","home" to "Acasă","title" to "Control antrenament","phone" to "Telefonul controlează antrenamentul","watch" to "Ceasul controlează antrenamentul",
            "pending" to "Transfer în curs. Antrenamentul rămâne în pauză până la confirmare.",
            "request" to "Preia antrenamentul","retry" to "Reîncearcă transferul","resume" to "Continuă pe ceas","back" to "‹ Înapoi",
            "openPhone" to "Deschide telefonul","connected" to "Conectează telefonul și păstrează antrenamentul deschis pentru transfer.",
            "ready" to "Antrenamentul preluat este în pauză. Îl poți continua aici fără telefon.",
            "return" to "Pentru a reveni la telefon, folosește Preia înapoi antrenamentul pe telefon.",
            "pendingCommands" to "Așteaptă confirmarea modificărilor pe telefon, apoi reîncearcă.",
            "requestSent" to "Cererea a fost salvată pe ceas. Păstrează antrenamentul deschis pe telefon.","failed" to "Preluarea nu a putut fi salvată. Reîncearcă.")
    )
    fun text(key: String,language: String=Locale.getDefault().language)=requireNotNull((words[language] ?: words.getValue("en"))[key])
}

package com.repforge.app.wear

import android.content.Context

internal object WearMenu216 {
    private val outdoor = setOf("running", "jogging", "walking", "hiking", "cycling", "inline-skating")
    fun eligible(id: String, distance: Boolean) = distance && id in outdoor
    fun eligible(ex: WearExercise?) = ex != null && eligible(ex.id, WearGps193.distance(ex))
    fun eligible(ex: WatchHomeExercise?) = ex != null && eligible(ex.id, ex.measurementType == "distance" || ex.repUnit in listOf("km", "m"))
    private fun prefs(context: Context) = context.getSharedPreferences("trainpilot_wear_gps_choice216", Context.MODE_PRIVATE)
    fun enabled(context: Context, id: String) = id in outdoor && prefs(context).getBoolean(id, false)
    fun choose(context: Context, id: String, enabled: Boolean): Boolean = id in outdoor && prefs(context).edit().putBoolean(id, enabled).commit()
    fun entries(active: Boolean, quick: Boolean): List<Pair<String,String>> = buildList {
        if (active) add("resume" to "workout")
        if (!active || quick) add((if (quick) "add" else "quick") to "quick")
        addAll(listOf("planned" to "planned216", "journal" to "journal", "metrics" to "metrics", "settings" to "settings", "home" to "home"))
    }
    private val words = mapOf(
        "hu" to listOf("Főmenü","Edzés folytatása","Gyors edzés","+ Gyakorlat","Tervezett edzések","Napló","Órás mérések","Beállítások","Kezdőlap","Naptár","Programnapok","Megnyitás telefonon","Edzés törlése","Edzés indítása","Mégse","GPS-útvonal","Be","Ki","Megjegyezzük ehhez a mozgáshoz. Csak kifejezett edzésindításkor kapcsol be.","A táv/sebesség mérése önmagában nem jelent GPS-útvonalat.","Következő indításkor","Vissza","GPS-pontok az órán és a párosított telefonon; felhőmentés nélkül.","Pihenő"),
        "en" to listOf("Main menu","Continue workout","Quick workout","+ Exercise","Planned workouts","Journal","Watch measurements","Settings","Home","Calendar","Program days","Open on phone","Discard workout","Start workout","Cancel","GPS route","On","Off","Remembered for this activity. Starts only when you explicitly start a workout.","Distance/speed measurements alone do not record a GPS route.","On next start","Back","GPS points stay on your watch and paired phone, outside cloud backups.","Rest"),
        "de" to listOf("Hauptmenü","Training fortsetzen","Schnelltraining","+ Übung","Geplante Trainings","Protokoll","Uhrenmessungen","Einstellungen","Startseite","Kalender","Programmtage","Am Telefon öffnen","Training verwerfen","Training starten","Abbrechen","GPS-Route","Ein","Aus","Für diese Aktivität gespeichert. Startet nur beim ausdrücklichen Trainingsstart.","Distanz-/Geschwindigkeitsmessung allein zeichnet keine GPS-Route auf.","Beim nächsten Start","Zurück","GPS-Punkte auf Uhr und gekoppeltem Telefon, ohne Cloud-Sicherung.","Pause"),
        "ro" to listOf("Meniu principal","Continuă antrenamentul","Antrenament rapid","+ Exercițiu","Antrenamente planificate","Jurnal","Măsurători pe ceas","Setări","Acasă","Calendar","Zile de program","Deschide pe telefon","Renunță la antrenament","Începe antrenamentul","Anulează","Traseu GPS","Pornit","Oprit","Memorat pentru această activitate. Pornește doar la începerea explicită a antrenamentului.","Distanța/viteza măsurată nu înseamnă înregistrarea unui traseu GPS.","La următoarea pornire","Înapoi","Puncte GPS pe ceas și telefonul asociat, fără copii cloud.","Pauză")
    )
    private val keys = listOf("menu","resume","quick","add","planned","journal","metrics","settings","home","calendar","days","phone","discard","start","cancel","gps","on","off","remember","aggregate","next","back","consent","rest")
    fun text(key: String, language: String = java.util.Locale.getDefault().language): String =
        (words[language] ?: words.getValue("en"))[keys.indexOf(key).also { require(it >= 0) }]
}

package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Synthetic, non-personal data shared by the old-APK seed and native assertions. */
internal object UpgradeFixture {
    const val PHOTO_ID = "00000000-0000-0000-0000-000000000123"
    const val ROUTE_KEY = "migration-route"
    val notes = "Árvíztűrő 🏋️ \"idézet\"\n".repeat(2400)
    val routeText = "1\t47.5000000\t19.0000000\t5.0\t0\t1792659600000\n1\t47.5001000\t19.0001000\t5.0\t1000\t1792659601000\n"
    fun digest(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
    fun values(): Map<String, String> {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context
        val fixture = context.assets.open("legacy-v1371.json").bufferedReader().use { JSONObject(it.readText()) }
        val storage = fixture.getJSONObject("storage")
        return storage.keys().asSequence().associateWith { storage.getString(it) }
    }
}

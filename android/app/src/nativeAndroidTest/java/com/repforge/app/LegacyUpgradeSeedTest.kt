package com.repforge.app

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Runs ONLY against the installed original release. No native migration class references. */
@RunWith(AndroidJUnit4::class)
class LegacyUpgradeSeedTest {
    private fun findWebView(view: View): WebView? = if (view is WebView) view else
        if (view is ViewGroup) (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) } else null
    private fun evaluate(view: WebView, script: String): String {
        val done = CountDownLatch(1); var result = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync { view.evaluateJavascript(script) { result = it; done.countDown() } }
        check(done.await(30, TimeUnit.SECONDS)); return result
    }
    @Test fun seedOriginalSignedAppAndBackup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(Intent().setClassName("com.repforge.app", "com.repforge.app.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            var view: WebView? = null
            instrumentation.runOnMainSync { view = findWebView(activity.window.decorView) }
            val web = requireNotNull(view) { "Original release must contain its actual WebView" }
            val end = System.currentTimeMillis() + 30000
            while (evaluate(web, "Boolean(window.TrainPilotBoot?.finished)") != "true") {
                check(System.currentTimeMillis() < end); Thread.sleep(100)
            }
            val source = JSONObject(UpgradeFixture.values() as Map<*, *>).toString()
            assertEquals("true", evaluate(web, "(()=>{const values=$source;localStorage.setItem('repforge:history','[]');localStorage.setItem('repforge:obsolete','deleted');localStorage.removeItem('repforge:obsolete');for(const [k,v] of Object.entries(values))localStorage.setItem(k,v);return true;})()"))
            val backup = File(context.noBackupFilesDir, "pre-native-fixture-backup.json")
            backup.writeText(source)
            File(context.noBackupFilesDir, "pre-native-fixture-backup.sha256").writeText(UpgradeFixture.digest(source))
            val gps = File(context.noBackupFilesDir, "gps_routes"); check(gps.isDirectory || gps.mkdirs())
            File(gps, UpgradeFixture.digest(UpgradeFixture.ROUTE_KEY) + ".route").writeText(UpgradeFixture.routeText)
            val photos = File(context.filesDir, "workout_photos"); check(photos.isDirectory || photos.mkdirs())
            File(photos, "${UpgradeFixture.PHOTO_ID}.jpg").outputStream().use { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 85, it) }
            // Initialize through the actual old plugin, without relying on its R8 class names.
            evaluate(web, "window.__seedHealthReady=false;(async()=>{const p=Capacitor.Plugins.HealthJournal||Capacitor.registerPlugin('HealthJournal');await p.initialize();window.__seedHealthReady=true;})();true")
            val healthEnd = System.currentTimeMillis() + 30000
            while (evaluate(web, "window.__seedHealthReady===true") != "true") {
                check(System.currentTimeMillis() < healthEnd); Thread.sleep(100)
            }
            SQLiteDatabase.openDatabase(context.getDatabasePath("health-journal.db").path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("INSERT OR REPLACE INTO meta(key,value) VALUES('migration-fixture','retained-health-data')")
            }
            Thread.sleep(1000) // Let Chromium commit its localStorage write before the package update.

        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
}

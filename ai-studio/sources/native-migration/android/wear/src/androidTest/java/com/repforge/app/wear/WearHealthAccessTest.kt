package com.repforge.app.wear

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real platform permission plus a disabled/enabled watch-wide setting, not a Compose fixture. */
@RunWith(AndroidJUnit4::class)
class WearHealthAccessTest {
    @Test fun grantedPrecisePermissionAndDisabledSystemLocationAreReportedSeparately() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val automation=instrumentation.uiAutomation
        // This emulator is disposable. Revoking our own permission after the test
        // would kill the instrumentation process, so leave the runtime grant intact.
        automation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_COARSE_LOCATION)
        automation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_FINE_LOCATION)
        val original=context.getSystemService(LocationManager::class.java).isLocationEnabled
        fun systemLocation(enabled: Boolean) {
            ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(
                "cmd location set-location-enabled $enabled")).bufferedReader().use { it.readText() }
            val deadline=android.os.SystemClock.elapsedRealtime()+5000
            while(context.getSystemService(LocationManager::class.java).isLocationEnabled!=enabled &&
                android.os.SystemClock.elapsedRealtime()<deadline)Thread.sleep(50)
            assertEquals(enabled,context.getSystemService(LocationManager::class.java).isLocationEnabled)
        }
        try {
            systemLocation(false)
            assertEquals(PackageManager.PERMISSION_GRANTED,context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION))
            val blocked=wearRecordingAccess(context,false,true)
            assertTrue(blocked.fineLocationGranted);assertFalse(blocked.systemLocationEnabled)
            assertTrue(blocked.locationBlocked);assertFalse(blocked.locationPermissionBlocked)
            assertFalse(blocked.foregroundLocation)
            val summary=JSONObject().put("state","error").put("distanceMeters",7.0)
            wearApplyLocationFailure(summary,blocked,"Missing permissions: [android.permission.ACCESS_FINE_LOCATION]")
            assertEquals(WEAR_LOCATION_SERVICES_MESSAGE,summary.getString("message"))
            assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS,wearLocationSettingsIntent().action)
            systemLocation(true)
            val ready=wearRecordingAccess(context,false,true)
            assertFalse(ready.locationBlocked);assertTrue(ready.foregroundLocation);assertFalse(ready.gpsEnabled)
            val retry=wearRetryRecording(summary)!!
            assertEquals("starting",retry.getString("state"));assertEquals(7.0,retry.getDouble("distanceMeters"),0.001)
            assertFalse(wearHasLocationFailure(retry))
        } finally { systemLocation(original) }
    }
}

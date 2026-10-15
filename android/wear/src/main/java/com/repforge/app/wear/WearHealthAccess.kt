package com.repforge.app.wear

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings

/** App permission and the watch-wide location switch are independent requirements. */
internal fun wearRecordingAccess(context: Context, gps: Boolean, providerRequiresLocation: Boolean) =
    WearRecordingAccess(gps, providerRequiresLocation,
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED,
        context.getSystemService(LocationManager::class.java)?.isLocationEnabled==true)

internal fun wearLocationSettingsIntent() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

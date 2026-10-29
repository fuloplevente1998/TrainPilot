package com.repforge.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Real merged-manifest entry points and missing/partial permission sets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeHealthPermissionEntryTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test fun platformPrivacyEntryResolvesToExistingPolicyWithSystemGate() {
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, context.packageName + ".ViewHealthPermissionUsageActivity"), 0)
        assertTrue(info.exported)
        assertEquals("android.permission.START_VIEW_PERMISSION_USAGE", info.permission)
        assertTrue(info.targetActivity.endsWith(".HealthPrivacyActivity"))
        val resolved = context.packageManager.resolveActivity(
            Intent("android.intent.action.VIEW_PERMISSION_USAGE")
                .addCategory("android.intent.category.HEALTH_PERMISSIONS")
                .setPackage(context.packageName), 0)
        assertNotNull(resolved)
    }

    @Test fun rationaleEntryResolvesToReadOnlyPrivacyActivity() {
        val resolved = context.packageManager.resolveActivity(
            Intent("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE")
                .setPackage(context.packageName), 0)
        assertNotNull(resolved)
        assertTrue(resolved!!.activityInfo.exported)
        assertTrue(resolved.activityInfo.name.endsWith(".HealthPrivacyActivity"))
    }

    @Test fun partiallyGrantedReadAccessStillRequestsEveryMissingType() {
        val permissions = HealthConnectRepository.ALL_RUNTIME_PERMISSIONS
        shadowOf(RuntimeEnvironment.getApplication<android.app.Application>()).denyPermissions(*permissions)
        assertEquals(permissions.toSet(), HealthConnectRepository.missingReadPermissions(context).toSet())
        shadowOf(RuntimeEnvironment.getApplication<android.app.Application>()).grantPermissions(permissions.first())
        assertEquals(permissions.drop(1).toSet(), HealthConnectRepository.missingReadPermissions(context).toSet())
        shadowOf(RuntimeEnvironment.getApplication<android.app.Application>()).grantPermissions(*permissions)
        assertTrue(HealthConnectRepository.missingReadPermissions(context).isEmpty())
    }
}

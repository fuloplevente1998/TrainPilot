package com.repforge.app

import com.repforge.app.ui.health.BackgroundBlock
import com.repforge.app.ui.health.NativeHealthBackgroundPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class NativeHealthBackgroundPolicyTest {
    @Test fun healthReadyNeedsAllThreeIndependentCapabilities() {
        assertEquals(BackgroundBlock.READY, NativeHealthBackgroundPolicy.health(true, true, true, true))
    }

    @Test fun healthWithoutSdkSupportNeverReportsEnabledCapability() {
        assertEquals(BackgroundBlock.UNSUPPORTED, NativeHealthBackgroundPolicy.health(false, true, true, true))
    }

    @Test fun healthMissingReadPermissionIsNotAWorkingSync() {
        assertEquals(BackgroundBlock.READ_PERMISSION, NativeHealthBackgroundPolicy.health(true, false, true, true))
    }

    @Test fun healthBackgroundPermissionIsSeparateFromForegroundRead() {
        assertEquals(BackgroundBlock.BACKGROUND_PERMISSION, NativeHealthBackgroundPolicy.health(true, true, false, true))
    }

    @Test fun pendingRestoreBlocksEvenOtherwisePermittedBackgroundReads() {
        assertEquals(BackgroundBlock.RESTORE, NativeHealthBackgroundPolicy.health(true, true, true, false))
        assertEquals(BackgroundBlock.RESTORE, NativeHealthBackgroundPolicy.ble(true, true, true, true, false))
    }

    @Test fun bleRequiresAnActualRememberedRdfitDevice() {
        assertEquals(BackgroundBlock.WATCH_NOT_READY, NativeHealthBackgroundPolicy.ble(false, true, true, true, true))
    }

    @Test fun bleConnectPermissionAndNotificationPermissionAreNotInterchangeable() {
        assertEquals(BackgroundBlock.BLUETOOTH_PERMISSION, NativeHealthBackgroundPolicy.ble(true, false, true, true, true))
        assertEquals(BackgroundBlock.NOTIFICATION_PERMISSION, NativeHealthBackgroundPolicy.ble(true, true, false, true, true))
    }

    @Test fun bleDisabledSystemRadioCannotBeTreatedAsRunningService() {
        assertEquals(BackgroundBlock.BLUETOOTH_OFF, NativeHealthBackgroundPolicy.ble(true, true, true, false, true))
    }

    @Test fun bleReadyDoesNotClaimTheGattConnectionHasCompleted() {
        assertEquals(BackgroundBlock.READY, NativeHealthBackgroundPolicy.ble(true, true, true, true, true))
    }
}

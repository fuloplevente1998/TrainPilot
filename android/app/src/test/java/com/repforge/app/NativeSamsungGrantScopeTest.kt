package com.repforge.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real SDK permission objects; no provider or physical measurement success claimed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeSamsungGrantScopeTest {
    @Test fun allRequestedPermissionsMapToTheCanonicalStatusKeys() {
        val reader = SamsungSdkReader.Reader(null, SamsungSdkReader.permissions(), Runnable {})
        assertEquals(SamsungHealthRepository.PERMISSION_KEYS.toSet(), reader.grantedKeys())
    }
    @Test fun energyOnlyScopeDoesNotInventActivityOrBodyPermissions() {
        val reader = SamsungSdkReader.Reader(null, SamsungSdkReader.energyPermission(), Runnable {})
        assertEquals(setOf("READ_ENERGY_SCORE"), reader.grantedKeys())
    }
    @Test fun missingGrantsRemainAnEmptyScope() {
        val reader = SamsungSdkReader.Reader(null, emptySet(), Runnable {})
        assertTrue(reader.grantedKeys().isEmpty())
    }
}

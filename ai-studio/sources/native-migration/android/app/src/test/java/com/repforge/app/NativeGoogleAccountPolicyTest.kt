package com.repforge.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class NativeGoogleAccountPolicyTest {
    private fun profile(sub: Any = "subject-a", email: Any = "a@example.invalid") =
        JSONObject().put("sub", sub).put("email", email)

    @Test fun disconnectedAndPartiallyPersistedAccountsCannotSync() {
        for ((sub, email) in listOf("" to "", "subject-a" to "", "" to "a@example.invalid", " " to "mail"))
            assertThrows(IllegalStateException::class.java) {
                NativeGoogleAccountPolicy.requireMatchingProfile(sub, email, profile(), connecting = false)
            }
    }
    @Test fun lateTokenFromAnotherAccountCannotReadOrDeleteConnectedAccountData() {
        assertThrows(IOException::class.java) {
            NativeGoogleAccountPolicy.requireMatchingProfile("subject-b", "b@example.invalid", profile(), false)
        }
    }
    @Test fun connectDoesNotSilentlySwitchAnExistingStableSubject() {
        assertThrows(IOException::class.java) {
            NativeGoogleAccountPolicy.requireMatchingProfile("subject-b", "b@example.invalid", profile(), true)
        }
    }
    @Test fun explicitConnectCanEstablishTheFirstAccount() {
        NativeGoogleAccountPolicy.requireMatchingProfile("", "", profile(), true)
    }
    @Test fun renamedEmailWithTheSameGoogleSubjectKeepsCloudOwnership() {
        NativeGoogleAccountPolicy.requireMatchingProfile("subject-a", "old@example.invalid", profile(), false)
    }
    @Test fun malformedVerifiedProfileNeverBecomesAnAccountIdentity() {
        for (invalid in listOf(profile(" "), profile(email = ""), profile(123), profile(email = false), JSONObject()))
            assertThrows(IOException::class.java) {
                NativeGoogleAccountPolicy.requireMatchingProfile("", "", invalid, true)
            }
    }
}

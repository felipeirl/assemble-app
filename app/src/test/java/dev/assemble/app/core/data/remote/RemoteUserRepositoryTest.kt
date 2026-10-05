package dev.assemble.app.core.data.remote

import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RemoteUserRepositoryTest {
    private val store = InMemoryUserDataStore()
    private val api = FakeAssembleApi()

    private fun repository(auth: FakeAuthGateway, scope: CoroutineScope) =
        RemoteUserRepository(auth, store, api, inMemorySettings(), scope)

    @Test
    fun signedOut_isReadyAndLoggedOut() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = null), scope)
        users.ready.first { it }
        assertFalse(users.session.value.isLoggedIn)
    }

    @Test
    fun newUser_isLoggedInWithoutOnboarding_andNamedFromEmail() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = "uid-1"), scope)
        users.awaitKnown()
        assertTrue(users.session.value.isLoggedIn)
        assertFalse(users.session.value.hasCompletedOnboarding)
        assertEquals("tony", users.currentProfile.value.name)
    }

    @Test
    fun completeOnboarding_writesPreferencesConsentAndCreatedAt() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = "uid-1"), scope)
        users.awaitKnown()
        val preferences = Preferences.Any.copy(origins = setOf(Origin.Mutant))

        users.completeOnboarding(preferences)

        val written = store.merges.single()
        assertEquals(ServerTime, written["onboardingCompletedAt"])
        assertEquals(ServerTime, written["createdAt"])
        assertEquals(mapOf("acceptedAt" to ServerTime, "version" to AI_CONSENT_VERSION), written["aiConsent"])
        assertTrue(users.session.value.hasCompletedOnboarding)
        assertEquals(preferences, users.preferences.value)
    }

    @Test
    fun logInWithEmail_failurePropagatesReason() = runRemoteTest { scope ->
        val auth = FakeAuthGateway(signedInUid = null).apply { failure = AuthFailure.InvalidCredentials }
        val users = repository(auth, scope)
        try {
            users.logInWithEmail("a@b.c", "secret1", createAccount = false)
            fail("esperava AuthException")
        } catch (error: AuthException) {
            assertEquals(AuthFailure.InvalidCredentials, error.reason)
        }
    }

    @Test
    fun deactivation_fromDocumentOrApi_andReactivate() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = "uid-1"), scope)
        users.awaitKnown()
        assertFalse(users.accountDeactivated.value)

        users.onAccountDeactivated()
        assertTrue(users.accountDeactivated.value)
        users.reactivateAccount()
        assertFalse(users.accountDeactivated.value)
        assertEquals(1, api.reactivateCalls)

        store.user.value = mapOf("status" to USER_STATUS_DEACTIVATED)
        assertTrue(users.accountDeactivated.value)
    }

    @Test
    fun deleteAccount_deactivatesOnServerAndSignsOut() = runRemoteTest { scope ->
        val auth = FakeAuthGateway(signedInUid = "uid-1")
        val users = repository(auth, scope)
        users.awaitKnown()

        users.deleteAccount()

        assertEquals(1, api.deactivateCalls)
        assertEquals(0, api.hideChatsCalls)
        assertFalse(users.session.value.isLoggedIn)
        assertTrue(users.accountDeletionHandledByServer)
    }
}

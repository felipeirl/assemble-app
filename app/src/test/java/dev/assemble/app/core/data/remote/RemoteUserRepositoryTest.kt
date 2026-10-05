package dev.assemble.app.core.data.remote

import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.UserProfile
import dev.assemble.app.core.network.ApiPhotoSignature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class RemoteUserRepositoryTest {
    private val store = InMemoryUserDataStore()
    private val api = FakeAssembleApi()

    private val uploaded = mutableListOf<String>()
    private var uploadResult: () -> String = { throw IOException("sem rede") }

    private fun repository(auth: FakeAuthGateway, scope: CoroutineScope, webClientId: String? = null) =
        RemoteUserRepository(
            auth, store, api, inMemorySettings(), scope, webClientId,
            photoUploader = { jpeg, _ -> uploaded += jpeg; uploadResult() },
        )

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
    fun completeOnboarding_writesLookingForOnlyWhenAnswered() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = "uid-1"), scope)
        users.awaitKnown()

        users.completeOnboarding(Preferences.Any, lookingFor = "  Falar de ciência ${"x".repeat(LOOKING_FOR_MAX)}")

        val written = store.merges.single()["lookingFor"] as String
        assertTrue(written.startsWith("Falar de ciência"))
        assertEquals(LOOKING_FOR_MAX, written.length)
    }

    @Test
    fun completeOnboarding_withoutLookingFor_doesNotWriteIt() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = "uid-1"), scope)
        users.awaitKnown()

        users.completeOnboarding(Preferences.Any, lookingFor = "   ")

        assertFalse("lookingFor" in store.merges.single())
    }

    private val signature = ApiPhotoSignature("https://api.cloudinary.com/v1_1/demo/image/upload", mapOf("signature" to "abc"))
    private val hostedUrl = "https://res.cloudinary.com/demo/image/upload/v1/assemble/avatars/uid-1.jpg"

    @Test
    fun updateProfile_uploadsANewPhotoAndStoresTheUrl() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = "uid-1"), scope)
        users.awaitKnown()
        api.onPhotoSignature = { signature }
        uploadResult = { hostedUrl }

        users.updateProfile(users.currentProfile.value.copy(photo = "QkFTRTY0"))

        assertEquals(listOf("QkFTRTY0"), uploaded)
        assertEquals(hostedUrl, store.merges.last()["avatarPhoto"])
    }

    @Test
    fun updateProfile_keepsTheBase64WhenCloudinaryIsNotConfigured() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = "uid-1"), scope)
        users.awaitKnown()

        users.updateProfile(users.currentProfile.value.copy(photo = "QkFTRTY0"))

        assertEquals(1, api.photoSignatureCalls)
        assertEquals(emptyList<String>(), uploaded)
        assertEquals("QkFTRTY0", store.merges.last()["avatarPhoto"])
    }

    @Test
    fun updateProfile_keepsTheBase64WhenTheUploadFails() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = "uid-1"), scope)
        users.awaitKnown()
        api.onPhotoSignature = { signature }

        users.updateProfile(users.currentProfile.value.copy(photo = "QkFTRTY0"))

        assertEquals("QkFTRTY0", store.merges.last()["avatarPhoto"])
    }

    @Test
    fun updateProfile_doesNotUploadAPhotoThatIsAlreadyAUrl() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = "uid-1"), scope)
        users.awaitKnown()

        users.updateProfile(users.currentProfile.value.copy(photo = hostedUrl))

        assertEquals(0, api.photoSignatureCalls)
        assertEquals(hostedUrl, store.merges.last()["avatarPhoto"])
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

    @Test
    fun googleLogin_isOfferedOnlyWithAWebClientId() = runRemoteTest { scope ->
        assertFalse(repository(FakeAuthGateway(), scope).googleLogin)
        assertFalse(repository(FakeAuthGateway(), scope, webClientId = " ").googleLogin)
        val users = repository(FakeAuthGateway(), scope, webClientId = "123.apps.googleusercontent.com")
        assertTrue(users.googleLogin)
        assertEquals("123.apps.googleusercontent.com", users.googleWebClientId)
    }

    @Test
    fun logInWithGoogleToken_signsInThroughTheGateway() = runRemoteTest { scope ->
        val auth = FakeAuthGateway(signedInUid = null)
        val users = repository(auth, scope, webClientId = "id")

        users.logInWithGoogleToken("google-id-token")

        assertEquals("google-id-token", auth.lastGoogleToken)
        assertEquals(FakeAuthGateway.SIGNED_IN_UID, auth.uid.value)
    }

    @Test
    fun updateProfile_savesAndRemovesThePhoto() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = "uid-1"), scope)
        users.awaitKnown()

        users.updateProfile(UserProfile("Tony", "", 0, photo = "AAAA"))
        assertEquals("AAAA", users.currentProfile.value.photo)

        users.updateProfile(UserProfile("Tony", "", 0, photo = null))
        assertEquals(null, users.currentProfile.value.photo)
        assertEquals(DeleteField, store.merges.last()["avatarPhoto"])
    }

    @Test
    fun updateProfile_withoutAnyPhoto_doesNotTouchThePhotoField() = runRemoteTest { scope ->
        val users = repository(FakeAuthGateway(signedInUid = "uid-1"), scope)
        users.awaitKnown()

        users.updateProfile(UserProfile("Tony", "", 0))

        assertFalse(store.merges.last().containsKey("avatarPhoto"))
    }

    @Test
    fun sendPasswordReset_delegatesToTheGateway() = runRemoteTest { scope ->
        val auth = FakeAuthGateway(signedInUid = null)
        repository(auth, scope).sendPasswordReset("tony@stark.com")
        assertEquals(listOf("tony@stark.com"), auth.resetRequests)
    }
}

package dev.assemble.app.core.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import dev.assemble.app.core.data.remote.AuthException
import dev.assemble.app.core.data.remote.AuthFailure
import dev.assemble.app.core.data.remote.AuthGateway
import dev.assemble.app.core.network.NotSignedInException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Login por e-mail e senha no Firebase Auth. */
class FirebaseAuthGateway(private val auth: FirebaseAuth) : AuthGateway {
    private val uidState = MutableStateFlow(auth.currentUser?.uid)
    private val readyState = MutableStateFlow(false)

    override val uid: StateFlow<String?> = uidState.asStateFlow()
    override val ready: StateFlow<Boolean> = readyState.asStateFlow()
    override val email: String? get() = auth.currentUser?.email

    private val pendingState = MutableStateFlow(isPending(auth.currentUser))
    override val emailVerificationPending: StateFlow<Boolean> = pendingState.asStateFlow()

    init {
        auth.addAuthStateListener { current ->
            uidState.value = current.currentUser?.uid
            pendingState.value = isPending(current.currentUser)
            readyState.value = true
        }
    }

    override suspend fun idToken(forceRefresh: Boolean): String {
        val user = auth.currentUser ?: throw NotSignedInException()
        return user.getIdToken(forceRefresh).awaitAuth().token ?: throw NotSignedInException()
    }

    override suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).awaitAuth()
    }

    override suspend fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email.trim(), password).awaitAuth()
    }

    override suspend fun signInWithGoogle(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).awaitAuth()
    }

    override suspend fun sendPasswordReset(email: String) {
        try {
            auth.sendPasswordResetEmail(email.trim()).awaitAuth()
        } catch (error: AuthException) {
            // Conta inexistente não pode ser distinguida de sucesso (enumeração de e-mails).
            if (error.reason != AuthFailure.InvalidCredentials) throw error
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun reloadEmailVerified(): Boolean {
        val user = auth.currentUser ?: throw NotSignedInException()
        user.reload().awaitAuth()
        // O backend lê "e-mail confirmado" do token: renova para a próxima chamada já passar.
        user.getIdToken(true).awaitAuth()
        pendingState.value = isPending(user)
        return !pendingState.value
    }

    override suspend fun sendFirebaseVerificationEmail() {
        val user = auth.currentUser ?: throw NotSignedInException()
        user.sendEmailVerification().awaitAuth()
    }

    /** Só o login por e-mail e senha precisa confirmar o e-mail; o do Google já vem confirmado. */
    private fun isPending(user: FirebaseUser?): Boolean =
        user != null && !user.isEmailVerified && user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitAuth(): T = try {
        await()
    } catch (error: FirebaseAuthWeakPasswordException) {
        throw AuthException(AuthFailure.WeakPassword, error)
    } catch (error: FirebaseAuthUserCollisionException) {
        throw AuthException(AuthFailure.EmailInUse, error)
    } catch (error: FirebaseAuthInvalidUserException) {
        throw AuthException(AuthFailure.InvalidCredentials, error)
    } catch (error: FirebaseAuthInvalidCredentialsException) {
        val reason = if (error.errorCode == "ERROR_INVALID_EMAIL") AuthFailure.InvalidEmail else AuthFailure.InvalidCredentials
        throw AuthException(reason, error)
    } catch (error: FirebaseNetworkException) {
        throw AuthException(AuthFailure.Network, error)
    } catch (error: com.google.firebase.FirebaseException) {
        throw AuthException(AuthFailure.Other, error)
    }
}

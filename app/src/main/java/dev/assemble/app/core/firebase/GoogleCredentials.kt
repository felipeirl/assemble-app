package dev.assemble.app.core.firebase

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dev.assemble.app.core.data.remote.AuthException
import dev.assemble.app.core.data.remote.AuthFailure

/**
 * Abre o seletor de contas do Google e devolve o ID token para o Firebase Auth. [context] precisa
 * ser a Activity (o seletor é uma tela). Cancelar vira [AuthFailure.Cancelled], sem mensagem de erro.
 */
suspend fun requestGoogleIdToken(context: Context, webClientId: String): String {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
        .build()
    val credential = try {
        CredentialManager.create(context).getCredential(context, request).credential
    } catch (error: GetCredentialCancellationException) {
        throw AuthException(AuthFailure.Cancelled, error)
    } catch (error: GetCredentialException) {
        throw AuthException(AuthFailure.Other, error)
    }
    if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        throw AuthException(AuthFailure.Other)
    }
    return try {
        GoogleIdTokenCredential.createFrom(credential.data).idToken
    } catch (error: GoogleIdTokenParsingException) {
        throw AuthException(AuthFailure.Other, error)
    }
}

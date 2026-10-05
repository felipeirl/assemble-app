package dev.assemble.app.feature.login

import dev.assemble.app.core.data.remote.AuthException
import dev.assemble.app.core.data.remote.AuthFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LoginErrorTest {

    @Test
    fun loginError_mapsAuthFailures() {
        assertEquals(LoginError.InvalidCredentials, loginError(AuthException(AuthFailure.InvalidCredentials)))
        assertEquals(LoginError.EmailInUse, loginError(AuthException(AuthFailure.EmailInUse)))
        assertEquals(LoginError.WeakPassword, loginError(AuthException(AuthFailure.WeakPassword)))
        assertEquals(LoginError.InvalidEmail, loginError(AuthException(AuthFailure.InvalidEmail)))
        assertEquals(LoginError.Generic, loginError(AuthException(AuthFailure.Network)))
        assertEquals(LoginError.Generic, loginError(IOException("offline")))
    }

    @Test
    fun canSubmitEmail_requiresEmailAndMinimumPassword() {
        assertFalse(LoginUiState(email = "tony", password = "123456").canSubmitEmail)
        assertFalse(LoginUiState(email = "tony@stark.com", password = "12345").canSubmitEmail)
        assertTrue(LoginUiState(email = " tony@stark.com ", password = "123456").canSubmitEmail)
    }
}

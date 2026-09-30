package dev.assemble.app.data.local;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Sessão via Firebase Auth anônimo (contrato V1 §3).
 * Identidade = Firebase UID ativo; chamadas ao backend levam o ID token no Bearer.
 * Falhas de autenticação são explícitas ({@link AuthException}), nunca silenciosas.
 */
public class SessionStore {
    private final Context appContext;

    public SessionStore(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /** Garante login anônimo; resolve com o usuário atual. */
    public Task<FirebaseUser> ensureAnonymous() {
        try {
            FirebaseInit.requireAvailable(appContext);
        } catch (IllegalStateException e) {
            return Tasks.forException(new AuthException("auth/unavailable", e.getMessage(), e));
        }
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            return Tasks.forResult(current);
        }
        return auth.signInAnonymously().continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                throw new AuthException("auth/sign-in-failed",
                        task.getException() != null ? task.getException().getMessage() : "signInAnonymously falhou");
            }
            AuthResult result = task.getResult();
            if (result.getUser() == null) {
                throw new AuthException("auth/no-user", "signInAnonymously sem usuário");
            }
            return result.getUser();
        });
    }

    /** UID atual ou exceção explícita se não autenticado. */
    public String requireUid() throws AuthException {
        try {
            FirebaseInit.requireAvailable(appContext);
        } catch (IllegalStateException e) {
            throw new AuthException("auth/unavailable", e.getMessage(), e);
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid() == null) {
            throw new AuthException("auth/no-user", "sem usuário autenticado; chame ensureAnonymous()");
        }
        return user.getUid();
    }

    /** ID token para o header Authorization: Bearer. Falha explícita em erro de rede/auth. */
    public Task<String> idToken(boolean forceRefresh) {
        try {
            FirebaseInit.requireAvailable(appContext);
        } catch (IllegalStateException e) {
            return Tasks.forException(new AuthException("auth/unavailable", e.getMessage(), e));
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Tasks.forException(new AuthException("auth/no-user", "sem usuário autenticado"));
        }
        return user.getIdToken(forceRefresh).continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                throw new AuthException("auth/token-failed",
                        task.getException() != null ? task.getException().getMessage() : "getIdToken falhou");
            }
            String token = task.getResult().getToken();
            if (token == null || token.isEmpty()) {
                throw new AuthException("auth/empty-token", "ID token vazio");
            }
            return token;
        });
    }
}

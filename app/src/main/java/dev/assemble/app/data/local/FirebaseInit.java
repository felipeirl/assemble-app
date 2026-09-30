package dev.assemble.app.data.local;

import android.content.Context;

import com.google.firebase.FirebaseApp;

import java.util.List;

/**
 * Inicialização manual do Firebase (sem plugin google-services).
 *
 * <p>DEV: sem google-services.json real, {@link #isAvailable()} retorna false e
 * todo acesso lança {@link IllegalStateException} com mensagem explícita
 * (stub DEV identificado — nunca dados inventados).
 */
public final class FirebaseInit {
    public static final String DEV_NO_FIREBASE =
            "DEV_NO_FIREBASE: google-services.json ausente; Firebase indisponível. "
            + "Ver docs/setup-firebase.md (usuário fornece o arquivo depois).";

    private static volatile Boolean available;

    private FirebaseInit() {}

    public static synchronized void init(Context context) {
        if (available != null) {
            return;
        }
        boolean ok = false;
        try {
            FirebaseApp.initializeApp(context.getApplicationContext());
            List<FirebaseApp> apps = FirebaseApp.getApps(context.getApplicationContext());
            ok = apps != null && !apps.isEmpty();
        } catch (Exception ignored) {
            ok = false;
        }
        available = ok;
    }

    public static boolean isAvailable(Context context) {
        if (available == null) {
            init(context);
        }
        return Boolean.TRUE.equals(available);
    }

    public static void requireAvailable(Context context) {
        if (!isAvailable(context)) {
            throw new IllegalStateException(DEV_NO_FIREBASE);
        }
    }
}

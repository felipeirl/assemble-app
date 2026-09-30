package dev.assemble.app.data.repository;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import dev.assemble.app.data.local.FirestoreClient;
import dev.assemble.app.data.model.UserProfile;

/**
 * Perfil users/{uid} (contrato V1 §2). Leitura/escrita do próprio perfil;
 * prefs ausentes = null (nunca inventar).
 */
public class ProfileRepository {
    private final FirestoreClient store;

    public ProfileRepository(FirestoreClient store) {
        this.store = store;
    }

    public void load(String uid, Callback<UserProfile> callback) {
        store.getProfile(uid).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onError(task.getException() != null
                        ? toException(task.getException())
                        : new IllegalStateException("firestore/get-profile-failed"));
                return;
            }
            callback.onSuccess(toProfile(task.getResult()));
        });
    }

    public void save(String uid, UserProfile profile, Callback<Void> callback) {
        store.saveProfile(uid, profile != null ? profile.toMap() : new HashMap<>())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onError(toException(task.getException()));
                    } else {
                        callback.onSuccess(null);
                    }
                });
    }

    static UserProfile toProfile(DocumentSnapshot doc) {
        UserProfile p = new UserProfile();
        if (doc == null || !doc.exists()) {
            return p;
        }
        p.displayName = doc.getString("displayName");
        Map<String, Object> prefs = (Map<String, Object>) doc.get("prefs");
        if (prefs != null) {
            Map<String, Integer> out = new HashMap<>();
            for (Map.Entry<String, Object> e : prefs.entrySet()) {
                if (e.getValue() instanceof Number) {
                    out.put(e.getKey(), ((Number) e.getValue()).intValue());
                }
            }
            p.prefs = out.isEmpty() ? null : out;
        }
        return p;
    }

    private static Exception toException(Exception e) {
        return e != null ? e : new IllegalStateException("firestore/op-failed");
    }
}

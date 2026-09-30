package dev.assemble.app.data.local;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Paths Firestore do contrato V1 §2 (sempre sob o UID ativo):
 * users/{uid}, users/{uid}/decisions/{characterId},
 * users/{uid}/matches/{characterId}, .../messages/{messageId}.
 */
public final class FirestorePaths {
    private FirestorePaths() {}

    public static DocumentReference user(FirebaseFirestore db, String uid) {
        return db.collection("users").document(uid);
    }

    public static CollectionReference decisions(FirebaseFirestore db, String uid) {
        return user(db, uid).collection("decisions");
    }

    public static DocumentReference decision(FirebaseFirestore db, String uid, String characterId) {
        return decisions(db, uid).document(characterId);
    }

    public static CollectionReference matches(FirebaseFirestore db, String uid) {
        return user(db, uid).collection("matches");
    }

    public static DocumentReference match(FirebaseFirestore db, String uid, String characterId) {
        return matches(db, uid).document(characterId);
    }

    public static CollectionReference messages(FirebaseFirestore db, String uid, String characterId) {
        return match(db, uid, characterId).collection("messages");
    }
}

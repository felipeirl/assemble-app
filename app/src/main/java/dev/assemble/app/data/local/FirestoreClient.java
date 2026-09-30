package dev.assemble.app.data.local;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente Firestore (contrato V1 §2).
 *
 * <p>ESCRITAS PERMITIDAS: perfil users/{uid} e decisões users/{uid}/decisions/{characterId}
 * — decisões SOMENTE {choice, updatedAt} (adendo §5 2026-09-30). Score, threshold,
 * algorithmVersion e connected NUNCA saem do cliente (rules negam; backend/Admin SDK
 * é a única fonte autoritativa; exibição usa DecisionResultCache em memória).
 *
 * <p>SEM ESCRITA: matches e messages são criados só pelo backend (Admin SDK);
 * o cliente apenas lê/observa. Cliente NUNCA cria conexão aceita nem resposta de IA.
 *
 * <p>SEM Firebase real: todos os métodos falham com {@link IllegalStateException}
 * (DEV_NO_FIREBASE) — stub DEV identificado, sem dados falsos.
 *
 * <p>Listeners: o chamador (ViewModel) deve guardar o {@link ListenerRegistration}
 * e chamar remove() no ciclo de vida (onCleared/stop). Sem listeners duplicados:
 * um registro por tela, removido antes de recriar.
 */
public class FirestoreClient {
    private final Context appContext;

    public FirestoreClient(Context context) {
        this.appContext = context.getApplicationContext();
    }

    private FirebaseFirestore db() {
        FirebaseInit.requireAvailable(appContext);
        return FirebaseFirestore.getInstance();
    }

    // ---- Perfil ----

    public Task<DocumentSnapshot> getProfile(String uid) {
        try {
            return FirestorePaths.user(db(), uid).get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    public Task<Void> saveProfile(String uid, Map<String, Object> profile) {
        try {
            return FirestorePaths.user(db(), uid).set(profile);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    // ---- Decisões (choice-only, §5) ----

    /**
     * Grava SÓ {choice, updatedAt} (allowlist das rules). Idempotente por
     * construção: doc id = characterId (repetir sobrescreve, nunca duplica
     * conexão). NÃO cria match nem abre conversa (contrato §3 PENDENTE).
     */
    public Task<Void> saveDecisionChoice(String uid, String characterId, String choice, long updatedAt) {
        if (!"PASS".equals(choice) && !"ASSEMBLE".equals(choice)) {
            return Tasks.forException(new IllegalArgumentException("choice deve ser PASS|ASSEMBLE"));
        }
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("choice", choice);
            doc.put("updatedAt", updatedAt);
            return FirestorePaths.decision(db(), uid, characterId).set(doc);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * @deprecated Proibido por §5: cliente nunca persiste campo autoritativo.
     * Mantido p/ compatibilidade; lança {@link IllegalStateException} se o map
     * contiver score/threshold/algorithmVersion/connected. Prefira
     * {@link #saveDecisionChoice(String, String, String, long)}.
     */
    @Deprecated
    public Task<Void> saveDecision(String uid, String characterId, Map<String, Object> decision) {
        if (decision != null
                && (decision.containsKey("score")
                    || decision.containsKey("threshold")
                    || decision.containsKey("algorithmVersion")
                    || decision.containsKey("connected"))) {
            throw new IllegalStateException(
                    "saveDecision(map): campo autoritativo proibido no cliente (§5); use saveDecisionChoice");
        }
        try {
            return FirestorePaths.decision(db(), uid, characterId).set(decision);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    public Task<QuerySnapshot> getDecisions(String uid) {
        try {
            return FirestorePaths.decisions(db(), uid).get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    public ListenerRegistration observeDecisions(String uid, EventListener<QuerySnapshot> listener) {
        return FirestorePaths.decisions(db(), uid).addSnapshotListener(listener);
    }

    // ---- Conexões e mensagens: SOMENTE LEITURA (backend cria) ----

    public Task<DocumentSnapshot> getMatch(String uid, String characterId) {
        try {
            return FirestorePaths.match(db(), uid, characterId).get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    public ListenerRegistration observeMatches(String uid, EventListener<QuerySnapshot> listener) {
        return FirestorePaths.matches(db(), uid).addSnapshotListener(listener);
    }

    public ListenerRegistration observeMessages(String uid, String characterId,
            EventListener<QuerySnapshot> listener) {
        Query query = FirestorePaths.messages(db(), uid, characterId).orderBy("createdAt");
        return query.addSnapshotListener(listener);
    }
}

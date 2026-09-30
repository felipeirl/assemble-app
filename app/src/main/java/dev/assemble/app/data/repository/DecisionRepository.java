package dev.assemble.app.data.repository;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dev.assemble.app.data.local.FirestoreClient;
import dev.assemble.app.data.local.SessionStore;
import dev.assemble.app.data.model.Decision;
import dev.assemble.app.data.remote.BackendHttpClient;
import dev.assemble.app.data.remote.ContractDto;

/**
 * Decisões (contrato V1 §1-§2 + adendo de segurança 2026-09-30, contrato §5).
 *
 * <p>CLIENTE → FIRESTORE: grava SÓ {choice, updatedAt} via
 * {@link #saveDecisionChoice} (allowlist das rules — score/threshold/
 * algorithmVersion/connected vindos do backend NUNCA são persistidos).
 * O resultado do POST /decisions vai para {@link DecisionResultCache}
 * (só exibição em memória da sessão; nunca fonte autoritativa).
 *
 * <p>Idempotente: doc id = characterId (repetir sobrescreve, nunca duplica conexão).
 * PENDENTE contrato §3: sem abrir conversa automática, sem criar match no cliente.
 * PASS só remove da fila (lógica de fila fica na UI/Vetor; aqui só persiste).
 */
public class DecisionRepository {
    private final BackendHttpClient http;
    private final SessionStore session;
    private final FirestoreClient store;
    private final DecisionResultCache cache = new DecisionResultCache();
    private final Executor io;

    public DecisionRepository(BackendHttpClient http, SessionStore session, FirestoreClient store) {
        this(http, session, store, Executors.newCachedThreadPool());
    }

    DecisionRepository(BackendHttpClient http, SessionStore session, FirestoreClient store, Executor io) {
        this.http = http;
        this.session = session;
        this.store = store;
        this.io = io;
    }

    public void submit(String characterId, String choice, Callback<Decision> callback) {
        if (!"PASS".equals(choice) && !"ASSEMBLE".equals(choice)) {
            callback.onError(new IllegalArgumentException("choice deve ser PASS|ASSEMBLE"));
            return;
        }
        session.idToken(false).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                callback.onError(tokenTask.getException() != null
                        ? toException(tokenTask.getException())
                        : new IllegalStateException("auth/token-failed"));
                return;
            }
            String token = tokenTask.getResult();
            io.execute(() -> {
                try {
                    String json = http.post("/decisions",
                            ContractDto.decisionBody(characterId, choice), token);
                    Decision decision = ContractDto.parseDecision(json);
                    decision.updatedAt = System.currentTimeMillis();
                    String uid = session.requireUid();
                    // Só exibição em memória — campo autoritativo nunca sai do cliente.
                    cache.put(characterId, decision);
                    // Persiste SÓ choice (rules negariam qualquer outra key).
                    store.saveDecisionChoice(uid, characterId, choice, decision.updatedAt)
                            .addOnCompleteListener(saveTask -> {
                                if (!saveTask.isSuccessful()) {
                                    callback.onError(toException(saveTask.getException()));
                                } else {
                                    // Sem transição score→conexão/chat: só persiste e retorna.
                                    callback.onSuccess(decision);
                                }
                            });
                } catch (Exception e) {
                    callback.onError(e);
                }
            });
        });
    }

    /** Escrita direta choice-only (sem POST). Score NUNCA incluído — ver §5. */
    public void saveDecisionChoice(String uid, String characterId, String choice,
            Callback<Void> callback) {
        if (!"PASS".equals(choice) && !"ASSEMBLE".equals(choice)) {
            callback.onError(new IllegalArgumentException("choice deve ser PASS|ASSEMBLE"));
            return;
        }
        store.saveDecisionChoice(uid, characterId, choice, System.currentTimeMillis())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onError(toException(task.getException()));
                    } else {
                        callback.onSuccess(null);
                    }
                });
    }

    /** Último resultado do POST p/ exibição (memória; null se ausente). */
    public Decision getCachedResult(String characterId) {
        return cache.get(characterId);
    }

    /** Evict no logout/troca de UID (display não atravessa sessão). */
    public void clearCache() {
        cache.clear();
    }

    /** Observa decisões do UID (listener; ViewModel remove no onCleared). */
    public com.google.firebase.firestore.ListenerRegistration observe(String uid,
            EventListener<QuerySnapshot> listener) {
        return store.observeDecisions(uid, listener);
    }

    private static Exception toException(Exception e) {
        return e != null ? e : new IllegalStateException("firestore/op-failed");
    }
}

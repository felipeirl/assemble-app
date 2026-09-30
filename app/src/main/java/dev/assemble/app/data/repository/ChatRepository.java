package dev.assemble.app.data.repository;

import android.net.Uri;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dev.assemble.app.data.local.FirestoreClient;
import dev.assemble.app.data.local.SessionStore;
import dev.assemble.app.data.model.ChatMessage;
import dev.assemble.app.data.remote.BackendHttpClient;
import dev.assemble.app.data.remote.ContractDto;

/**
 * Chat (contrato V1 §1-§2): envia texto do usuário via
 * POST /matches/{characterId}/messages; a resposta de IA é gravada pelo backend
 * em seguida — o cliente NUNCA cria resposta de IA, só observa messages/ via listener.
 */
public class ChatRepository {
    private final BackendHttpClient http;
    private final SessionStore session;
    private final FirestoreClient store;
    private final Executor io;

    public ChatRepository(BackendHttpClient http, SessionStore session, FirestoreClient store) {
        this(http, session, store, Executors.newCachedThreadPool());
    }

    ChatRepository(BackendHttpClient http, SessionStore session, FirestoreClient store, Executor io) {
        this.http = http;
        this.session = session;
        this.store = store;
        this.io = io;
    }

    public void send(String characterId, String text, Callback<ChatMessage> callback) {
        if (text == null || text.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("text vazio"));
            return;
        }
        session.idToken(false).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                callback.onError(tokenTask.getException() != null
                        ? tokenTask.getException()
                        : new IllegalStateException("auth/token-failed"));
                return;
            }
            String token = tokenTask.getResult();
            io.execute(() -> {
                try {
                    String json = http.post("/matches/" + Uri.encode(characterId) + "/messages",
                            ContractDto.messageBody(text), token);
                    callback.onSuccess(ContractDto.parseSentMessage(json));
                } catch (Exception e) {
                    callback.onError(e);
                }
            });
        });
    }

    /** Listener de messages/ ordenado por createdAt; remover no ciclo de vida. */
    public ListenerRegistration observe(String uid, String characterId,
            EventListener<QuerySnapshot> listener) {
        return store.observeMessages(uid, characterId, listener);
    }

    /** Mapeia snapshot → mensagens (role user|character, fictional marca ficção). */
    public static List<ChatMessage> toMessages(QuerySnapshot snapshot) {
        List<ChatMessage> out = new ArrayList<>();
        if (snapshot == null) {
            return out;
        }
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            ChatMessage m = new ChatMessage();
            m.messageId = doc.getId();
            m.role = doc.getString("role");
            m.text = doc.getString("text");
            Boolean fictional = doc.getBoolean("fictional");
            m.fictional = Boolean.TRUE.equals(fictional);
            Long createdAt = doc.getLong("createdAt");
            m.createdAt = createdAt != null ? createdAt : 0L;
            out.add(m);
        }
        return out;
    }
}

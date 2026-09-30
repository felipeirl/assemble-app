package dev.assemble.app.data.repository;

import android.net.Uri;

import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dev.assemble.app.data.local.SessionStore;
import dev.assemble.app.data.model.GameCharacter;
import dev.assemble.app.data.remote.ApiException;
import dev.assemble.app.data.remote.BackendHttpClient;
import dev.assemble.app.data.remote.ContractDto;

/**
 * Personagens (contrato V1 §1): GET /characters?cursor e GET /characters/{id}.
 * Somente leitura; fatos vêm do backend/Comic Vine com fonte. Tradução ausente
 * permanece null (translationStatus sinaliza; nunca inventar texto).
 */
public class CharacterRepository {
    private final BackendHttpClient http;
    private final SessionStore session;
    private final Executor io;

    public CharacterRepository(BackendHttpClient http, SessionStore session) {
        this(http, session, Executors.newCachedThreadPool());
    }

    CharacterRepository(BackendHttpClient http, SessionStore session, Executor io) {
        this.http = http;
        this.session = session;
        this.io = io;
    }

    public void loadPage(String cursor, Callback<ContractDto.CharacterPage> callback) {
        session.idToken(false).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                callback.onError(toException(tokenTask.getException()));
                return;
            }
            String token = tokenTask.getResult();
            io.execute(() -> {
                try {
                    String query = cursor != null && !cursor.isEmpty()
                            ? "cursor=" + Uri.encode(cursor)
                            : "";
                    String json = http.get("/characters", query, token);
                    callback.onSuccess(ContractDto.parseCharacterPage(json));
                } catch (Exception e) {
                    callback.onError(e);
                }
            });
        });
    }

    public void loadDetail(String characterId, Callback<GameCharacter> callback) {
        session.idToken(false).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                callback.onError(toException(tokenTask.getException()));
                return;
            }
            String token = tokenTask.getResult();
            io.execute(() -> {
                try {
                    String json = http.get("/characters/" + Uri.encode(characterId), "", token);
                    callback.onSuccess(ContractDto.parseCharacter(new JSONObject(json)));
                } catch (Exception e) {
                    callback.onError(e);
                }
            });
        });
    }

    private static Exception toException(Exception e) {
        return e != null ? e : new ApiException("auth/token-failed: sem ID token", null);
    }
}

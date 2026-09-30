package dev.assemble.app.data.repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import dev.assemble.app.data.model.Decision;

/**
 * Cache SÓ-LEITURA em memória do resultado do POST /decisions
 * (adendo de segurança 2026-09-30, contrato §5).
 *
 * <p>Guarda score/threshold/connected vindos do backend APENAS para exibir
 * na sessão atual; nunca persiste campo autoritativo no Firestore
 * (as rules negariam a escrita). Não é fonte de verdade: volta do backend
 * a cada POST. Evict com {@link #clear()} no logout/troca de UID.
 */
public final class DecisionResultCache {
    private final ConcurrentMap<String, Decision> cache = new ConcurrentHashMap<>();

    public void put(String characterId, Decision decision) {
        if (characterId != null && decision != null) {
            cache.put(characterId, decision);
        }
    }

    /** Resultado do último POST p/ exibição; null se ausente. Nunca persistido. */
    public Decision get(String characterId) {
        return characterId != null ? cache.get(characterId) : null;
    }

    public void clear() {
        cache.clear();
    }
}

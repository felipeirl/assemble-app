package dev.assemble.app.data.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Decisão (contrato V1 §2 + adendo de segurança 2026-09-30, contrato §5).
 *
 * <p>Dois planos separados, sem mistura:
 * <ul>
 *   <li>CLIENTE → FIRESTORE: {@link #toMapChoice()} — SOMENTE {choice, updatedAt}.
 *       score/threshold/algorithmVersion/connected NUNCA saem do cliente
 *       (firestore.rules nega a escrita; backend é a única fonte autoritativa).</li>
 *   <li>BACKEND → CLIENTE (só exibição): {@code ContractDto.parseDecision(json)}
 *       — valores do POST /decisions, guardados só em memória
 *       ({@code DecisionResultCache}), nunca persistidos.</li>
 * </ul>
 * Transição score→conexão/chat PENDENTE (contrato §3): sem auto-abrir conversa.
 */
public class Decision {
    /** PASS | ASSEMBLE */
    public String choice;
    /** Score do backend p/ exibição em memória; null = indisponível. Nunca persistido. */
    public Integer score;
    public int threshold;
    public String algorithmVersion;
    public boolean connected;
    public long updatedAt;

    public Decision() {}

    /**
     * Payload cliente→Firestore: SOMENTE choice + updatedAt (allowlist das rules).
     * Não existe serialização de campo autoritativo no cliente (§5).
     */
    public Map<String, Object> toMapChoice() {
        Map<String, Object> out = new HashMap<>();
        out.put("choice", choice);
        out.put("updatedAt", updatedAt);
        return out;
    }
}

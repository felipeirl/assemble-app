package dev.assemble.app.data.local;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Espelho JVM-puro da lógica de firestore.rules (adendo §5 2026-09-30).
 *
 * <p>SEM deploy, SEM emulator: esta classe replica a allowlist das rules para
 * teste unitário local. Enforcement real acontece nas rules; aqui garantimos
 * que a LÓGICA que o cliente assume (choice-only, sem autoritativo, sem delete,
 * sem write em matches/messages, dono) está correta e travada por teste.
 * Se as rules mudarem, este espelho + RulesSpecTest devem mudar juntos.
 */
public final class RulesSpec {
    /** Allowlist decisions create/update: SOMENTE choice + updatedAt. */
    public static final Set<String> DECISION_ALLOWED_KEYS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("choice", "updatedAt")));

    /** Campos autoritativos: só backend/Admin SDK (rules negam explícito). */
    public static final Set<String> AUTHORITATIVE_KEYS =
            Collections.unmodifiableSet(new HashSet<>(
                    Arrays.asList("score", "threshold", "algorithmVersion", "connected")));

    private RulesSpec() {}

    public static boolean isOwner(String authUid, String docOwnerUid) {
        return authUid != null && authUid.equals(docOwnerUid);
    }

    public static boolean isValidChoice(String choice) {
        return "PASS".equals(choice) || "ASSEMBLE".equals(choice);
    }

    /** decisions create/update: dono + choice válido + keys ⊆ {choice,updatedAt} + sem autoritativo. */
    public static boolean canCreateOrUpdateDecision(String authUid, String ownerUid,
            Set<String> keys, String choice) {
        if (!isOwner(authUid, ownerUid) || !isValidChoice(choice) || keys == null) {
            return false;
        }
        if (!keys.contains("choice") || !DECISION_ALLOWED_KEYS.containsAll(keys)) {
            return false;
        }
        for (String k : keys) {
            if (AUTHORITATIVE_KEYS.contains(k)) {
                return false;
            }
        }
        return true;
    }

    /** decisions delete: NEGADO sempre (re-decidir = sobrescrever choice). */
    public static boolean canDeleteDecision(String authUid, String ownerUid) {
        return false;
    }

    /** users/{uid} perfil: leitura/escrita do dono. */
    public static boolean canReadWriteProfile(String authUid, String ownerUid) {
        return isOwner(authUid, ownerUid);
    }

    /** matches/** e messages/**: cliente só get/list do dono. */
    public static boolean canReadOwn(String authUid, String ownerUid) {
        return isOwner(authUid, ownerUid);
    }

    /** matches/** e messages/**: escrita do cliente NEGADA sempre. */
    public static boolean canWriteMatchOrMessage(String authUid, String ownerUid) {
        return false;
    }
}

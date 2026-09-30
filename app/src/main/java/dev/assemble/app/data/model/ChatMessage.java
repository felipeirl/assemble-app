package dev.assemble.app.data.model;

/**
 * Mensagem em .../matches/{characterId}/messages/{messageId} (contrato V1 §2).
 * role user|character; fictional=true marca resposta de personagem (ficcional,
 * nunca fato). Cliente nunca cria resposta de IA — só envia texto do usuário
 * via POST /matches/{id}/messages; a resposta IA é gravada pelo backend.
 */
public class ChatMessage {
    /** user | character */
    public String role;
    public String text;
    public boolean fictional;
    public long createdAt;
    public String messageId;

    public ChatMessage() {}

    public boolean isCharacterFiction() {
        return "character".equals(role) && fictional;
    }
}

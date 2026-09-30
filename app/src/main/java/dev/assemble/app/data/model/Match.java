package dev.assemble.app.data.model;

/**
 * Conexão users/{uid}/matches/{characterId} — SOMENTE LEITURA no cliente.
 * Criada exclusivamente pelo backend; cliente nunca cria conexão aceita.
 */
public class Match {
    public String characterId;
    public Integer score;
    public int threshold;
    public String algorithmVersion;
    public long createdAt;

    public Match() {}
}

package dev.assemble.app.data.model;

import java.util.Map;

/**
 * Personagem factual (contrato V1 §2).
 * summaryPt null = tradução indisponível (ver translationStatus, nunca inventar texto).
 * attrs null = atributos insuficientes (score null no backend).
 */
public class GameCharacter {
    public String id;
    public String name;
    public String imageUrl;
    public String summaryOriginal;
    public String summaryPt;
    public String sourceUrl;
    public Map<String, Integer> attrs;
    public String attrsVersion;
    /** OK | PENDING | UNAVAILABLE */
    public String translationStatus;

    public GameCharacter() {}

    /** true quando a tradução pt-BR está indisponível (UI deve sinalizar, não inventar). */
    public boolean isTranslationUnavailable() {
        return "UNAVAILABLE".equals(translationStatus) || summaryPt == null;
    }
}

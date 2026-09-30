package dev.assemble.app.data.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Perfil do usuário (contrato V1 §2).
 * Prefs 0..5 ou null (não definido). Sem score aqui: score é calculado pelo backend.
 */
public class UserProfile {
    public String displayName;
    public Map<String, Integer> prefs;

    public UserProfile() {}

    public UserProfile(String displayName, Integer heroism, Integer humor, Integer strategy, Integer powers) {
        this.displayName = displayName;
        Map<String, Integer> map = new HashMap<>();
        map.put("heroism", heroism);
        map.put("humor", humor);
        map.put("strategy", strategy);
        map.put("powers", powers);
        this.prefs = Collections.unmodifiableMap(map);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<>();
        out.put("displayName", displayName);
        out.put("prefs", prefs != null ? new HashMap<>(prefs) : null);
        return out;
    }
}

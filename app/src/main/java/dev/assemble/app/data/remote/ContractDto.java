package dev.assemble.app.data.remote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dev.assemble.app.data.model.ChatMessage;
import dev.assemble.app.data.model.Decision;
import dev.assemble.app.data.model.GameCharacter;

/**
 * DTOs JSON do contrato V1 §1. Parsing defensivo: campos ausentes viram null
 * (nunca valores inventados — score/biografia ausentes permanecem null).
 */
public final class ContractDto {
    private ContractDto() {}

    public static class CharacterPage {
        public final List<GameCharacter> items = new ArrayList<>();
        public String nextCursor;
    }

    public static CharacterPage parseCharacterPage(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        CharacterPage page = new CharacterPage();
        JSONArray items = root.optJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                page.items.add(parseCharacter(items.optJSONObject(i)));
            }
        }
        page.nextCursor = root.isNull("nextCursor") ? null : root.optString("nextCursor", null);
        return page;
    }

    public static GameCharacter parseCharacter(JSONObject o) throws JSONException {
        if (o == null) {
            return null;
        }
        GameCharacter c = new GameCharacter();
        c.id = o.optString("id", null);
        c.name = o.optString("name", null);
        c.imageUrl = o.optString("imageUrl", null);
        c.summaryOriginal = o.optString("summaryOriginal", null);
        c.summaryPt = o.isNull("summaryPt") ? null : o.optString("summaryPt", null);
        c.sourceUrl = o.optString("sourceUrl", null);
        c.translationStatus = o.optString("translationStatus", "UNAVAILABLE");
        JSONObject attrs = o.optJSONObject("attrs");
        if (attrs != null) {
            Map<String, Integer> map = new HashMap<>();
            Iterator<String> keys = attrs.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                if (!attrs.isNull(k)) {
                    map.put(k, attrs.optInt(k));
                }
            }
            c.attrs = map.isEmpty() ? null : map;
        }
        c.attrsVersion = o.optString("attrsVersion", null);
        return c;
    }

    public static Decision parseDecision(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        Decision d = new Decision();
        d.choice = o.optString("choice", null);
        d.score = o.isNull("score") ? null : o.optInt("score");
        d.threshold = o.optInt("threshold", 70);
        d.algorithmVersion = o.optString("algorithmVersion", "v1");
        d.connected = o.optBoolean("connected", false);
        return d;
    }

    public static ChatMessage parseSentMessage(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        ChatMessage m = new ChatMessage();
        m.messageId = o.optString("messageId", null);
        m.role = "user";
        m.text = o.optString("text", null);
        m.createdAt = o.optLong("createdAt", 0);
        return m;
    }

    public static String decisionBody(String characterId, String choice) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("characterId", characterId);
        o.put("choice", choice);
        return o.toString();
    }

    public static String messageBody(String text) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("text", text);
        return o.toString();
    }
}

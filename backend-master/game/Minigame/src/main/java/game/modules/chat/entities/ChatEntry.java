/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.json.simple.JSONObject
 */
package game.modules.chat.entities;

import org.json.simple.JSONObject;

public class ChatEntry {
    private String nickname;
    private String message;

    public ChatEntry() {
    }

    public ChatEntry(String nickname, String message) {
        this.nickname = nickname;
        this.message = message;
    }

    public String getNickname() {
        return this.nickname;
    }

    public String getMessage() {
        return this.message;
    }

    public JSONObject toJson() {
        JSONObject result = new JSONObject();
        result.put("u", this.nickname);
        result.put("m", this.message);
        return result;
    }
}


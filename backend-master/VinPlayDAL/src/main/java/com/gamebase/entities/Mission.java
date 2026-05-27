/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.vbee.common.enums.Games
 */
package com.gamebase.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamebase.entities.MissionRule;
import com.vinplay.vbee.common.enums.Games;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Mission
implements Serializable {
    private static final long serialVersionUID = -15632346L;
    private String id;
    private String name;
    private String description;
    private String type;
    private long point;
    private int event_id;
    private int game_id;
    private long reward;
    private int status;
    private Date created_at;
    private Date updated_at;
    private Map<String, MissionRule> rules = new HashMap<String, MissionRule>();

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getPoint() {
        return this.point;
    }

    public void setPoint(long point) {
        this.point = point;
    }

    public int getEvent_id() {
        return this.event_id;
    }

    public void setEvent_id(int event_id) {
        this.event_id = event_id;
    }

    public int getGame_id() {
        return this.game_id;
    }

    public void setGame_id(int game_id) {
        this.game_id = game_id;
    }

    public long getReward() {
        return this.reward;
    }

    public void setReward(long reward) {
        this.reward = reward;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreated_at() {
        return this.created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public Date getUpdated_at() {
        return this.updated_at;
    }

    public void setUpdated_at(Date updated_at) {
        this.updated_at = updated_at;
    }

    public String getGameName() {
        if (this.game_id == 0) {
            return "";
        }
        Games game = Games.findGameById((int)this.game_id);
        if (game != null) {
            return game.getName();
        }
        return "";
    }

    public Map<String, MissionRule> getRules() {
        return this.rules;
    }

    public void setRules(Map<String, MissionRule> rules) {
        this.rules = rules;
    }

    public String getRuleString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this.rules);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRuleString(String saveRule) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, MissionRule>> typeRef = new TypeReference<HashMap<String, MissionRule>>(){};
        try {
            this.rules = (Map)mapper.readValue(saveRule, (TypeReference)typeRef);
        }
        catch (IOException e) {
            e.printStackTrace();
            this.rules = new HashMap<String, MissionRule>();
        }
    }
}


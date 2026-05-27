/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.usercore.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserLevel {
    private long id;
    private String nick_name;
    private String code;
    private String ancestor;
    private String created_at;
    private String parent_user;

    public UserLevel() {
    }

    public UserLevel(long id, String nick_name, String code, String ancestor, String created_at, String parent_user) {
        this.id = id;
        this.nick_name = nick_name;
        this.ancestor = ancestor;
        this.created_at = created_at;
        this.parent_user = parent_user;
    }

    public UserLevel(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.nick_name = rs.getString("nick_name").trim();
        this.code = rs.getString("code").trim();
        this.ancestor = rs.getString("ancestor");
        this.created_at = rs.getString("created_at");
        this.parent_user = rs.getString("parent_user");
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNick_name() {
        return this.nick_name;
    }

    public void setNick_name(String nick_name) {
        this.nick_name = nick_name;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAncestor() {
        return this.ancestor;
    }

    public void setAncestor(String ancestor) {
        this.ancestor = ancestor;
    }

    public String getCreated_at() {
        return this.created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getParent_user() {
        return this.parent_user;
    }

    public void setParent_user(String parent_user) {
        this.parent_user = parent_user;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e) {
            return "";
        }
    }
}


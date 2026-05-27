/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.usercore.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EmailActive {
    private String nickname;
    private String email;
    private String time;
    private String checksum;

    public EmailActive() {
    }

    public EmailActive(String nickname, String email, String time, String checksum) {
        this.nickname = nickname;
        this.email = email;
        this.time = time;
        this.checksum = checksum;
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

    public String getChecksum() {
        return this.checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}


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

public class UserAttendance {
    private int id;
    private int attend_id;
    private String nick_name;
    private String date_attend;
    private int consecutive;
    private long bonus_basic;
    private long bonus_consecutive;
    private long bonus_vip;
    private String spin;
    private String result;
    private String ip;

    public UserAttendance() {
    }

    public UserAttendance(int id, int attend_id, String nick_name, String date_attend, int consecutive, long bonus_basic, long bonus_consecutive, long bonus_vip, String spin, String result, String ip) {
        this.id = id;
        this.attend_id = attend_id;
        this.nick_name = nick_name;
        this.date_attend = date_attend;
        this.consecutive = consecutive;
        this.bonus_basic = bonus_basic;
        this.bonus_consecutive = bonus_consecutive;
        this.bonus_vip = bonus_vip;
        this.spin = spin;
        this.result = result;
        this.ip = ip;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAttend_id() {
        return this.attend_id;
    }

    public void setAttend_id(int attend_id) {
        this.attend_id = attend_id;
    }

    public String getNick_name() {
        return this.nick_name;
    }

    public void setNick_name(String nick_name) {
        this.nick_name = nick_name;
    }

    public String getDate_attend() {
        return this.date_attend;
    }

    public void setDate_attend(String date_attend) {
        this.date_attend = date_attend;
    }

    public int getConsecutive() {
        return this.consecutive;
    }

    public void setConsecutive(int consecutive) {
        this.consecutive = consecutive;
    }

    public long getBonus_basic() {
        return this.bonus_basic;
    }

    public void setBonus_basic(long bonus_basic) {
        this.bonus_basic = bonus_basic;
    }

    public long getBonus_consecutive() {
        return this.bonus_consecutive;
    }

    public void setBonus_consecutive(long bonus_consecutive) {
        this.bonus_consecutive = bonus_consecutive;
    }

    public long getBonus_vip() {
        return this.bonus_vip;
    }

    public void setBonus_vip(long bonus_vip) {
        this.bonus_vip = bonus_vip;
    }

    public String getSpin() {
        return this.spin;
    }

    public void setSpin(String spin) {
        this.spin = spin;
    }

    public String getResult() {
        return this.result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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


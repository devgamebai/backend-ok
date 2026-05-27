/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.agent;

import java.io.Serializable;
import java.util.Date;

public class VinPlayAgentModel
implements Serializable {
    private String nick_name;
    private Integer agent_id;
    private Date create_time;
    private String referral_code;
    private Long countUser;

    public VinPlayAgentModel(String nick_name, Integer agent_id, Date create_time, String referral_code, Long cntUser) {
        this.nick_name = nick_name;
        this.agent_id = agent_id;
        this.create_time = create_time;
        this.referral_code = referral_code;
        this.countUser = cntUser;
    }

    public VinPlayAgentModel() {
    }

    public VinPlayAgentModel(String nick_name, Integer agent_id, Date create_time, String referral_code) {
        this.nick_name = nick_name;
        this.agent_id = agent_id;
        this.create_time = create_time;
        this.referral_code = referral_code;
    }

    public Long getCountUser() {
        return this.countUser;
    }

    public void setCountUser(Long countUser) {
        this.countUser = countUser;
    }

    public String getNick_name() {
        return this.nick_name;
    }

    public void setNick_name(String nick_name) {
        this.nick_name = nick_name;
    }

    public Integer getAgent_id() {
        return this.agent_id;
    }

    public void setAgent_id(Integer agent_id) {
        this.agent_id = agent_id;
    }

    public Date getCreate_time() {
        return this.create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }

    public String getReferral_code() {
        return this.referral_code;
    }

    public void setReferral_code(String referral_code) {
        this.referral_code = referral_code;
    }
}


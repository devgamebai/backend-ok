/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.liveUser.entities;

import java.util.Date;

public class LiveUserDepositEntity {
    private int id;
    private String nick_name;
    private String action_name;
    private String fid;
    private int cash;
    private String msgSuccess;
    private String type;
    private boolean run;
    private Date deposit_at;
    private Date created_at;
    private Date last_updated_at;

    public Date getLast_updated_at() {
        return this.last_updated_at;
    }

    public void setLast_updated_at(Date last_updated_at) {
        this.last_updated_at = last_updated_at;
    }

    public String getAction_name() {
        return this.action_name;
    }

    public void setAction_name(String action_name) {
        this.action_name = action_name;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNick_name() {
        return this.nick_name;
    }

    public void setNick_name(String nick_name) {
        this.nick_name = nick_name;
    }

    public String getFid() {
        return this.fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public int getCash() {
        return this.cash;
    }

    public void setCash(int cash) {
        this.cash = cash;
    }

    public String getMsgSuccess() {
        return this.msgSuccess;
    }

    public void setMsgSuccess(String msgSuccess) {
        this.msgSuccess = msgSuccess;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRun() {
        return this.run;
    }

    public void setRun(boolean run) {
        this.run = run;
    }

    public Date getDeposit_at() {
        return this.deposit_at;
    }

    public void setDeposit_at(Date deposit_at) {
        this.deposit_at = deposit_at;
    }

    public Date getCreated_at() {
        return this.created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }
}


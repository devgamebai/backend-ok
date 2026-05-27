/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

import java.io.Serializable;
import java.util.Date;

public class BankSmsModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String content;
    private String sms;
    private long amount;
    private String createTime;
    private int status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSms() {
        return sms;
    }

    public void setSms(String sms) {
        this.sms = sms;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}


/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

public class TopupObj {
    private String id;
    private String target;
    private int type;
    private int amount;
    private int status;
    private String message;
    private String sign;
    private String provider;
    private String partnerTransId;
    private String partner;

    public TopupObj() {
    }

    public TopupObj(String id, String target, int type, int amount, int status, String message, String sign, String provider, String partnerTransId, String partner) {
        this.id = id;
        this.target = target;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.message = message;
        this.sign = sign;
        this.provider = provider;
        this.partnerTransId = partnerTransId;
        this.partner = partner;
    }

    public String getPartner() {
        return this.partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getType() {
        return this.type;
    }

    public String getPartnerTransId() {
        return this.partnerTransId;
    }

    public void setPartnerTransId(String partnerTransId) {
        this.partnerTransId = partnerTransId;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTarget() {
        return this.target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}


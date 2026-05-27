/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.dvt;

import com.vinplay.vbee.common.messages.BaseMessage;

public class CashoutByTopUpMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String nickname;
    private String referenceId;
    private String target;
    private int amount;
    private int status;
    private String message;
    private String sign;
    private int code;
    private String partner;
    private String partnerTransId;
    private String provider;
    private int type;

    public CashoutByTopUpMessage(String nickname, String referenceId, String target, int amount, int status, String message, String sign, int code, String partner, String partnerTransId, String provider, int type) {
        this.nickname = nickname;
        this.referenceId = referenceId;
        this.target = target;
        this.amount = amount;
        this.status = status;
        this.message = message;
        this.sign = sign;
        this.code = code;
        this.partner = partner;
        this.partnerTransId = partnerTransId;
        this.provider = provider;
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSign() {
        return this.sign;
    }

    public String getPartner() {
        return this.partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getPartnerTransId() {
        return this.partnerTransId;
    }

    public void setPartnerTransId(String partnerTransId) {
        this.partnerTransId = partnerTransId;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getReferenceId() {
        return this.referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
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

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}


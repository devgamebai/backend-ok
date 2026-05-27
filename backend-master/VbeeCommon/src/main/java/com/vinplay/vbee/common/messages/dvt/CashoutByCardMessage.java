/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.dvt;

import com.vinplay.vbee.common.messages.BaseMessage;

public class CashoutByCardMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String nickname;
    private String referenceId;
    private String provider;
    private int amount;
    private int quantity;
    private int status;
    private String message;
    private String softpin;
    private String sign;
    private int code;
    private String partner;
    private String partnerTransId;

    public CashoutByCardMessage(String nickname, String referenceId, String provider, int amount, int quantity, int status, String message, String softpin, String sign, int code, String partner, String partnerTransId) {
        this.nickname = nickname;
        this.referenceId = referenceId;
        this.provider = provider;
        this.amount = amount;
        this.quantity = quantity;
        this.status = status;
        this.message = message;
        this.softpin = softpin;
        this.sign = sign;
        this.code = code;
        this.partner = partner;
        this.partnerTransId = partnerTransId;
    }

    public String getPartnerTransId() {
        return this.partnerTransId;
    }

    public void setPartnerTransId(String partnerTransId) {
        this.partnerTransId = partnerTransId;
    }

    public String getPartner() {
        return this.partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getSign() {
        return this.sign;
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

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
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

    public String getSoftpin() {
        return this.softpin;
    }

    public void setSoftpin(String softpin) {
        this.softpin = softpin;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}


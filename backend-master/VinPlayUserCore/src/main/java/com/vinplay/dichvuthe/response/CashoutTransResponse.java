/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.response;

public class CashoutTransResponse {
    private String referenceId;
    private String provider;
    private int amount;
    private int quantity;
    private String partner;
    private String partnerTransId;
    private int isScanned;
    private int code;
    private String message;
    private int status;
    private String softpin;
    private String nickName;

    public CashoutTransResponse() {
    }

    public CashoutTransResponse(String referenceId, String provider, int amount, int quantity, String partner, String partnerTransId, int isScanned, int code, String message, int status, String softpin, String nickName) {
        this.referenceId = referenceId;
        this.provider = provider;
        this.amount = amount;
        this.quantity = quantity;
        this.partner = partner;
        this.partnerTransId = partnerTransId;
        this.isScanned = isScanned;
        this.code = code;
        this.message = message;
        this.status = status;
        this.softpin = softpin;
        this.nickName = nickName;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getSoftpin() {
        return this.softpin;
    }

    public void setSoftpin(String softpin) {
        this.softpin = softpin;
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

    public int getQuantity() {
        return this.quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getPartnerTransId() {
        return this.partnerTransId;
    }

    public void setPartnerTransId(String partnerTransId) {
        this.partnerTransId = partnerTransId;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getPartner() {
        return this.partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public int getIsScanned() {
        return this.isScanned;
    }

    public void setIsScanned(int isScanned) {
        this.isScanned = isScanned;
    }
}


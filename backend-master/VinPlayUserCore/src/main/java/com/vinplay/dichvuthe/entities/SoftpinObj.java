/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.SoftpinJson
 */
package com.vinplay.dichvuthe.entities;

import com.vinplay.vbee.common.models.SoftpinJson;
import java.util.List;

public class SoftpinObj {
    private String id;
    private String provider;
    private int amount;
    private int quantity;
    private int status;
    private String message;
    private List<SoftpinJson> softpinList;
    private String sign;
    private String partnerTransId;
    private String partner;

    public SoftpinObj() {
    }

    public SoftpinObj(String id, String provider, int amount, int quantity, int status, String message, List<SoftpinJson> softpinList, String sign, String partnerTransId, String partner) {
        this.id = id;
        this.provider = provider;
        this.amount = amount;
        this.quantity = quantity;
        this.status = status;
        this.message = message;
        this.softpinList = softpinList;
        this.sign = sign;
        this.partnerTransId = partnerTransId;
        this.partner = partner;
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

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<SoftpinJson> getSoftpinList() {
        return this.softpinList;
    }

    public void setSoftpinList(List<SoftpinJson> softpinList) {
        this.softpinList = softpinList;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}


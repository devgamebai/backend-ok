/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(value=XmlAccessType.FIELD)
@XmlType(name="cardResp", propOrder={"cardData", "message", "status", "transid"})
public class CardResp {
    protected String cardData;
    protected String message;
    protected Integer status;
    protected String transid;

    public String getCardData() {
        return this.cardData;
    }

    public void setCardData(String value) {
        this.cardData = value;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String value) {
        this.message = value;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer value) {
        this.status = value;
    }

    public String getTransid() {
        return this.transid;
    }

    public void setTransid(String value) {
        this.transid = value;
    }

    public String toString() {
        return "CardResp{cardData=" + this.cardData + ", message=" + this.message + ", status=" + this.status + ", transid=" + this.transid + '}';
    }
}


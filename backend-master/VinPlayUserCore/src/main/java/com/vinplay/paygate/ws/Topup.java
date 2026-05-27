/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(value=XmlAccessType.FIELD)
@XmlType(name="topup", propOrder={"transactionId", "userName", "partnerId", "mPin", "cardData", "md5SessionId"})
public class Topup {
    protected String transactionId;
    protected String userName;
    @XmlElement(name="partner_id")
    protected String partnerId;
    protected String mPin;
    protected String cardData;
    protected String md5SessionId;

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String value) {
        this.transactionId = value;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String value) {
        this.userName = value;
    }

    public String getPartnerId() {
        return this.partnerId;
    }

    public void setPartnerId(String value) {
        this.partnerId = value;
    }

    public String getMPin() {
        return this.mPin;
    }

    public void setMPin(String value) {
        this.mPin = value;
    }

    public String getCardData() {
        return this.cardData;
    }

    public void setCardData(String value) {
        this.cardData = value;
    }

    public String getMd5SessionId() {
        return this.md5SessionId;
    }

    public void setMd5SessionId(String value) {
        this.md5SessionId = value;
    }
}


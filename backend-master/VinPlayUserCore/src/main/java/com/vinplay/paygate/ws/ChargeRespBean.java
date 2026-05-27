/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(value=XmlAccessType.FIELD)
@XmlType(name="chargeRespBean", propOrder={"dRemainAmount", "sSerialNumber", "message", "status", "transid"})
public class ChargeRespBean {
    @XmlElement(name="DRemainAmount")
    protected String dRemainAmount;
    @XmlElement(name="SSerialNumber")
    protected String sSerialNumber;
    protected String message;
    protected Integer status;
    protected Long transid;

    public String getDRemainAmount() {
        return this.dRemainAmount;
    }

    public void setDRemainAmount(String value) {
        this.dRemainAmount = value;
    }

    public String getSSerialNumber() {
        return this.sSerialNumber;
    }

    public void setSSerialNumber(String value) {
        this.sSerialNumber = value;
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

    public Long getTransid() {
        return this.transid;
    }

    public void setTransid(Long value) {
        this.transid = value;
    }

    public String toString() {
        return "ChargeRespBean{dRemainAmount=" + this.dRemainAmount + ", sSerialNumber=" + this.sSerialNumber + ", message=" + this.message + ", status=" + this.status + ", transid=" + this.transid + '}';
    }
}


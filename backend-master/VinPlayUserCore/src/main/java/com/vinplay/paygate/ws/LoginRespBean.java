/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(value=XmlAccessType.FIELD)
@XmlType(name="loginRespBean", propOrder={"message", "sessionid", "status", "transid"})
public class LoginRespBean {
    protected String message;
    protected String sessionid;
    protected int status;
    protected String transid;

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String value) {
        this.message = value;
    }

    public String getSessionid() {
        return this.sessionid;
    }

    public void setSessionid(String value) {
        this.sessionid = value;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int value) {
        this.status = value;
    }

    public String getTransid() {
        return this.transid;
    }

    public void setTransid(String value) {
        this.transid = value;
    }

    public String toString() {
        return "LoginRespBean{message=" + this.message + ", sessionid=" + this.sessionid + ", status=" + this.status + ", transid=" + this.transid + '}';
    }
}


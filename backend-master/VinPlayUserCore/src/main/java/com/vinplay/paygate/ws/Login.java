/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(value=XmlAccessType.FIELD)
@XmlType(name="login", propOrder={"userName", "encodePassword", "partnerId"})
public class Login {
    @XmlElement(name="user_name")
    protected String userName;
    @XmlElement(name="encode_password")
    protected String encodePassword;
    @XmlElement(name="partner_id")
    protected String partnerId;

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String value) {
        this.userName = value;
    }

    public String getEncodePassword() {
        return this.encodePassword;
    }

    public void setEncodePassword(String value) {
        this.encodePassword = value;
    }

    public String getPartnerId() {
        return this.partnerId;
    }

    public void setPartnerId(String value) {
        this.partnerId = value;
    }
}


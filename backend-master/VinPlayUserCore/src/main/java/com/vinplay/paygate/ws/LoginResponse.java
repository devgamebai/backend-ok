/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate.ws;

import com.vinplay.paygate.ws.LoginRespBean;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(value=XmlAccessType.FIELD)
@XmlType(name="loginResponse", propOrder={"_return"})
public class LoginResponse {
    @XmlElement(name="return")
    protected LoginRespBean _return;

    public LoginRespBean getReturn() {
        return this._return;
    }

    public void setReturn(LoginRespBean value) {
        this._return = value;
    }
}


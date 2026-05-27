/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate.ws;

import com.vinplay.paygate.ws.ChargeRespBean;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(value=XmlAccessType.FIELD)
@XmlType(name="chargeResponse", propOrder={"_return"})
public class ChargeResponse {
    @XmlElement(name="return")
    protected ChargeRespBean _return;

    public ChargeRespBean getReturn() {
        return this._return;
    }

    public void setReturn(ChargeRespBean value) {
        this._return = value;
    }
}


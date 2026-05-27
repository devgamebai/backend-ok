/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate.ws;

import com.vinplay.paygate.ws.CardResp;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(value=XmlAccessType.FIELD)
@XmlType(name="topupResponse", propOrder={"_return"})
public class TopupResponse {
    @XmlElement(name="return")
    protected CardResp _return;

    public CardResp getReturn() {
        return this._return;
    }

    public void setReturn(CardResp value) {
        this._return = value;
    }
}


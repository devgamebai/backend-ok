/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMessage;

public class PotMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String potName;
    private long moneyExchange;
    private long valuePot;
    private long valuePotSystem;

    public PotMessage(String potName, long moneyExchange, long valuePot, long valuePotSystem) {
        this.potName = potName;
        this.moneyExchange = moneyExchange;
        this.valuePot = valuePot;
        this.valuePotSystem = valuePotSystem;
    }

    public String getPotName() {
        return this.potName;
    }

    public void setPotName(String potName) {
        this.potName = potName;
    }

    public long getMoneyExchange() {
        return this.moneyExchange;
    }

    public void setMoneyExchange(long moneyExchange) {
        this.moneyExchange = moneyExchange;
    }

    public long getValuePot() {
        return this.valuePot;
    }

    public void setValuePot(long valuePot) {
        this.valuePot = valuePot;
    }

    public long getValuePotSystem() {
        return this.valuePotSystem;
    }

    public void setValuePotSystem(long valuePotSystem) {
        this.valuePotSystem = valuePotSystem;
    }
}


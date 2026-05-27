/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.minigame;

import com.vinplay.vbee.common.messages.BaseMessage;

public class ThanhDuMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String username;
    private int number;
    private long totalValue;
    private long currentReferenceId;
    private String references;
    private short type;

    public ThanhDuMessage(String username, int number, long totalValue, long currentReferenceId, String references, short type) {
        this.username = username;
        this.number = number;
        this.totalValue = totalValue;
        this.currentReferenceId = currentReferenceId;
        this.references = references;
        this.type = type;
    }

    public ThanhDuMessage() {
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getNumber() {
        return this.number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public long getTotalValue() {
        return this.totalValue;
    }

    public void setTotalValue(long totalValue) {
        this.totalValue = totalValue;
    }

    public long getCurrentReferenceId() {
        return this.currentReferenceId;
    }

    public void setCurrentReferenceId(long currentReferenceId) {
        this.currentReferenceId = currentReferenceId;
    }

    public String getReferences() {
        return this.references;
    }

    public void setReferences(String references) {
        this.references = references;
    }

    public short getType() {
        return this.type;
    }

    public void setType(short type) {
        this.type = type;
    }
}


/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

import java.io.Serializable;

public class PotModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String potName;
    private long value;
    private int lastDay;

    public PotModel(int id, String potName, long value) {
        this.id = id;
        this.potName = potName;
        this.value = value;
    }

    public int getLastDay() {
        return this.lastDay;
    }

    public void setLastDay(int lastDay) {
        this.lastDay = lastDay;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPotName() {
        return this.potName;
    }

    public void setPotName(String potName) {
        this.potName = potName;
    }

    public long getValue() {
        return this.value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}


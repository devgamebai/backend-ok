/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.vippoint;

public class EventVPBonusModel {
    private String name;
    private int value;
    private int num;
    private int use;

    public EventVPBonusModel(String name, int value, int num, int use) {
        this.name = name;
        this.value = value;
        this.num = num;
        this.use = use;
    }

    public EventVPBonusModel() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getNum() {
        return this.num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public int getUse() {
        return this.use;
    }

    public void setUse(int use) {
        this.use = use;
    }
}


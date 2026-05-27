/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.entities;

public class IAPModel {
    private int id;
    private String name;
    private int value;

    public IAPModel(int id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public IAPModel() {
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
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
}


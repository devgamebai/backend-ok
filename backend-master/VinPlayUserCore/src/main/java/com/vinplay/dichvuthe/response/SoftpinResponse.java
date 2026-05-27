/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.response;

import com.vinplay.dichvuthe.entities.SoftpinObj;

public class SoftpinResponse {
    private String softpin;
    private int code;
    private long currentMoney;
    private SoftpinObj softpinObj;

    public SoftpinResponse(String softpin, int code, long currentMoney, SoftpinObj softpinObj) {
        this.softpin = softpin;
        this.code = code;
        this.currentMoney = currentMoney;
        this.softpinObj = softpinObj;
    }

    public SoftpinObj getSoftpinObj() {
        return this.softpinObj;
    }

    public void setSoftpinObj(SoftpinObj softpinObj) {
        this.softpinObj = softpinObj;
    }

    public String getSoftpin() {
        return this.softpin;
    }

    public void setSoftpin(String softpin) {
        this.softpin = softpin;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public long getCurrentMoney() {
        return this.currentMoney;
    }

    public void setCurrentMoney(long currentMoney) {
        this.currentMoney = currentMoney;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.safebox.response;

public class SafeBoxResponse {
    public int status;
    public String message;
    public double amount;
    public long currentMoney;

    public SafeBoxResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}


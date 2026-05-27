/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

import java.io.Serializable;

public class BroadcastMsgEntry
implements Serializable {
    private static final long serialVersionUID = 1L;
    private int g;
    private String n;
    private long m;

    public BroadcastMsgEntry(int g, String n, long m) {
        this.g = g;
        this.n = n;
        this.m = m;
    }

    public int getG() {
        return this.g;
    }

    public void setG(int g) {
        this.g = g;
    }

    public String getN() {
        return this.n;
    }

    public void setN(String n) {
        this.n = n;
    }

    public long getM() {
        return this.m;
    }

    public void setM(long m) {
        this.m = m;
    }
}


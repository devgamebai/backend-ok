/*
 * Decompiled with CFR 0.144.
 */
package com.vbee.security.model;

public class AgentIP {
    private String IP;
    private int count;
    private long lastRequest;

    public String getIP() {
        return this.IP;
    }

    public void setIP(String iP) {
        this.IP = iP;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getLastRequest() {
        return this.lastRequest;
    }

    public void setLastRequest(long lastRequest) {
        this.lastRequest = lastRequest;
    }
}


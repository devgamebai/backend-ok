/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

import java.util.List;

public class AlertObj {
    private List<String> receives;
    private String content;
    private boolean call;

    public AlertObj() {
    }

    public AlertObj(List<String> receives, String content, boolean call) {
        this.receives = receives;
        this.content = content;
        this.call = call;
    }

    public List<String> getReceives() {
        return this.receives;
    }

    public void setReceives(List<String> receives) {
        this.receives = receives;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isCall() {
        return this.call;
    }

    public void setCall(boolean call) {
        this.call = call;
    }
}


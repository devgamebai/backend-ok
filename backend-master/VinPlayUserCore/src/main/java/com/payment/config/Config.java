/*
 * Decompiled with CFR 0.152.
 */
package com.payment.config;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public static final String BANK = "bank";
    public static final String Card = "card";
    public static final String BANK_OUT = "bankOut";
    private String key;
    private String name;
    private boolean enable;
    private List<String> available = new ArrayList<String>();
    private String config;

    public List<String> getAvailable() {
        return this.available;
    }

    public void setAvailable(List<String> available) {
        this.available = available;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConfig() {
        return this.config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public Boolean getEnable() {
        return this.enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}

